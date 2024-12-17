package chapter6;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
@VertxGen // To generate RxJava 2 / Kotlin / whatever bindings
public interface SensorDataService {

  static SensorDataService create(Vertx vertx) {
    return new SensorDataServiceImpl(vertx);
  }

  static SensorDataService createProxy(Vertx vertx, String address) {
    return new SensorDataServiceVertxEBProxy(vertx, address);
  }

  Future<JsonObject> valueFor(String sensorId);

  Future<JsonObject> average();
}
