package chapter6;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.stream.Collectors;

class SensorDataServiceImpl implements SensorDataService {

  private final HashMap<String, Double> lastValues = new HashMap<>();

  SensorDataServiceImpl(Vertx vertx) {
    vertx.eventBus().<JsonObject>consumer("sensor.updates", message -> {
      JsonObject json = message.body();
      lastValues.put(json.getString("id"), json.getDouble("temp"));
    });
  }

  @Override
  public Future<JsonObject> valueFor(String sensorId) {
    if (lastValues.containsKey(sensorId)) {
      JsonObject data = new JsonObject()
        .put("sensorId", sensorId)
        .put("value", lastValues.get(sensorId));
      return Future.succeededFuture(data);
    } else {
      return Future.failedFuture("No value has been observed for " + sensorId);
    }
  }

  @Override
  public Future<JsonObject> average() {
    double avg = lastValues.values().stream()
      .collect(Collectors.averagingDouble(Double::doubleValue));
    JsonObject data = new JsonObject().put("average", avg);
    return Future.succeededFuture(data);
  }
}
