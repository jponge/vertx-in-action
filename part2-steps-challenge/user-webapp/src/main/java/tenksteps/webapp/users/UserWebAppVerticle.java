package tenksteps.webapp.users;


import io.reactivex.Completable;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserWebAppVerticle extends AbstractVerticle {

  private static final int HTTP_PORT = 8080;
  private static final Logger logger = LoggerFactory.getLogger(UserWebAppVerticle.class);

  @Override
  public Completable rxStart() {
    Router router = Router.router(vertx);
    router.route().handler(StaticHandler.create("webroot/assets"));
    router.get("/*").handler(ctx -> ctx.reroute("/index.html"));
    return vertx.createHttpServer()
      .requestHandler(router)
      .rxListen(HTTP_PORT)
      .ignoreElement();
  }

  public static void main(String[] args) {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
    Vertx vertx = Vertx.vertx();
    vertx
      .rxDeployVerticle(new UserWebAppVerticle())
      .subscribe(
        ok -> logger.info("HTTP server started on port {}", HTTP_PORT),
        err -> logger.error("Woops", err));
  }
}
