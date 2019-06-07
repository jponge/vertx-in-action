package tenksteps.publicapi;

import io.reactivex.Completable;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicApiVerticle extends AbstractVerticle {

  private static final int HTTP_PORT = 4000;
  private static final Logger logger = LoggerFactory.getLogger(PublicApiVerticle.class);

  private WebClient webClient;

  @Override
  public Completable rxStart() {
    Router router = Router.router(vertx);
    BodyHandler bodyHandler = BodyHandler.create();
    router.post().handler(bodyHandler);
    router.put().handler(bodyHandler);

    String prefix = "/api/v1";
    // Account
    router.post(prefix + "/register").handler(this::register);
    router.post(prefix + "/token").handler(this::token);
    // Profile
    router.get(prefix + "/:username").handler(this::fetchUser);
    router.put(prefix + "/:username").handler(this::updateUser);
    // Data
    router.get(prefix + "/:username/total").handler(this::totalSteps);
    router.get(prefix + "/:username/:year/:month").handler(this::monthlySteps);
    router.get(prefix + "/:username/:year/:month/:day").handler(this::dailySteps);

    webClient = WebClient.create(vertx);

    return vertx.createHttpServer()
      .requestHandler(router)
      .rxListen(HTTP_PORT)
      .ignoreElement();
  }

  private void register(RoutingContext ctx) {
    webClient
      .post(3000, "localhost", "/register")
      .rxSendJson(ctx.getBodyAsJson())
      .subscribe(
        response -> ctx.response().setStatusCode(response.statusCode()).end(),
        err -> internalError(err, ctx));
  }

  private void internalError(Throwable err, RoutingContext ctx) {
    logger.error("Woops", err);
    ctx.fail(500);
  }

  private void token(RoutingContext ctx) {

  }

  private void fetchUser(RoutingContext ctx) {

  }

  private void updateUser(RoutingContext ctx) {

  }

  private void totalSteps(RoutingContext ctx) {

  }

  private void monthlySteps(RoutingContext ctx) {

  }

  private void dailySteps(RoutingContext ctx) {

  }

  public static void main(String[] args) {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
    Vertx vertx = Vertx.vertx();
    vertx
      .rxDeployVerticle(new PublicApiVerticle())
      .subscribe(
        ok -> logger.info("HTTP server started on port {}", HTTP_PORT),
        err -> logger.error("Woops", err));
  }
}
