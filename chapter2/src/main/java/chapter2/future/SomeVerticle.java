package chapter2.future;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public class SomeVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> promise) {   // <1>
    vertx.createHttpServer()
      .requestHandler(req -> req.response().end("Ok"))
      .listen(8080, ar -> {
        if (ar.succeeded()) {       // <2>
          promise.complete();   // <3>
        } else {
          promise.fail(ar.cause()); // <4>
        }
      });
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new SomeVerticle());
  }
}
