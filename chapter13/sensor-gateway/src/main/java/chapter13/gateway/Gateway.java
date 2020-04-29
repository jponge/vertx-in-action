package chapter13.gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.PrometheusScrapingHandler;
import io.vertx.micrometer.VertxPrometheusOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class Gateway extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(Gateway.class);

  private final HashMap<String, JsonObject> data = new HashMap<>();

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Map<String, String> env = System.getenv();
    int httpPort = Integer.parseInt(env.getOrDefault("HTTP_PORT", "8080"));
    String targetDestination = env.getOrDefault("EB_UPDATE_DESTINATION", "heatsensor.updates");

    vertx.eventBus().<JsonObject>consumer(targetDestination, message -> {
      JsonObject json = message.body();
      String id = json.getString("id");
      data.put(id, json);
      logger.info("Received an update from sensor {}", id);
    });

    Router router = Router.router(vertx);
    router.get("/data").handler(this::handleRequest);
    router.get("/health").handler(this::healthCheck);

    router.route("/metrics")
      .handler(ctx -> {
        logger.info("Collecting metrics");
        ctx.next();
      })
      .handler(PrometheusScrapingHandler.create());

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

  private void handleRequest(RoutingContext ctx) {
    JsonArray entries = new JsonArray();
    for (String key : data.keySet()) {
      entries.add(data.get(key));
    }
    JsonObject payload = new JsonObject().put("data", entries);
    ctx.response()
      .putHeader("Content-Type", "application/json")
      .end(payload.encode());
  }

  private final JsonObject okStatus = new JsonObject()
    .put("status", "UP");

  private void healthCheck(RoutingContext ctx) {
    logger.info("Health check");
    ctx.response()
      .putHeader("Content-Type", "application/json")
      .end(okStatus.encode());
  }

  public static void main(String[] args) throws UnknownHostException {
    String ipv4 = InetAddress.getLocalHost().getHostAddress();
    VertxOptions options = new VertxOptions()
      .setEventBusOptions(new EventBusOptions()
        .setHost(ipv4)
        .setClusterPublicHost(ipv4))
      .setMetricsOptions(new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions()
          .setPublishQuantiles(true)
          .setEnabled(true))
        .setEnabled(true));
    Vertx.clusteredVertx(options, ar -> {
      if (ar.succeeded()) {
        ar.result().deployVerticle(new Gateway());
      } else {
        logger.error("Could not start", ar.cause());
      }
    });
  }
}
