package chapter4.jukebox;

import io.vertx.core.Vertx;

public class Main {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new Jukebox());
    vertx.deployVerticle(new NetControl());
  }
}
