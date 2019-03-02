package chapter6;

import io.vertx.core.AbstractVerticle;

public class TickVerticle extends AbstractVerticle {

  @Override
  public void start() {
    long interval = config().getLong("interval", 1000L);
    String message = config().getString("message", "Hello");
    vertx.setPeriodic(interval, id -> System.out.println(message));
  }
}
