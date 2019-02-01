package chapter5.kotlin.coroutines

import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.predicate.ResponsePredicate
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.core.json.Json
import io.vertx.kotlin.core.json.array
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.ext.web.client.sendAwait
import io.vertx.kotlin.ext.web.client.sendJsonAwait
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class CollectorService : CoroutineVerticle() {

  private val logger = LoggerFactory.getLogger(CollectorService::class.java)

  private lateinit var webClient: WebClient

  override suspend fun start() {
    webClient = WebClient.create(vertx)
    vertx.createHttpServer()
      .requestHandler(this::handleRequest)
      .listenAwait(8080)
  }

  private fun handleRequest(request: HttpServerRequest) {
    launch {
      try {
        val t1 = async { fetchTemperature(3000) }
        val t2 = async { fetchTemperature(3001) }
        val t3 = async { fetchTemperature(3002) }

        val array = Json.array(t1.await(), t2.await(), t3.await())
        val json = json { obj("data" to array) }

        sendToSnapshot(json)
        request.response()
          .putHeader("Content-Type", "application/json")
          .end(json.encode())

      } catch (err: Throwable) {
        logger.error("Something went wrong", err)
        request.response().setStatusCode(500).end()
      }
    }
  }

  private suspend fun fetchTemperature(port: Int): JsonObject {
    return webClient
      .get(port, "localhost", "/")
      .expect(ResponsePredicate.SC_SUCCESS)
      .`as`(BodyCodec.jsonObject())
      .sendAwait()
      .body()
  }

  private suspend fun sendToSnapshot(json: JsonObject) {
    webClient
      .post(4000, "localhost", "/")
      .expect(ResponsePredicate.SC_SUCCESS)
      .sendJsonAwait(json)
  }
}
