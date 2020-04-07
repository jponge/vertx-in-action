package tenksteps.userprofiles;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.mongo.MongoAuthenticationOptions;
import io.vertx.ext.auth.mongo.MongoAuthorizationOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.mongo.MongoAuthentication;
import io.vertx.reactivex.ext.auth.mongo.MongoUserUtil;
import io.vertx.reactivex.ext.mongo.MongoClient;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;
import java.util.regex.Pattern;

public class UserProfileApiVerticle extends AbstractVerticle {

  private static final int HTTP_PORT = 3000;
  private static final Logger logger = LoggerFactory.getLogger(UserProfileApiVerticle.class);

  private MongoClient mongoClient;
  private MongoAuthentication authProvider;
  private MongoUserUtil userUtil;

  private JsonObject mongoConfig() {
    return new JsonObject()
      .put("host", "localhost")
      .put("port", 27017)
      .put("db_name", "profiles");
  }

  @Override
  public Completable rxStart() {
    mongoClient = MongoClient.createShared(vertx, mongoConfig());

    authProvider = MongoAuthentication.create(mongoClient, new MongoAuthenticationOptions());
    userUtil = MongoUserUtil.create(mongoClient, new MongoAuthenticationOptions(), new MongoAuthorizationOptions());

    Router router = Router.router(vertx);
    BodyHandler bodyHandler = BodyHandler.create();
    router.post().handler(bodyHandler);
    router.put().handler(bodyHandler);
    router.post("/register")
      .handler(this::validateRegistration)
      .handler(this::register);
    router.get("/:username").handler(this::fetchUser);
    router.put("/:username").handler(this::updateUser);
    router.post("/authenticate").handler(this::authenticate);
    router.get("/owns/:deviceId").handler(this::whoOwns);

    return vertx.createHttpServer()
      .requestHandler(router)
      .rxListen(HTTP_PORT)
      .ignoreElement();
  }

  private JsonObject jsonBody(RoutingContext ctx) {
    if (ctx.getBody().length() == 0) {
      return new JsonObject();
    } else {
      return ctx.getBodyAsJson();
    }
  }

  private void validateRegistration(RoutingContext ctx) {
    JsonObject body = jsonBody(ctx);
    if (anyRegistrationFieldIsMissing(body) || anyRegistrationFieldIsWrong(body)) {
      ctx.fail(400);
    } else {
      ctx.next();
    }
  }

  private boolean anyRegistrationFieldIsMissing(JsonObject body) {
    return !(body.containsKey("username") &&
      body.containsKey("password") &&
      body.containsKey("email") &&
      body.containsKey("city") &&
      body.containsKey("deviceId") &&
      body.containsKey("makePublic"));
  }

  private final Pattern validUsername = Pattern.compile("\\w[\\w+|-]*");
  private final Pattern validDeviceId = Pattern.compile("\\w[\\w+|-]*");

  // Email regexp from https://www.owasp.org/index.php/OWASP_Validation_Regex_Repository
  private final Pattern validEmail = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

  private boolean anyRegistrationFieldIsWrong(JsonObject body) {
    return !validUsername.matcher(body.getString("username")).matches() ||
      !validEmail.matcher(body.getString("email")).matches() ||
      body.getString("password").trim().isEmpty() ||
      !validDeviceId.matcher(body.getString("deviceId")).matches();
  }

  private void register(RoutingContext ctx) {
    JsonObject body = jsonBody(ctx);
    String username = body.getString("username");
    String password = body.getString("password");

    JsonObject extraInfo = new JsonObject()
      .put("$set", new JsonObject()
        .put("email", body.getString("email"))
        .put("city", body.getString("city"))
        .put("deviceId", body.getString("deviceId"))
        .put("makePublic", body.getBoolean("makePublic")));

    userUtil
      .rxCreateUser(username, password)
      .flatMapMaybe(docId -> insertExtraInfo(extraInfo, docId))
      .ignoreElement()
      .subscribe(
        () -> completeRegistration(ctx),
        err -> handleRegistrationError(ctx, err));
  }

  private MaybeSource<? extends JsonObject> insertExtraInfo(JsonObject extraInfo, String docId) {
    JsonObject query = new JsonObject().put("_id", docId);
    return mongoClient
      .rxFindOneAndUpdate("user", query, extraInfo)
      .onErrorResumeNext(err -> {
        return deleteIncompleteUser(query, err);
      });
  }

  private MaybeSource<? extends JsonObject> deleteIncompleteUser(JsonObject query, Throwable err) {
    if (isIndexViolated(err)) {
      return mongoClient
        .rxRemoveDocument("user", query)
        .flatMap(del -> Maybe.error(err));
    } else {
      return Maybe.error(err);
    }
  }

  private void completeRegistration(RoutingContext ctx) {
    ctx.response().end();
  }

  private void handleRegistrationError(RoutingContext ctx, Throwable err) {
    if (isIndexViolated(err)) {
      logger.error("Registration failure: {}", err.getMessage());
      ctx.fail(409);
    } else {
      fail500(ctx, err);
    }
  }

  private boolean isIndexViolated(Throwable err) {
    return err.getMessage().contains("E11000");
  }

  private void fetchUser(RoutingContext ctx) {
    String username = ctx.pathParam("username");

    JsonObject query = new JsonObject()
      .put("username", username);

    JsonObject fields = new JsonObject()
      .put("_id", 0)
      .put("username", 1)
      .put("email", 1)
      .put("deviceId", 1)
      .put("city", 1)
      .put("makePublic", 1);

    mongoClient
      .rxFindOne("user", query, fields)
      .toSingle()
      .subscribe(
        json -> completeFetchRequest(ctx, json),
        err -> handleFetchError(ctx, err));
  }

  private void completeFetchRequest(RoutingContext ctx, JsonObject json) {
    ctx.response()
      .putHeader("Content-Type", "application/json")
      .end(json.encode());
  }

  private void handleFetchError(RoutingContext ctx, Throwable err) {
    if (err instanceof NoSuchElementException) {
      ctx.fail(404);
    } else {
      fail500(ctx, err);
    }
  }

  private void updateUser(RoutingContext ctx) {
    String username = ctx.pathParam("username");
    JsonObject body = jsonBody(ctx);

    JsonObject query = new JsonObject().put("username", username);
    JsonObject updates = new JsonObject();
    if (body.containsKey("city")) {
      updates.put("city", body.getString("city"));
    }
    if (body.containsKey("email")) {
      updates.put("email", body.getString("email"));
    }
    if (body.containsKey("makePublic")) {
      updates.put("makePublic", body.getBoolean("makePublic"));
    }

    if (updates.isEmpty()) {
      ctx.response()
        .setStatusCode(200)
        .end();
      return;
    }
    updates = new JsonObject()
      .put("$set", updates);

    mongoClient
      .rxFindOneAndUpdate("user", query, updates)
      .ignoreElement()
      .subscribe(
        () -> completeEmptySuccess(ctx),
        err -> handleUpdateError(ctx, err));
  }

  private void completeEmptySuccess(RoutingContext ctx) {
    ctx.response().setStatusCode(200).end();
  }

  private void handleUpdateError(RoutingContext ctx, Throwable err) {
    fail500(ctx, err);
  }

  private void authenticate(RoutingContext ctx) {
    authProvider.rxAuthenticate(jsonBody(ctx))
      .subscribe(
        user -> completeEmptySuccess(ctx),
        err -> handleAuthenticationError(ctx, err));
  }

  private void handleAuthenticationError(RoutingContext ctx, Throwable err) {
    logger.error("Authentication problem {}", err.getMessage());
    ctx.response().setStatusCode(401).end();
  }

  private void whoOwns(RoutingContext ctx) {
    String deviceId = ctx.pathParam("deviceId");

    JsonObject query = new JsonObject()
      .put("deviceId", deviceId);

    JsonObject fields = new JsonObject()
      .put("_id", 0)
      .put("username", 1)
      .put("deviceId", 1);

    mongoClient
      .rxFindOne("user", query, fields)
      .toSingle()
      .subscribe(
        json -> completeFetchRequest(ctx, json),
        err -> handleFetchError(ctx, err));
  }

  private void fail500(RoutingContext ctx, Throwable err) {
    logger.error("Woops", err);
    ctx.fail(500);
  }

  public static void main(String[] args) {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
    Vertx.vertx()
      .rxDeployVerticle(new UserProfileApiVerticle())
      .subscribe(
        ok -> logger.info("HTTP server started on port {}", HTTP_PORT),
        err -> logger.error("Woops", err));
  }
}
