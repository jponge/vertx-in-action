package chapter13.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class HeatApi extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(HeatApi.class);

  private double lowLimit;
  private double highLimit;
  private WebClient webClient;

  @Override
  public void start(Promise<Void> startPromise) {
    Map<String, String> env = System.getenv();
    int httpPort = Integer.parseInt(env.getOrDefault("HTTP_PORT", "8080"));
    String gatewayHost = env.getOrDefault("GATEWAY_HOST", "sensor-gateway");
    int gatewayPort = Integer.parseInt(env.getOrDefault("GATEWAY_PORT", "8080"));
    lowLimit = Double.parseDouble(env.getOrDefault("LOW_TEMP", "10.0"));
    highLimit = Double.parseDouble(env.getOrDefault("HIGH_TEMP", "30.0"));

    logger.info("Correct temperatures range: [{}, {}]", lowLimit, highLimit);

    webClient = WebClient.create(vertx, new WebClientOptions()
      .setDefaultHost(gatewayHost)
      .setDefaultPort(gatewayPort));

    Router router = Router.router(vertx);
    router.get("/all").handler(this::fetchAllData);
    router.get("/warnings").handler(this::sensorsOverLimits);
    router.get("/health/ready").handler(this::readinessCheck);
    router.get("/health/live").handler(this::livenessCheck);

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
  }

  private void fetchData(RoutingContext routingContext, Consumer<HttpResponse<JsonObject>> action) {
    webClient.get("/data")
      .as(BodyCodec.jsonObject())
      .expect(ResponsePredicate.SC_OK)
      .timeout(5000)
      .send(ar -> {
        if (ar.succeeded()) {
          action.accept(ar.result());
        } else {
          routingContext.fail(500);
          logger.error("Could not fetch data", ar.cause());
        }
      });
  }

  private void fetchAllData(RoutingContext routingContext) {
    fetchData(routingContext, resp -> {
      routingContext.response()
        .putHeader("Content-Type", "application/json")
        .end(resp.body().encode());
    });
  }

  private void sensorsOverLimits(RoutingContext routingContext) {
    Predicate<JsonObject> abnormalValue = json -> {
      Double temperature = json.getDouble("temp");
      return (temperature <= lowLimit) || (highLimit <= temperature);
    };
    fetchData(routingContext, resp -> {
      JsonObject data = resp.body();
      JsonArray warnings = new JsonArray();
      data.getJsonArray("data").stream()
        .map(JsonObject.class::cast)
        .filter(abnormalValue)
        .forEach(warnings::add);
      data.put("data", warnings);
      routingContext.response()
        .putHeader("Content-Type", "application/json")
        .end(data.encode());
    });
  }

  private final JsonObject okStatus = new JsonObject()
    .put("status", "UP");

  private void livenessCheck(RoutingContext ctx) {
    logger.info("Liveness check");
    ctx.response()
      .putHeader("Content-Type", "application/json")
      .end(okStatus.encode());
  }

  private void readinessCheck(RoutingContext ctx) {
    webClient.get("/health")
      .expect(ResponsePredicate.SC_OK)
      .timeout(5000)
      .send(ar -> {
        if (ar.succeeded()) {
          logger.info("Readiness check complete");
          ctx.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json")
            .end(okStatus.encode());
        } else {
          logger.error("Readiness check failed", ar.cause());
          ctx.response()
            .setStatusCode(503)
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject()
              .put("status", "DOWN")
              .put("reason", ar.cause().getMessage())
              .encode());
        }
      });
  }

  public static void main(String[] args) {
    Vertx.vertx().deployVerticle(new HeatApi());
  }
}
