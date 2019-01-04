package chapter5.sensor;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.util.Random;
import java.util.UUID;

public class HeatSensor extends AbstractVerticle {

  private final Random random = new Random();
  private final String id = UUID.randomUUID().toString();
  private double temp = 21.0;

  private void scheduleNextUpdate() {
    vertx.setTimer(random.nextInt(5000) + 1000, this::update);
  }

  private void update(long tid) {
    temp = temp + (delta() / 10);
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
  public void start() {
    vertx.createHttpServer()
      .requestHandler(this::handleRequest)
      .listen(config().getInteger("http.port", 3000));
    scheduleNextUpdate();
  }

  private void handleRequest(HttpServerRequest req) {
    JsonObject data = new JsonObject()
      .put("id", id)
      .put("temp", temp);
    req.response()
      .putHeader("Content-Type", "application/json")
      .end(data.encode());
  }
}
