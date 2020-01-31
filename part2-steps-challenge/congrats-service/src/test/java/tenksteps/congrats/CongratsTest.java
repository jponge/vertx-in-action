package tenksteps.congrats;

import io.reactivex.Flowable;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.mail.MailClient;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.reactivex.kafka.admin.KafkaAdminClient;
import io.vertx.reactivex.kafka.client.producer.KafkaProducer;
import io.vertx.reactivex.kafka.client.producer.KafkaProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
@Testcontainers
@DisplayName("Tests for the congrats service")
class CongratsTest {

  @Container
  private static final DockerComposeContainer CONTAINERS = new DockerComposeContainer(new File("src/test/docker/docker-compose.yml"));

  private KafkaProducer<String, JsonObject> producer;
  private WebClient webClient;

  @BeforeEach
  void prepare(Vertx vertx, VertxTestContext testContext) {
    Map<String, String> conf = new HashMap<>();
    conf.put("bootstrap.servers", "localhost:9092");
    conf.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    conf.put("value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer");
    conf.put("acks", "1");
    producer = KafkaProducer.create(vertx, conf);
    webClient = WebClient.create(vertx);
    KafkaAdminClient adminClient = KafkaAdminClient.create(vertx, conf);
    adminClient
      .rxDeleteTopics(Arrays.asList("incoming.steps", "daily.step.updates"))
      .onErrorComplete()
      .andThen(vertx.rxDeployVerticle(new CongratsVerticle()))
      .ignoreElement()
      .andThen(vertx.rxDeployVerticle(new FakeUserService()))
      .ignoreElement()
      .andThen(webClient.delete(8025, "localhost", "/api/v1/messages").rxSend())
      .ignoreElement()
      .delay(1, TimeUnit.SECONDS, RxHelper.scheduler(vertx))
      .subscribe(testContext::completeNow, testContext::failNow);
  }

  private KafkaProducerRecord<String, JsonObject> record(String deviceId, long steps) {
    LocalDateTime now = LocalDateTime.now();
    String key = deviceId + ":" + now.getYear() + "-" + now.getMonth() + "-" + now.getDayOfMonth();
    JsonObject json = new JsonObject()
      .put("deviceId", deviceId)
      .put("timestamp", now.toString())
      .put("stepsCount", steps);
    return KafkaProducerRecord.create("daily.step.updates", key, json);
  }

  @Test
  @DisplayName("Smoke test to send a mail using mailhog")
  void smokeTestSendmail(Vertx vertx, VertxTestContext testContext) {
    MailConfig config = new MailConfig()
      .setHostname("localhost")
      .setPort(1025);
    MailClient client = MailClient.createShared(vertx, config);
    MailMessage message = new MailMessage()
      .setFrom("a@b.tld")
      .setSubject("Yo")
      .setTo("c@d.tld")
      .setText("This is cool");
    client
      .rxSendMail(message)
      .subscribe(
        ok -> testContext.completeNow(),
        testContext::failNow);
  }

  @Test
  @DisplayName("No email must be sent below 10k steps")
  void checkNothingBelow10k(Vertx vertx, VertxTestContext testContext) {
    producer
      .rxSend(record("123", 5000))
      .ignoreElement()
      .delay(3, TimeUnit.SECONDS, RxHelper.scheduler(vertx))
      .andThen(webClient
        .get(8025, "localhost", "/api/v2/search?kind=to&query=foo@mail.tld")
        .as(BodyCodec.jsonObject()).rxSend())
      .map(HttpResponse::body)
      .subscribe(
        json -> {
          testContext.verify(() -> assertThat(json.getInteger("total")).isEqualTo(0));
          testContext.completeNow();
        },
        testContext::failNow);
  }

  @Test
  @DisplayName("An email must be sent for 10k+ steps")
  void checkSendsOver10k(Vertx vertx, VertxTestContext testContext) {
    producer
      .rxSend(record("123", 11_000))
      .ignoreElement()
      .delay(3, TimeUnit.SECONDS, RxHelper.scheduler(vertx))
      .andThen(webClient
        .get(8025, "localhost", "/api/v2/search?kind=to&query=foo@mail.tld")
        .as(BodyCodec.jsonObject()).rxSend())
      .map(HttpResponse::body)
      .subscribe(
        json -> {
          testContext.verify(() -> assertThat(json.getInteger("total")).isEqualTo(1));
          testContext.completeNow();
        },
        testContext::failNow);
  }

  @Test
  @DisplayName("Just one email must be sent to a user for 10k+ steps on single day")
  void checkNotTwiceToday(Vertx vertx, VertxTestContext testContext) {
    producer
      .rxSend(record("123", 11_000))
      .ignoreElement()
      .delay(3, TimeUnit.SECONDS, RxHelper.scheduler(vertx))
      .andThen(webClient
        .get(8025, "localhost", "/api/v2/search?kind=to&query=foo@mail.tld")
        .as(BodyCodec.jsonObject()).rxSend())
      .map(HttpResponse::body)
      .map(json -> {
        testContext.verify(() -> assertThat(json.getInteger("total")).isEqualTo(1));
        return json;
      })
      .ignoreElement()
      .andThen(producer.rxSend(record("123", 11_100)))
      .ignoreElement()
      .delay(3, TimeUnit.SECONDS, RxHelper.scheduler(vertx))
      .andThen(webClient
        .get(8025, "localhost", "/api/v2/search?kind=to&query=foo@mail.tld")
        .as(BodyCodec.jsonObject()).rxSend())
      .map(HttpResponse::body)
      .subscribe(
        json -> {
          testContext.verify(() -> assertThat(json.getInteger("total")).isEqualTo(1));
          testContext.completeNow();
        },
        testContext::failNow);
  }

  private Flowable<Long> retryLater(Vertx vertx, Flowable<Throwable> errs) {
    return errs
      .flatMap(d -> Flowable.timer(10, TimeUnit.SECONDS, RxHelper.scheduler(vertx)));
  }
}
