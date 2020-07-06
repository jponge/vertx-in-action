package tenksteps.webapp.dashboard;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.internal.functions.Functions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
import io.vertx.reactivex.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
      .subscribe(record -> forwardKafkaRecord(record, "client.updates.throughput"));

    KafkaConsumer.<String, JsonObject>create(vertx, KafkaConfig.consumerConfig("dashboard-webapp-city-trend"))
      .subscribe("event-stats.city-trend.updates")
      .toFlowable()
      .subscribe(record -> forwardKafkaRecord(record, "client.updates.city-trend"));

    KafkaConsumer.<String, JsonObject>create(vertx, KafkaConfig.consumerConfig("dashboard-webapp-ranking"))
      .subscribe("event-stats.user-activity.updates")
      .toFlowable()
      .filter(record -> record.value().getBoolean("makePublic"))
      .buffer(5, TimeUnit.SECONDS, RxHelper.scheduler(vertx))
      .subscribe(this::updatePublicRanking);

    hydrate();

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    SockJSBridgeOptions bridgeOptions = new SockJSBridgeOptions()
      .addInboundPermitted(new PermittedOptions().setAddressRegex("client.updates.*"))
      .addOutboundPermitted(new PermittedOptions().setAddressRegex("client.updates.*"));
    sockJSHandler.bridge(bridgeOptions);
    router.route("/eventbus/*").handler(sockJSHandler);

    router.route().handler(StaticHandler.create("webroot/assets"));
    router.get("/*").handler(ctx -> ctx.reroute("/index.html"));

    return vertx.createHttpServer()
      .requestHandler(router)
      .rxListen(HTTP_PORT)
      .ignoreElement();
  }

  private void hydrate() {
    WebClient webClient = WebClient.create(vertx);
    webClient
      .get(3001, "localhost", "/ranking-last-24-hours")
      .as(BodyCodec.jsonArray())
      .rxSend()
      .delay(5, TimeUnit.SECONDS, RxHelper.scheduler(vertx))
      .retry(5)
      .map(HttpResponse::body)
      .flattenAsFlowable(Functions.identity())
      .cast(JsonObject.class)
      .flatMapSingle(json -> whoOwnsDevice(webClient, json))
      .flatMapSingle(json -> fillWithUserProfile(webClient, json))
      .subscribe(
        this::hydrateEntryIfPublic,
        err -> logger.error("Hydratation error", err),
        () -> logger.info("Hydratation completed"));
  }

  private void hydrateEntryIfPublic(JsonObject data) {
    if (data.getBoolean("makePublic")) {
      data.put("timestamp", Instant.now().toString());
      publicRanking.put(data.getString("username"), data);
    }
  }

  private Single<JsonObject> fillWithUserProfile(WebClient webClient, JsonObject json) {
    return webClient
      .get(3000, "localhost", "/" + json.getString("username"))
      .as(BodyCodec.jsonObject())
      .rxSend()
      .retry(5)
      .map(HttpResponse::body)
      .map(resp -> resp.mergeIn(json));
  }

  private Single<JsonObject> whoOwnsDevice(WebClient webClient, JsonObject json) {
    return webClient
      .get(3000, "localhost", "/owns/" + json.getString("deviceId"))
      .as(BodyCodec.jsonObject())
      .rxSend()
      .retry(5)
      .map(HttpResponse::body)
      .map(resp -> resp.mergeIn(json));
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
    Long first = a.getLong("stepsCount");
    Long second = b.getLong("stepsCount");
    return second.compareTo(first);
  }

  private void forwardKafkaRecord(KafkaConsumerRecord<String, JsonObject> record, String destination) {
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
