package tenksteps.ingester;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.amqp.AmqpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.amqp.AmqpClient;
import io.vertx.reactivex.amqp.AmqpMessage;
import io.vertx.reactivex.amqp.AmqpReceiver;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.kafka.client.producer.KafkaProducer;
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

    AmqpClient.create(vertx, amqpOptions)
      .rxConnect()
      .flatMap(conn -> conn.rxCreateReceiver("step-events"))
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

  private void handleAmqpMessage(AmqpMessage amqpMessage) {
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
