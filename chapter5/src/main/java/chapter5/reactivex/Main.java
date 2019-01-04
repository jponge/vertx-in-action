package chapter5.reactivex;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class Main {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    vertx.deployVerticle("chapter5.sensor.HeatSensor",
      new DeploymentOptions().setConfig(new JsonObject()
        .put("http.port", 3000)));

    vertx.deployVerticle("chapter5.sensor.HeatSensor",
      new DeploymentOptions().setConfig(new JsonObject()
        .put("http.port", 3001)));

    vertx.deployVerticle("chapter5.sensor.HeatSensor",
      new DeploymentOptions().setConfig(new JsonObject()
        .put("http.port", 3002)));

    vertx.deployVerticle("chapter5.snapshot.SnapshotService");
    vertx.deployVerticle("chapter5.reactivex.CollectorService");
  }
}
