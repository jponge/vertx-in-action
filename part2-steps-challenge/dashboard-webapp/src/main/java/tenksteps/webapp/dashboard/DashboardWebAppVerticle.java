package tenksteps.webapp.dashboard;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
import io.vertx.reactivex.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DashboardWebAppVerticle extends AbstractVerticle {

  private static final int HTTP_PORT = 8081;
  private static final Logger logger = LoggerFactory.getLogger(DashboardWebAppVerticle.class);

  @Override
  public Completable rxStart() {
    Router router = Router.router(vertx);

    KafkaConsumer.<String, JsonObject>create(vertx, KafkaConfig.consumerConfig("dashboard-webapp-throughput"))
      .subscribe("event-stats.throughput")
      .toFlowable()
      .subscribe(record -> this.forward(record, "client.updates.throughput"));

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    BridgeOptions bridgeOptions = new BridgeOptions()
      .addInboundPermitted(new PermittedOptions().setAddressRegex("client.updates.*"))
      .addOutboundPermitted(new PermittedOptions().setAddressRegex("client.updates.*"));
    sockJSHandler.bridge(bridgeOptions);
    router.route("/eventbus/*").handler(sockJSHandler);

    router.get("/").handler(ctx -> ctx.reroute("/index.html"));
    router.route().handler(StaticHandler.create("webroot/assets"));

    return vertx.createHttpServer()
      .requestHandler(router)
      .rxListen(HTTP_PORT)
      .ignoreElement();
  }

  private void forward(KafkaConsumerRecord<String, JsonObject> record, String destination) {
    vertx.eventBus().send(destination, record.value());
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
