package tenksteps.publicapi;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.client.predicate.ResponsePredicate;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.JWTAuthHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class PublicApiVerticle extends AbstractVerticle {

  private static final int HTTP_PORT = 4000;
  private static final Logger logger = LoggerFactory.getLogger(PublicApiVerticle.class);

  private WebClient webClient;
  private JWTAuth jwtAuth;

  @Override
  public Completable rxStart() {

    String publicKey;
    String privateKey;
    try {
      publicKey = CryptoHelper.publicKey();
      privateKey = CryptoHelper.privateKey();
    } catch (IOException e) {
      return Completable.error(e);
    }

    jwtAuth = JWTAuth.create(vertx, new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("RS256")
        .setPublicKey(publicKey)
        .setSecretKey(privateKey)));

    Router router = Router.router(vertx);
    BodyHandler bodyHandler = BodyHandler.create();
    router.post().handler(bodyHandler);
    router.put().handler(bodyHandler);

    String prefix = "/api/v1";
    JWTAuthHandler jwtHandler = JWTAuthHandler.create(jwtAuth);

    // Account
    router.post(prefix + "/register").handler(this::register);
    router.post(prefix + "/token").handler(this::token);

    // Profile
    router.get(prefix + "/:username").handler(jwtHandler).handler(this::checkUser).handler(this::fetchUser);
    router.put(prefix + "/:username").handler(jwtHandler).handler(this::checkUser).handler(this::updateUser);

    // Data
    router.get(prefix + "/:username/total").handler(jwtHandler).handler(this::checkUser).handler(this::totalSteps);
    router.get(prefix + "/:username/:year/:month").handler(jwtHandler).handler(this::checkUser).handler(this::monthlySteps);
    router.get(prefix + "/:username/:year/:month/:day").handler(jwtHandler).handler(this::checkUser).handler(this::dailySteps);

    webClient = WebClient.create(vertx);

    return vertx.createHttpServer()
      .requestHandler(router)
      .rxListen(HTTP_PORT)
      .ignoreElement();
  }

  private void checkUser(RoutingContext ctx) {
    String subject = ctx.user().principal().getString("sub");
    if (!ctx.pathParam("username").equals(subject)) {
      ctx.response().setStatusCode(403).end();
    } else {
      ctx.next();
    }
  }

  private void register(RoutingContext ctx) {
    webClient
      .post(3000, "localhost", "/register")
      .rxSendJson(ctx.getBodyAsJson())
      .subscribe(
        response -> ctx.response().setStatusCode(response.statusCode()).end(),
        err -> sendBadGateway(err, ctx));
  }

  private void sendBadGateway(Throwable err, RoutingContext ctx) {
    logger.error("Woops", err);
    ctx.fail(502);
  }

  private void token(RoutingContext ctx) {
    JsonObject payload = ctx.getBodyAsJson();
    String username = payload.getString("username");
    webClient
      .post(3000, "localhost", "/authenticate")
      .expect(ResponsePredicate.SC_SUCCESS)
      .rxSendJson(payload)
      .flatMap(resp -> fetchUserDetails(username))
      .map(resp -> resp.body().getString("deviceId"))
      .map(deviceId -> makeJwtToken(username, deviceId))
      .subscribe(
        token -> sendToken(ctx, token),
        err -> handleAuthError(ctx, err));
  }

  private Single<HttpResponse<JsonObject>> fetchUserDetails(String username) {
    return webClient
      .get(3000, "localhost", "/" + username)
      .expect(ResponsePredicate.SC_OK)
      .as(BodyCodec.jsonObject())
      .rxSend();
  }

  private String makeJwtToken(String username, String deviceId) {
    JsonObject claims = new JsonObject()
      .put("deviceId", deviceId);
    JWTOptions jwtOptions = new JWTOptions()
      .setAlgorithm("RS256")
      .setExpiresInMinutes(10_080) // 7 days
      .setIssuer("10k-steps-api")
      .setSubject(username);
    return jwtAuth.generateToken(claims, jwtOptions);
  }

  private void handleAuthError(RoutingContext ctx, Throwable err) {
    logger.error("Authentication error", err);
    ctx.fail(401);
  }

  private void sendToken(RoutingContext ctx, String token) {
    ctx.response().putHeader("Content-Type", "application/jwt").end(token);
  }

  private void fetchUser(RoutingContext ctx) {
    webClient
      .get(3000, "localhost", "/" + ctx.pathParam("username"))
      .expect(ResponsePredicate.SC_OK)
      .as(BodyCodec.jsonObject())
      .rxSend()
      .subscribe(
        resp -> forwardJson(ctx, resp),
        err -> sendBadGateway(err, ctx));
  }

  private void forwardJson(RoutingContext ctx, HttpResponse<JsonObject> resp) {
    ctx.response()
      .putHeader("Content-Type", "application/json")
      .end(resp.body().encode());
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
