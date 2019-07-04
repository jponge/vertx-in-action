package tenksteps.webapp.dashboard;

import io.reactivex.Completable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
import io.vertx.reactivex.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DashboardWebAppVerticle extends AbstractVerticle {

  private static final int HTTP_PORT = 8081;
  private static final Logger logger = LoggerFactory.getLogger(DashboardWebAppVerticle.class);

  private final HashMap<String, JsonObject> publicRanking = new HashMap<>();

  @Override
  public Completable rxStart() {
    Router router = Router.router(vertx);

    KafkaConsumer.<String, JsonObject>create(vertx, KafkaConfig.consumerConfig("dashboard-webapp-throughput"))
      .subscribe("event-stats.throughput")
      .toFlowable()
      .subscribe(record -> forward(record, "client.updates.throughput"));

    KafkaConsumer.<String, JsonObject>create(vertx, KafkaConfig.consumerConfig("dashboard-webapp-city-trend"))
      .subscribe("event-stats.city-trend.updates")
      .toFlowable()
      .subscribe(record -> forward(record, "client.updates.city-trend"));

    KafkaConsumer.<String, JsonObject>create(vertx, KafkaConfig.consumerConfig("dashboard-webapp-ranking"))
      .subscribe("event-stats.user-activity.updates")
      .toFlowable()
      .filter(record -> record.value().getBoolean("makePublic"))
      .buffer(5, TimeUnit.SECONDS, RxHelper.scheduler(vertx))
      .subscribe(this::updatePublicRanking);

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    BridgeOptions bridgeOptions = new BridgeOptions()
      .addInboundPermitted(new PermittedOptions().setAddressRegex("client.updates.*"))
      .addOutboundPermitted(new PermittedOptions().setAddressRegex("client.updates.*"));
    sockJSHandler.bridge(bridgeOptions);
    router.route("/eventbus/*").handler(sockJSHandler);

    router.get("/").handler(ctx -> ctx.reroute("/index.html"));
    router.route().handler(StaticHandler.create("webroot/assets"));

    publicRanking.put("Didier", new JsonObject()
      .put("username", "Didier")
      .put("stepsCount", 5)
      .put("timestamp", Instant.EPOCH));

    return vertx.createHttpServer()
      .requestHandler(router)
      .rxListen(HTTP_PORT)
      .ignoreElement();
  }

  private void updatePublicRanking(List<KafkaConsumerRecord<String, JsonObject>> records) {
    copyBetterScores(records);
    pruneOldEntries();
    vertx.eventBus().publish("client.updates.publicRanking", computeRanking());
  }

  private JsonArray computeRanking() {
    List<JsonObject> ranking = publicRanking.entrySet()
      .stream()
      .map(Map.Entry::getValue)
      .sorted(this::compareStepsCountInReverseOrder)
      .map(json -> new JsonObject()
        .put("username", json.getString("username"))
        .put("stepsCount", json.getLong("stepsCount"))
        .put("city", json.getString("city")))
      .collect(Collectors.toList());
    return new JsonArray(ranking);
  }

  private void pruneOldEntries() {
    Instant now = Instant.now();
    Iterator<Map.Entry<String, JsonObject>> iterator = publicRanking.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, JsonObject> entry = iterator.next();
      Instant timestamp = Instant.parse(entry.getValue().getString("timestamp"));
      if (timestamp.until(now, ChronoUnit.DAYS) >= 1L) {
        iterator.remove();
      }
    }
  }

  private void copyBetterScores(List<KafkaConsumerRecord<String, JsonObject>> records) {
    for (KafkaConsumerRecord<String, JsonObject> record : records) {
      JsonObject json = record.value();
      long stepsCount = json.getLong("stepsCount");
      JsonObject previousData = publicRanking.get(json.getString("username"));
      if (previousData == null || previousData.getLong("stepsCount") < stepsCount) {
        publicRanking.put(json.getString("username"), json);
      }
    }
  }

  private int compareStepsCountInReverseOrder(JsonObject a, JsonObject b) {
    // We cannot just subtract due to possible integer overflow
    long c1 = a.getLong("stepsCount");
    long c2 = b.getLong("stepsCount");
    if (c1 < c2) {
      return 1;
    } else if (c1 == c2) {
      return 0;
    } else {
      return -1;
    }
  }

  private void forward(KafkaConsumerRecord<String, JsonObject> record, String destination) {
    vertx.eventBus().publish(destination, record.value());
  }

  public static void main(String[] args) {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
    Vertx vertx = Vertx.vertx();
    vertx
      .rxDeployVerticle(new DashboardWebAppVerticle())
      .subscribe(
        ok -> logger.info("HTTP server started on port {}", HTTP_PORT),
        err -> logger.error("Woops", err));
  }
}
