package chapter5.kotlin.coroutines

import io.vertx.core.Vertx
import io.vertx.kotlin.core.DeploymentOptions
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

fun main(args: Array<String>) {
  val vertx = Vertx.vertx()

  vertx.deployVerticle("chapter5.sensor.HeatSensor",
    DeploymentOptions(config = json {
      obj("http.port" to 3000)
    }))

  vertx.deployVerticle("chapter5.sensor.HeatSensor",
    DeploymentOptions(config = json {
      obj("http.port" to 3001)
    }))

  vertx.deployVerticle("chapter5.sensor.HeatSensor",
    DeploymentOptions(config = json {
      obj("http.port" to 3002)
    }))

  vertx.deployVerticle("chapter5.snapshot.SnapshotService")
  vertx.deployVerticle("chapter5.kotlin.coroutines.CollectorService")
}
