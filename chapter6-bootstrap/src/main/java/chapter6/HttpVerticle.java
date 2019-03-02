package chapter6;

import io.vertx.core.AbstractVerticle;

public class HttpVerticle extends AbstractVerticle {

  @Override
  public void start() {
    int port = config().getInteger("http-port", 8080);
    vertx.createHttpServer()
      .requestHandler(req -> req.response().end("Ok"))
      .listen(port, ar -> {
        if (ar.succeeded()) {
          System.out.println("Open http://localhost:" + port);
        } else {
          ar.cause().printStackTrace();
        }
      });
  }
}
