package tenksteps.activities;

import io.reactivex.Completable;
import io.vertx.reactivex.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventsVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(EventsVerticle.class);

  @Override
  public Completable rxStart() {
    return Completable.complete(); // todo
  }
}
