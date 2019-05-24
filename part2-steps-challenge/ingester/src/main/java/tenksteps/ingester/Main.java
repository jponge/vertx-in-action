package tenksteps.ingester;

import io.reactivex.Completable;
import io.vertx.amqp.AmqpClientOptions;
import io.vertx.reactivex.amqp.AmqpClient;
import io.vertx.reactivex.amqp.AmqpMessage;
import io.vertx.reactivex.amqp.AmqpReceiver;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends AbstractVerticle {

  private static final int HTTP_PORT = 3002;
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  @Override
  public Completable rxStart() {
    Router router = Router.router(vertx);

    AmqpClientOptions amqpOptions = new AmqpClientOptions()
      .setHost("localhost")
      .setPort(5672)
      .setUsername("artemis")
      .setPassword("simetraehcapa");

    AmqpClient.create(vertx, amqpOptions)
      .rxConnect()
      .flatMap(conn -> conn.rxCreateReceiver("step-events"))
      .toFlowable()
      .flatMap(AmqpReceiver::toFlowable)
      .subscribe(this::handleAmqpMessage, this::handleAmqpError);

    return vertx.createHttpServer()
      .requestHandler(router)
      .rxListen(HTTP_PORT)
      .ignoreElement();
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
