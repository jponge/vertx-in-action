package chapter5.kotlin.coroutines

import io.vertx.core.Vertx
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

fun main() {
  val vertx = Vertx.vertx()

  vertx.deployVerticle("chapter5.sensor.HeatSensor",
    deploymentOptionsOf(config = json {
      obj("http.port" to 3000)
    }))

  vertx.deployVerticle("chapter5.sensor.HeatSensor",
    deploymentOptionsOf(config = json {
      obj("http.port" to 3001)
    }))

  vertx.deployVerticle("chapter5.sensor.HeatSensor",
    deploymentOptionsOf(config = json {
      obj("http.port" to 3002)
    }))

  vertx.deployVerticle("chapter5.snapshot.SnapshotService")
  vertx.deployVerticle("chapter5.kotlin.coroutines.CollectorService")
}
