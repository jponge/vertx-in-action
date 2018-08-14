package chapter2.dissecting;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadsAndContexts {

  private static final Logger logger = LoggerFactory.getLogger(ThreadsAndContexts.class);

  public static void main(String[] args) {
    createAndRun();
    dataAndExceptions();
  }

  private static void createAndRun() {
    Vertx vertx = Vertx.vertx();

    vertx.getOrCreateContext()
      .runOnContext(v -> logger.info("ABC"));

    vertx.getOrCreateContext()
      .runOnContext(v -> logger.info("123"));
  }

  private static void dataAndExceptions() {
    Vertx vertx = Vertx.vertx();
    Context ctx = vertx.getOrCreateContext();
    ctx.put("foo", "bar");

    ctx.exceptionHandler(t -> {
      if ("Tada".equals(t.getMessage())) {
        logger.info("Got a _Tada_ exception");
      } else {
        logger.error("Woops", t);
      }
    });

    ctx.runOnContext(v -> {
      throw new RuntimeException("Tada");
    });

    ctx.runOnContext(v -> {
      logger.info("foo = {}", (String) ctx.get("foo"));
    });
  }
}
