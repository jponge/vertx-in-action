package tenksteps.eventstats;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.Flowable;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.reactivex.kafka.client.producer.KafkaProducer;
import io.vertx.reactivex.kafka.client.producer.KafkaProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class EventStatsVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(EventStatsVerticle.class);

  private KafkaProducer<String, JsonObject> producer;
  private KafkaConsumer<String, JsonObject> throughputConsumer;

  @Override
  public Completable rxStart() {
    producer = KafkaProducer.create(vertx, KafkaConfig.producer());
    throughputConsumer = KafkaConsumer.create(vertx, KafkaConfig.consumer("event-stats-throughput"));

    throughputConsumer
      .subscribe("incoming.steps")
      .toFlowable()
      .buffer(5, TimeUnit.SECONDS, RxHelper.scheduler(vertx))
      .flatMapCompletable(this::publishThroughput)
      .doOnError(err -> logger.error("Woops", err))
      .retryWhen(this::retryLater)
      .subscribe();

    return Completable.complete();
  }

  private CompletableSource publishThroughput(List<KafkaConsumerRecord<String, JsonObject>> records) {
    KafkaProducerRecord<String, JsonObject> record = KafkaProducerRecord.create("event-stats.throughput", new JsonObject()
      .put("seconds", 5)
      .put("count", records.size())
      .put("throughput", (((double) records.size()) / 5.0d)));
    return producer.rxWrite(record);
  }

  private Flowable<Long> retryLater(Flowable<Throwable> errs) {
    return errs
      .flatMap(d -> Flowable.timer(10, TimeUnit.SECONDS, RxHelper.scheduler(vertx)));
  }
}
