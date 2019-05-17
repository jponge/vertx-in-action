package tenksteps.userprofiles;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.mongo.AuthenticationException;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.mongo.MongoAuth;
import io.vertx.reactivex.ext.mongo.MongoClient;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

public class UserProfileApiVerticle extends AbstractVerticle {

  private static final int HTTP_PORT = 3000;
  private static final Logger logger = LoggerFactory.getLogger(UserProfileApiVerticle.class);

  private MongoAuth authProvider;
  private MongoClient mongoClient;

  private JsonObject mongoConfig() {
    return new JsonObject()
      .put("host", "localhost")
      .put("port", 27017)
      .put("db_name", "profiles");
  }

  @Override
  public Completable rxStart() {
    mongoClient = MongoClient.createShared(vertx, mongoConfig());

    JsonObject authConfig = new JsonObject();
    authProvider = MongoAuth.create(mongoClient, authConfig);

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
      body.containsKey("city") &&
      body.containsKey("deviceId") &&
      body.containsKey("makePublic"));
  }

  private final Pattern validUsername = Pattern.compile("\\w+");
  private final Pattern validDeviceId = Pattern.compile("\\w[\\w+|-]*");

  private boolean anyRegistrationFieldIsWrong(JsonObject body) {
    return !validUsername.matcher(body.getString("username")).matches() ||
      body.getString("password").trim().isEmpty() ||
      !validDeviceId.matcher(body.getString("deviceId")).matches();
  }

  private void register(RoutingContext ctx) {
    JsonObject body = jsonBody(ctx);
    String username = body.getString("username");
    String password = body.getString("password");

    JsonObject extraInfo = new JsonObject()
      .put("$set", new JsonObject()
        .put("city", body.getString("city"))
        .put("deviceId", body.getString("deviceId"))
        .put("makePublic", body.getBoolean("makePublic")));

    authProvider
      .rxInsertUser(username, password, emptyList(), emptyList())
      .flatMapMaybe(docId -> {
        JsonObject query = new JsonObject().put("_id", docId);
        return mongoClient.rxFindOneAndUpdate("user", query, extraInfo);
      })
      .subscribe(
        ok -> completeRegistration(ctx),
        err -> handleRegistrationError(ctx, err));
  }

  private void completeRegistration(RoutingContext ctx) {
    ctx.response().end();
  }

  private void handleRegistrationError(RoutingContext ctx, Throwable err) {
    if (err.getMessage().contains("E11000")) {
      ctx.fail(409);
    } else {
      fail500(ctx, err);
    }
  }

  private void fetchUser(RoutingContext ctx) {
    String username = ctx.pathParam("username");

    JsonObject query = new JsonObject()
      .put("username", username);

    JsonObject fields = new JsonObject()
      .put("_id", 0)
      .put("username", 1)
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
    if (err instanceof AuthenticationException) {
      ctx.response().setStatusCode(401).end();
    } else {
      fail500(ctx, err);
    }
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
