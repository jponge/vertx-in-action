package tenksteps.activities;

import io.reactiverse.reactivex.pgclient.PgClient;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.kafka.admin.KafkaAdminClient;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.producer.KafkaProducer;
import io.vertx.reactivex.kafka.client.producer.KafkaProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
@DisplayName("Kafka event processing tests")
class EventProcessingTest {

  private KafkaConsumer<String, JsonObject> consumer;
  private KafkaProducer<String, JsonObject> producer;

  @BeforeEach
  void resetPg(Vertx vertx, VertxTestContext testContext) {
    consumer = KafkaConsumer.create(vertx, KafkaConfig.consumerOffsetLatest("activity-service-test"));
    producer = KafkaProducer.create(vertx, KafkaConfig.producer());
    KafkaAdminClient adminClient = KafkaAdminClient.create(vertx, KafkaConfig.producer());
    PgClient.rxConnect(vertx, PgConfig.pgOpts())
      .flatMap(pgConnection -> pgConnection.rxQuery("DELETE FROM StepEvent"))
      .flatMapCompletable(rs -> adminClient.rxDeleteTopics(Arrays.asList("incoming.steps", "daily.step.updates")))
      .subscribe(
        testContext::completeNow,
        testContext::failNow);
  }

  @Test
  void plop(Vertx vertx, VertxTestContext testContext) {
    vertx
      .rxDeployVerticle(new EventsVerticle())
      .flatMap(id -> {
        JsonObject steps = new JsonObject()
          .put("deviceId", "123")
          .put("deviceSync", 1L)
          .put("stepsCount", 200);
        return producer.rxWrite(KafkaProducerRecord.create("incoming.steps", "123", steps));
      })
      .flatMap(id -> {
        JsonObject steps = new JsonObject()
          .put("deviceId", "123")
          .put("deviceSync", 2L)
          .put("stepsCount", 50);
        return producer.rxWrite(KafkaProducerRecord.create("incoming.steps", "123", steps));
      })
      .subscribe(ok -> {
      }, testContext::failNow);

    consumer.subscribe("daily.step.updates")
      .toFlowable()
      .skip(1)
      .subscribe(record -> {
        JsonObject json = record.value();
        testContext.verify(() -> {
          assertThat(json.getString("deviceId")).isEqualTo("123");
          assertThat(json.containsKey("timestamp")).isTrue();
          assertThat(json.getInteger("stepsCount")).isEqualTo(250);
        });
        testContext.completeNow();
      }, testContext::failNow);
  }
}
