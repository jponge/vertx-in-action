package tenksteps.activities;

import io.reactiverse.pgclient.PgException;
import io.reactiverse.reactivex.pgclient.PgClient;
import io.reactiverse.reactivex.pgclient.PgConnection;
import io.reactiverse.reactivex.pgclient.Tuple;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.reactivex.kafka.client.producer.KafkaProducer;
import io.vertx.reactivex.kafka.client.producer.KafkaProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static tenksteps.activities.SqlQueries.insertStepEvent;
import static tenksteps.activities.SqlQueries.stepsCountForToday;

public class EventsVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(EventsVerticle.class);

  private PgClient pgClient;
  private KafkaConsumer<String, JsonObject> eventConsumer;
  private KafkaProducer<String, JsonObject> updateProducer;

  @Override
  public Completable rxStart() {
    eventConsumer = KafkaConsumer.create(vertx, KafkaConfig.consumerOffsetEarliest("activity-service"));
    updateProducer = KafkaProducer.create(vertx, KafkaConfig.producer());
    return PgClient.rxConnect(vertx, PgConfig.pgOpts())
      .map(this::setPgClient)
      .flatMapCompletable(conn -> setupConsumer());
  }

  private PgConnection setPgClient(PgConnection pgClient) {
    this.pgClient = pgClient;
    return pgClient;
  }

  private Completable setupConsumer() {
    eventConsumer
      .subscribe("incoming.steps")
      .toFlowable()
      .flatMap(this::insertRecord)
      .flatMap(this::generateActivityUpdate)
      .doOnError(err -> logger.error("Woops", err))
      .retry() // TODO: add re-subscription delay
      .subscribe();
    return Completable.complete();
  }

  private Flowable<KafkaConsumerRecord<String, JsonObject>> insertRecord(KafkaConsumerRecord<String, JsonObject> record) {
    JsonObject data = record.value();

    Tuple values = Tuple.of(
      data.getString("deviceId"),
      data.getLong("deviceSync"),
      data.getInteger("stepsCount"));

    return pgClient
      .rxPreparedQuery(insertStepEvent(), values)
      .map(rs -> record)
      .onErrorReturn(err -> {
        if (duplicateKeyInsert(err)) {
          return record;
        } else {
          throw new RuntimeException(err);
        }
      })
      .toFlowable();
  }

  private boolean duplicateKeyInsert(Throwable err) {
    return (err instanceof PgException) && "23505".equals(((PgException) err).getCode());
  }

  private Flowable<KafkaConsumerRecord<String, JsonObject>> generateActivityUpdate(KafkaConsumerRecord<String, JsonObject> record) {
    String deviceId = record.value().getString("deviceId");
    return pgClient
      .rxPreparedQuery(stepsCountForToday(), Tuple.of(deviceId))
      .map(rs -> rs.iterator().next())
      .map(row -> new JsonObject()
        .put("deviceId", deviceId)
        .put("timestamp", row.getTemporal(0).toString())
        .put("stepsCount", row.getLong(1)))
      .flatMap(json -> updateProducer.rxWrite(KafkaProducerRecord.create("daily.step.updates", deviceId, json)))
      .map(rs -> record)
      .toFlowable();
  }
}
