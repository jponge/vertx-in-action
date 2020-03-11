package chapter4.jukebox;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Jukebox extends AbstractVerticle {

  private final Logger logger = LoggerFactory.getLogger(Jukebox.class);

  @Override
  public void start() {
    logger.info("Start");

    EventBus eventBus = vertx.eventBus();
    eventBus.consumer("jukebox.list", this::list);
    eventBus.consumer("jukebox.schedule", this::schedule);
    eventBus.consumer("jukebox.play", this::play);
    eventBus.consumer("jukebox.pause", this::pause);

    vertx.createHttpServer()
      .requestHandler(this::httpHandler)
      .listen(8080);

    vertx.setPeriodic(100, this::streamAudioChunk);
  }

  // --------------------------------------------------------------------------------- //

  private enum State {PLAYING, PAUSED}

  private State currentMode = State.PAUSED;

  private final Queue<String> playlist = new ArrayDeque<>();

  // --------------------------------------------------------------------------------- //

  private void list(Message<?> request) {
    vertx.fileSystem().readDir("tracks", ".*mp3$", ar -> {
      if (ar.succeeded()) {
        List<String> files = ar.result()
          .stream()
          .map(File::new)
          .map(File::getName)
          .collect(Collectors.toList());
        JsonObject json = new JsonObject().put("files", new JsonArray(files));
        request.reply(json);
      } else {
        logger.error("readDir failed", ar.cause());
        request.fail(500, ar.cause().getMessage());
      }
    });
  }

  // --------------------------------------------------------------------------------- //

  private void play(Message<?> request) {
    logger.info("Play");
    currentMode = State.PLAYING;
  }

  private void pause(Message<?> request) {
    logger.info("Pause");
    currentMode = State.PAUSED;
  }

  private void schedule(Message<JsonObject> request) {
    String file = request.body().getString("file");
    logger.info("Scheduling {}", file);
    if (playlist.isEmpty() && currentMode == State.PAUSED) {
      currentMode = State.PLAYING;
    }
    playlist.offer(file);
  }

  // --------------------------------------------------------------------------------- //

  private void httpHandler(HttpServerRequest request) {
    logger.info("{} '{}' {}", request.method(), request.path(), request.remoteAddress());
    if ("/".equals(request.path())) {
      openAudioStream(request);
      return;
    }
    if (request.path().startsWith("/download/")) {
      String sanitizedPath = request.path().substring(10).replaceAll("/", "");
      download(sanitizedPath, request);
      return;
    }
    request.response().setStatusCode(404).end();
  }

  // --------------------------------------------------------------------------------- //

  private final Set<HttpServerResponse> streamers = new HashSet<>();

  private void openAudioStream(HttpServerRequest request) {
    logger.info("New streamer");
    HttpServerResponse response = request.response()
      .putHeader("Content-Type", "audio/mpeg")
      .setChunked(true);
    streamers.add(response);
    response.endHandler(v -> {
      streamers.remove(response);
      logger.info("A streamer left");
    });
  }

  // --------------------------------------------------------------------------------- //

  private void download(String path, HttpServerRequest request) {
    String file = "tracks/" + path;
    if (!vertx.fileSystem().existsBlocking(file)) {
      request.response().setStatusCode(404).end();
      return;
    }
    OpenOptions opts = new OpenOptions().setRead(true);
    vertx.fileSystem().open(file, opts, ar -> {
      if (ar.succeeded()) {
        downloadFile(ar.result(), request);
      } else {
        logger.error("Read failed", ar.cause());
        request.response().setStatusCode(500).end();
      }
    });
  }

  private void downloadFile(AsyncFile file, HttpServerRequest request) {
    HttpServerResponse response = request.response();
    response.setStatusCode(200)
      .putHeader("Content-Type", "audio/mpeg")
      .setChunked(true);

    file.handler(buffer -> {
      response.write(buffer);
      if (response.writeQueueFull()) {
        file.pause();
        response.drainHandler(v -> file.resume());
      }
    });

    file.endHandler(v -> response.end());
  }

  private void downloadFilePipe(AsyncFile file, HttpServerRequest request) {
    HttpServerResponse response = request.response();
    response.setStatusCode(200)
      .putHeader("Content-Type", "audio/mpeg")
      .setChunked(true);
    file.pipeTo(response);
  }

  // --------------------------------------------------------------------------------- //

  private AsyncFile currentFile;
  private long positionInFile;

  private void streamAudioChunk(long id) {
    if (currentMode == State.PAUSED) {
      return;
    }
    if (currentFile == null && playlist.isEmpty()) {
      currentMode = State.PAUSED;
      return;
    }
    if (currentFile == null) {
      openNextFile();
    }
    currentFile.read(Buffer.buffer(4096), 0, positionInFile, 4096, ar -> {
      if (ar.succeeded()) {
        processReadBuffer(ar.result());
      } else {
        logger.error("Read failed", ar.cause());
        closeCurrentFile();
      }
    });
  }

  // --------------------------------------------------------------------------------- //

  private void openNextFile() {
    logger.info("Opening {}", playlist.peek());
    OpenOptions opts = new OpenOptions().setRead(true);
    currentFile = vertx.fileSystem()
      .openBlocking("tracks/" + playlist.poll(), opts);
    positionInFile = 0;
  }

  private void closeCurrentFile() {
    logger.info("Closing file");
    positionInFile = 0;
    currentFile.close();
    currentFile = null;
  }

  // --------------------------------------------------------------------------------- //

  private void processReadBuffer(Buffer buffer) {
    logger.info("Read {} bytes from pos {}", buffer.length(), positionInFile);
    positionInFile += buffer.length();
    if (buffer.length() == 0) {
      closeCurrentFile();
      return;
    }
    for (HttpServerResponse streamer : streamers) {
      if (!streamer.writeQueueFull()) {
        streamer.write(buffer.copy());
      }
    }
  }
}
