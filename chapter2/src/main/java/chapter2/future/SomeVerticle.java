package chapter2.future;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class SomeVerticle extends AbstractVerticle {

  @Override
  public void start(Future<Void> startFuture) {   // <1>
    vertx.createHttpServer()
      .requestHandler(req -> req.response().end("Ok"))
      .listen(8080, ar -> {
        if (ar.succeeded()) {       // <2>
          startFuture.complete();   // <3>
        } else {
          startFuture.fail(ar.cause()); // <4>
        }
      });
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new SomeVerticle());
  }
}
