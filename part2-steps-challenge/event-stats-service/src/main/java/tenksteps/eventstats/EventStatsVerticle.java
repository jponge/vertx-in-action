package tenksteps.eventstats;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.reactivex.kafka.client.producer.KafkaProducer;
import io.vertx.reactivex.kafka.client.producer.KafkaProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EventStatsVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(EventStatsVerticle.class);

  private WebClient webClient;

  private KafkaProducer<String, JsonObject> producer;

  @Override
  public Completable rxStart() {
    webClient = WebClient.create(vertx);
    producer = KafkaProducer.create(vertx, KafkaConfig.producer());

    KafkaConsumer.<String, JsonObject>create(vertx, KafkaConfig.consumer("event-stats-throughput"))
      .subscribe("incoming.steps")
      .toFlowable()
      .buffer(5, TimeUnit.SECONDS, RxHelper.scheduler(vertx))
      .flatMapCompletable(this::publishThroughput)
      .doOnError(err -> logger.error("Woops", err))
      .retryWhen(this::retryLater)
      .subscribe();

    KafkaConsumer.<String, JsonObject>create(vertx, KafkaConfig.consumer("event-stats-user-activity-updates"))
      .subscribe("daily.step.updates")
      .toFlowable()
      .flatMapSingle(this::addDeviceOwner)
      .flatMapSingle(this::addOwnerData)
      .flatMapCompletable(this::publishUserActivityUpdate)
      .doOnError(err -> logger.error("Woops", err))
      .retryWhen(this::retryLater)
      .subscribe();

    KafkaConsumer.<String, JsonObject>create(vertx, KafkaConfig.consumer("event-stats-city-trends"))
      .subscribe("event-stats.user-activity.updates")
      .toFlowable()
      .groupBy(this::city)
      .flatMap(groupedFlowable -> groupedFlowable.buffer(5, TimeUnit.SECONDS, RxHelper.scheduler(vertx)))
      .flatMapCompletable(this::publishCityTrendUpdate)
      .doOnError(err -> logger.error("Woops", err))
      .retryWhen(this::retryLater)
      .subscribe();

    return Completable.complete();
  }

  private CompletableSource publishCityTrendUpdate(List<KafkaConsumerRecord<String, JsonObject>> records) {
    if (records.size() > 0) {
      String city = city(records.get(0));
      Long stepsCount = records.stream()
        .map(record -> record.value().getLong("stepsCount"))
        .reduce(0L, Long::sum);
      KafkaProducerRecord<String, JsonObject> record = KafkaProducerRecord.create("event-stats.city-trend.updates", city, new JsonObject()
        .put("timestamp", LocalDateTime.now().toString())
        .put("seconds", 5)
        .put("city", city)
        .put("stepsCount", stepsCount)
        .put("updates", records.size()));
      return producer.rxWrite(record);
    } else {
      return Completable.complete();
    }
  }

  private String city(KafkaConsumerRecord<String, JsonObject> record) {
    return record.value().getString("city");
  }

  private CompletableSource publishThroughput(List<KafkaConsumerRecord<String, JsonObject>> records) {
    KafkaProducerRecord<String, JsonObject> record = KafkaProducerRecord.create("event-stats.throughput", new JsonObject()
      .put("seconds", 5)
      .put("count", records.size())
      .put("throughput", (((double) records.size()) / 5.0d)));
    return producer.rxWrite(record);
  }

  private Single<JsonObject> addDeviceOwner(KafkaConsumerRecord<String, JsonObject> record) {
    JsonObject data = record.value();
    return webClient
      .get(3000, "localhost", "/owns/" + data.getString("deviceId"))
      .as(BodyCodec.jsonObject())
      .rxSend()
      .map(HttpResponse::body)
      .map(data::mergeIn);
  }

  private Single<JsonObject> addOwnerData(JsonObject data) {
    String username = data.getString("username");
    return webClient
      .get(3000, "localhost", "/" + username)
      .as(BodyCodec.jsonObject())
      .rxSend()
      .map(HttpResponse::body)
      .map(data::mergeIn);
  }

  private CompletableSource publishUserActivityUpdate(JsonObject data) {
    return producer.rxWrite(
      KafkaProducerRecord.create("event-stats.user-activity.updates", data.getString("username"), data));
  }

  private Flowable<Throwable> retryLater(Flowable<Throwable> errs) {
    return errs.delay(10, TimeUnit.SECONDS, RxHelper.scheduler(vertx));
  }

  public static void main(String[] args) {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
    Vertx vertx = Vertx.vertx();
    vertx.rxDeployVerticle(new EventStatsVerticle())
      .subscribe(
        ok -> logger.info("Started"),
        err -> logger.error("Woops", err));
  }
}
