package chapter13.sensor;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class HeatSensor extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(HeatSensor.class);

  private final Random random = new Random();
  private final String id = UUID.randomUUID().toString();
  private double temp = 21.0;
  private String targetDestination;

  private void scheduleNextUpdate() {
    vertx.setTimer(random.nextInt(5000) + 1000, this::update);
  }

  private void update(long tid) {
    temp = temp + (delta() / 10);
    vertx.eventBus().publish(targetDestination, makeJsonPayload());
    logger.info("{} new temperature is {}", id, temp);
    scheduleNextUpdate();
  }

  private double delta() {
    if (random.nextInt() > 0) {
      return random.nextGaussian();
    } else {
      return -random.nextGaussian();
    }
  }

  @Override
  public void start(Promise<Void> startPromise) {
    Map<String, String> env = System.getenv();
    int httpPort = Integer.parseInt(env.getOrDefault("HTTP_PORT", "8080"));
    targetDestination = env.getOrDefault("EB_UPDATE_DESTINATION", "heatsensor.updates");

    Router router = Router.router(vertx);
    router.get("/data").handler(this::handleRequest);
    router.get("/health").handler(this::healthCheck);

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(httpPort, ar -> {
        if (ar.succeeded()) {
          logger.info("HTTP server listening on port {}", httpPort);
          startPromise.complete();
        } else {
          startPromise.fail(ar.cause());
        }
      });
    scheduleNextUpdate();
  }

  private JsonObject makeJsonPayload() {
    return new JsonObject()
      .put("id", id)
      .put("timestamp", System.currentTimeMillis())
      .put("temp", temp);
  }

  private void handleRequest(RoutingContext ctx) {
    ctx.response()
      .putHeader("Content-Type", "application/json")
      .end(makeJsonPayload().encode());
  }

  private final JsonObject okStatus = new JsonObject()
    .put("status", "UP");

  private void healthCheck(RoutingContext ctx) {
    logger.info("Health check");
    ctx.response()
      .putHeader("Content-Type", "application/json")
      .end(okStatus.encode());
  }

  public static void main(String[] args) throws UnknownHostException {
    String ipv4 = InetAddress.getLocalHost().getHostAddress();
    VertxOptions options = new VertxOptions()
      .setEventBusOptions(new EventBusOptions()
        .setHost(ipv4)
        .setClusterPublicHost(ipv4));
    Vertx.clusteredVertx(options, ar -> {
      if (ar.succeeded()) {
        ar.result().deployVerticle(new HeatSensor());
      } else {
        logger.error("Could not start", ar.cause());
      }
    });
  }
}
