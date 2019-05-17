package tenksteps.activities;

import io.reactiverse.reactivex.pgclient.PgClient;
import io.reactiverse.reactivex.pgclient.PgConnection;
import io.reactivex.Completable;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.producer.KafkaProducer;
import kafka.utils.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventsVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(EventsVerticle.class);

  private PgClient pgClient;
  private KafkaConsumer<String, JsonObject> eventConsumer;
  private KafkaProducer<String, JsonObject> updateProducer;

  @Override
  public Completable rxStart() {
    eventConsumer = KafkaConsumer.create(vertx, KafkaConfig.consumer("activity-service"));

    updateProducer = KafkaProducer.create(vertx, KafkaConfig.producer());

    return PgClient.rxConnect(vertx, PgConfig.pgOpts())
      .map(this::setPgClient)
      .ignoreElement();
  }

  private PgConnection setPgClient(PgConnection pgClient) {
    this.pgClient = pgClient;
    return pgClient;
  }
}
