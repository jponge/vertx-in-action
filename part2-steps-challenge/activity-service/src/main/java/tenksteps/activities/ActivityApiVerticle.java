package tenksteps.activities;

import io.reactiverse.pgclient.PgPoolOptions;
import io.reactiverse.reactivex.pgclient.PgClient;
import io.reactiverse.reactivex.pgclient.PgConnection;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.producer.KafkaProducer;
import kafka.utils.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivityApiVerticle extends AbstractVerticle {

  private static final int HTTP_PORT = 3001;
  private static final Logger logger = LoggerFactory.getLogger(ActivityApiVerticle.class);

  private PgClient pgClient;
  private KafkaConsumer<String, JsonObject> eventConsumer;
  private KafkaProducer<String, JsonObject> updateProducer;

  @Override
  public Completable rxStart() {
    Router router = Router.router(vertx);

    Single<HttpServer> serverStart = vertx.createHttpServer()
      .requestHandler(router)
      .rxListen(8080);

    eventConsumer = KafkaConsumer.create(vertx, KafkaConfig.consumer("activity-service"));
    updateProducer = KafkaProducer.create(vertx, KafkaConfig.producer());

    Single<PgConnection> dbConnect = PgClient.rxConnect(vertx, pgOpts());

    return Single.zip(serverStart, dbConnect, (httpServer, dbClient) -> {
      pgClient = dbClient;
      return pgClient;
    }).ignoreElement();
  }

  private PgPoolOptions pgOpts() {
    return new PgPoolOptions()
      .setHost("localhost")
      .setDatabase("postgres")
      .setUser("postgres")
      .setPassword("vertx-in-action");
  }

  public static void main(String[] args) {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
    Vertx.vertx()
      .rxDeployVerticle(new ActivityApiVerticle())
      .subscribe(
        ok -> logger.info("Server started on port {}", HTTP_PORT),
        err -> logger.error("Woops", err));
  }
}
