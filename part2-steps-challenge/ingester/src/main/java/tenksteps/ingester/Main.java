package tenksteps.ingester;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.amqp.AmqpClientOptions;
import io.vertx.amqp.AmqpReceiverOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.amqp.AmqpClient;
import io.vertx.reactivex.amqp.AmqpMessage;
import io.vertx.reactivex.amqp.AmqpReceiver;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.kafka.client.producer.KafkaProducer;
import io.vertx.reactivex.kafka.client.producer.KafkaProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Main extends AbstractVerticle {

  private static final int HTTP_PORT = 3002;
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private KafkaProducer<String, JsonObject> updateProducer;

  @Override
  public Completable rxStart() {
    Router router = Router.router(vertx);

    AmqpClientOptions amqpOptions = amqpConfig();

    updateProducer = KafkaProducer.create(vertx, kafkaConfig());

    AmqpReceiverOptions receiverOptions = new AmqpReceiverOptions()
      .setAutoAcknowledgement(false);

    AmqpClient.create(vertx, amqpOptions)
      .rxConnect()
      .flatMap(conn -> conn.rxCreateReceiver("step-events", receiverOptions))
      .toFlowable()
      .flatMap(AmqpReceiver::toFlowable)
      .doOnError(this::handleAmqpError)
      .retryWhen(this::retryLater)
      .subscribe(this::handleAmqpMessage, this::handleAmqpError);

    return vertx.createHttpServer()
      .requestHandler(router)
      .rxListen(HTTP_PORT)
      .ignoreElement();
  }

  private Flowable<Long> retryLater(Flowable<Throwable> errs) {
    return errs
      .flatMap(d -> Flowable.timer(10, TimeUnit.SECONDS, RxHelper.scheduler(vertx)));
  }

  private AmqpClientOptions amqpConfig() {
    return new AmqpClientOptions()
      .setHost("localhost")
      .setPort(5672)
      .setUsername("artemis")
      .setPassword("simetraehcapa");
  }

  Map<String, String> kafkaConfig() {
    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", "localhost:9092");
    config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer");
    config.put("acks", "1");
    return config;
  }

  private void handleAmqpError(Throwable err) {
    logger.error("Woops AMQP", err);
  }

  private void handleAmqpMessage(AmqpMessage message) {
    JsonObject payload = message.bodyAsJsonObject();
    if (!payload.containsKey("deviceId") || !payload.containsKey("deviceSync") || !payload.containsKey("stepsCount")) {
      logger.error("Invalid AMQP message: {}", payload.encode());
      return;
    }

    String deviceId = payload.getString("deviceId");
    JsonObject recordData = new JsonObject()
      .put("deviceId", deviceId)
      .put("deviceSync", payload.getLong("deviceSync"))
      .put("stepsCount", payload.getInteger("stepsCount"));

    KafkaProducerRecord<String, JsonObject> record = KafkaProducerRecord
      .create("incoming.steps", deviceId, recordData);

    updateProducer.rxSend(record).subscribe(
      ok -> message.accepted(),
      err -> message.rejected());
  }

  public static void main(String[] args) {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
    Vertx vertx = Vertx.vertx();
    vertx.rxDeployVerticle(new Main())
      .subscribe(
        ok -> logger.info("HTTP server started on port {}", HTTP_PORT),
        err -> logger.error("Woops", err));
  }
}
