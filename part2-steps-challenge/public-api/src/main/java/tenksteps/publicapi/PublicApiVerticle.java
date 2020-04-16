package tenksteps.publicapi;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.circuitbreaker.OpenCircuitException;
import io.vertx.circuitbreaker.TimeoutException;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.reactivex.circuitbreaker.CircuitBreaker;
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
import io.vertx.reactivex.ext.web.handler.CorsHandler;
import io.vertx.reactivex.ext.web.handler.JWTAuthHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class PublicApiVerticle extends AbstractVerticle {

  private static final int HTTP_PORT = 4000;
  private static final Logger logger = LoggerFactory.getLogger(PublicApiVerticle.class);

  private WebClient webClient;
  private JWTAuth jwtAuth;
  private CircuitBreaker tokenCircuitBreaker;
  private CircuitBreaker activityCircuitBreaker;
  private Cache<String, Long> stepsCache;

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
        .setBuffer(publicKey))
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("RS256")
        .setBuffer(privateKey)));

    Router router = Router.router(vertx);

    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add("x-requested-with");
    allowedHeaders.add("Access-Control-Allow-Origin");
    allowedHeaders.add("origin");
    allowedHeaders.add("Content-Type");
    allowedHeaders.add("accept");
    allowedHeaders.add("Authorization");

    Set<HttpMethod> allowedMethods = new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);
    allowedMethods.add(HttpMethod.OPTIONS);
    allowedMethods.add(HttpMethod.PUT);

    router.route().handler(CorsHandler
      .create("*")
      .allowedHeaders(allowedHeaders)
      .allowedMethods(allowedMethods));

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

    String tokenCircuitBreakerName = "token-circuit-breaker";
    tokenCircuitBreaker = CircuitBreaker.create(tokenCircuitBreakerName, vertx, circuitBreakerOptions());

    String activityCircuitBreakerName = "activity-circuit-breaker";
    activityCircuitBreaker = CircuitBreaker.create(activityCircuitBreakerName, vertx, circuitBreakerOptions());

    tokenCircuitBreaker.openHandler(v -> this.logBreakerUpdate("open", tokenCircuitBreakerName));
    tokenCircuitBreaker.halfOpenHandler(v -> this.logBreakerUpdate("half open", tokenCircuitBreakerName));
    tokenCircuitBreaker.closeHandler(v -> this.logBreakerUpdate("closed", tokenCircuitBreakerName));

    activityCircuitBreaker.openHandler(v -> this.logBreakerUpdate("open", activityCircuitBreakerName));
    activityCircuitBreaker.halfOpenHandler(v -> this.logBreakerUpdate("half open", activityCircuitBreakerName));
    activityCircuitBreaker.closeHandler(v -> this.logBreakerUpdate("closed", activityCircuitBreakerName));

    stepsCache = Caffeine.newBuilder()
      .maximumSize(10_000)
      .build();

    return vertx.createHttpServer()
      .requestHandler(router)
      .rxListen(HTTP_PORT)
      .ignoreElement();
  }

  private CircuitBreakerOptions circuitBreakerOptions() {
    return new CircuitBreakerOptions()
      .setMaxFailures(5)
      .setMaxRetries(0)
      .setTimeout(5000)
      .setResetTimeout(10_000);
  }

  private void logBreakerUpdate(String state, String breakerName) {
    logger.info("Circuit breaker {} is now {}", breakerName, state);
  }

  private void checkUser(RoutingContext ctx) {
    String subject = ctx.user().principal().getString("sub");
    if (!ctx.pathParam("username").equals(subject)) {
      sendStatusCode(ctx, 403);
    } else {
      ctx.next();
    }
  }

  private void register(RoutingContext ctx) {
    webClient
      .post(3000, "localhost", "/register")
      .putHeader("Content-Type", "application/json")
      .rxSendJson(ctx.getBodyAsJson())
      .subscribe(
        response -> sendStatusCode(ctx, response.statusCode()),
        err -> sendBadGateway(ctx, err));
  }

  private void sendStatusCode(RoutingContext ctx, int code) {
    ctx.response().setStatusCode(code).end();
  }

  private void sendBadGateway(RoutingContext ctx, Throwable err) {
    logger.error("Woops", err);
    ctx.fail(502);
  }

  private void token(RoutingContext ctx) {
    tokenCircuitBreaker.<String>rxExecute(promise -> {
      JsonObject payload = ctx.getBodyAsJson();
      String username = payload.getString("username");
      webClient
        .post(3000, "localhost", "/authenticate")
        .expect(ResponsePredicate.SC_SUCCESS)
        .rxSendJson(payload)
        .flatMap(resp -> fetchUserDetails(username))
        .map(resp -> resp.body().getString("deviceId"))
        .map(deviceId -> makeJwtToken(username, deviceId))
        .subscribe(promise::complete, err -> {
          if (err instanceof NoStackTraceThrowable) {
            promise.complete("");
          } else {
            promise.fail(err);
          }
        });
    }).subscribe(
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
    if (err instanceof OpenCircuitException) {
      logger.error("Circuit breaker is open: {}", tokenCircuitBreaker.name());
      ctx.fail(504);
    } else if (err instanceof TimeoutException) {
      logger.error("Circuit breaker timeout: {}", tokenCircuitBreaker.name());
      ctx.fail(504);
    } else {
      logger.error("Authentication error", err);
      ctx.fail(401);
    }
  }

  private void sendToken(RoutingContext ctx, String token) {
    if (token.isEmpty()) {
      handleAuthError(ctx, null);
    } else {
      ctx.response().putHeader("Content-Type", "application/jwt").end(token);
    }
  }

  private void fetchUser(RoutingContext ctx) {
    webClient
      .get(3000, "localhost", "/" + ctx.pathParam("username"))
      .as(BodyCodec.jsonObject())
      .rxSend()
      .subscribe(
        resp -> forwardJsonOrStatusCode(ctx, resp),
        err -> sendBadGateway(ctx, err));
  }

  private void forwardJsonOrStatusCode(RoutingContext ctx, HttpResponse<JsonObject> resp) {
    if (resp.statusCode() != 200) {
      sendStatusCode(ctx, resp.statusCode());
    } else {
      ctx.response()
        .putHeader("Content-Type", "application/json")
        .end(resp.body().encode());
    }
  }

  private void updateUser(RoutingContext ctx) {
    webClient
      .put(3000, "localhost", "/" + ctx.pathParam("username"))
      .putHeader("Content-Type", "application/json")
      .expect(ResponsePredicate.SC_OK)
      .rxSendBuffer(ctx.getBody())
      .subscribe(
        resp -> ctx.response().end(),
        err -> sendBadGateway(ctx, err));
  }

  private void totalSteps(RoutingContext ctx) {
    String deviceId = ctx.user().principal().getString("deviceId");
    activityCircuitBreaker.<Void>executeWithFallback(promise -> {
      webClient
        .get(3001, "localhost", "/" + deviceId + "/total")
        .timeout(5000)
        .expect(ResponsePredicate.SC_OK)
        .as(BodyCodec.jsonObject())
        .rxSend()
        .subscribe(resp -> {
          cacheTotalSteps(deviceId, resp);
          forwardJsonOrStatusCode(ctx, resp);
          promise.complete();
        }, err -> {
          tryToRecoverFromCache(ctx, deviceId);
          promise.fail(err);
        });
    }, err -> {
      tryToRecoverFromCache(ctx, deviceId);
      return null;
    });
  }

  private void tryToRecoverFromCache(RoutingContext ctx, String deviceId) {
    Long steps = stepsCache.getIfPresent("total:" + deviceId);
    if (steps == null) {
      logger.error("No cached data for the total steps of device {}", deviceId);
      ctx.fail(502);
    } else {
      JsonObject payload = new JsonObject()
        .put("count", steps);
      ctx.response()
        .putHeader("Content-Type", "application/json")
        .end(payload.encode());
    }
  }

  private void cacheTotalSteps(String deviceId, HttpResponse<JsonObject> resp) {
    if (resp.statusCode() == 200) {
      stepsCache.put("total:" + deviceId, resp.body().getLong("count"));
    }
  }

  private void monthlySteps(RoutingContext ctx) {
    String deviceId = ctx.user().principal().getString("deviceId");
    String year = ctx.pathParam("year");
    String month = ctx.pathParam("month");
    webClient
      .get(3001, "localhost", "/" + deviceId + "/" + year + "/" + month)
      .as(BodyCodec.jsonObject())
      .rxSend()
      .subscribe(
        resp -> forwardJsonOrStatusCode(ctx, resp),
        err -> sendBadGateway(ctx, err));
  }

  private void dailySteps(RoutingContext ctx) {
    String deviceId = ctx.user().principal().getString("deviceId");
    String year = ctx.pathParam("year");
    String month = ctx.pathParam("month");
    String day = ctx.pathParam("day");
    webClient
      .get(3001, "localhost", "/" + deviceId + "/" + year + "/" + month + "/" + day)
      .as(BodyCodec.jsonObject())
      .rxSend()
      .subscribe(
        resp -> forwardJsonOrStatusCode(ctx, resp),
        err -> sendBadGateway(ctx, err));
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
