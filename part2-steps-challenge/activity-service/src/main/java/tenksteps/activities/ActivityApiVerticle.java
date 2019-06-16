package tenksteps.activities;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.Tuple;
import io.vertx.sqlclient.PoolOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.LocalDateTime;

public class ActivityApiVerticle extends AbstractVerticle {

  static final int HTTP_PORT = 3001;
  private static final Logger logger = LoggerFactory.getLogger(ActivityApiVerticle.class);

  private PgPool pgPool;

  @Override
  public Completable rxStart() {
    pgPool = PgPool.pool(vertx, PgConfig.pgConnectOpts(), new PoolOptions());

    Router router = Router.router(vertx);
    router.get("/:deviceId/total").handler(this::totalSteps);
    router.get("/:deviceId/:year/:month").handler(this::stepsOnMonth);
    router.get("/:deviceId/:year/:month/:day").handler(this::stepsOnDay);

    return vertx.createHttpServer()
      .requestHandler(router)
      .rxListen(HTTP_PORT)
      .ignoreElement();
  }

  private void totalSteps(RoutingContext ctx) {
    String deviceId = ctx.pathParam("deviceId");
    Tuple params = Tuple.of(deviceId);
    pgPool.rxPreparedQuery(SqlQueries.totalStepsCount(), params)
      .map(rs -> rs.iterator().next())
      .subscribe(
        row -> sendCount(ctx, row),
        err -> handleError(ctx, err));
  }

  private void sendCount(RoutingContext ctx, Row row) {
    Integer count = row.getInteger(0);
    if (count != null) {
      JsonObject payload = new JsonObject()
        .put("count", count);
      ctx.response()
        .putHeader("Content-Type", "application/json")
        .end(payload.encode());
    } else {
      send404(ctx);
    }
  }

  private void send404(RoutingContext ctx) {
    ctx.response().setStatusCode(404).end();
  }

  private void handleError(RoutingContext ctx, Throwable err) {
    logger.error("Woops", err);
    ctx.response().setStatusCode(500).end();
  }

  private void stepsOnMonth(RoutingContext ctx) {
    try {
      String deviceId = ctx.pathParam("deviceId");
      LocalDateTime dateTime = LocalDateTime.of(
        Integer.valueOf(ctx.pathParam("year")),
        Integer.valueOf(ctx.pathParam("month")),
        1, 0, 0);
      Tuple params = Tuple.of(deviceId, dateTime);
      pgPool.rxPreparedQuery(SqlQueries.monthlyStepsCount(), params)
        .map(rs -> rs.iterator().next())
        .subscribe(
          row -> sendCount(ctx, row),
          err -> handleError(ctx, err));
    } catch (DateTimeException | NumberFormatException e) {
      sendBadRequest(ctx);
    }
  }

  private void sendBadRequest(RoutingContext ctx) {
    ctx.response().setStatusCode(400).end();
  }

  private void stepsOnDay(RoutingContext ctx) {
    try {
      String deviceId = ctx.pathParam("deviceId");
      LocalDateTime dateTime = LocalDateTime.of(
        Integer.valueOf(ctx.pathParam("year")),
        Integer.valueOf(ctx.pathParam("month")),
        Integer.valueOf(ctx.pathParam("day")), 0, 0);
      Tuple params = Tuple.of(deviceId, dateTime);
      pgPool.rxPreparedQuery(SqlQueries.dailyStepsCount(), params)
        .map(rs -> rs.iterator().next())
        .subscribe(
          row -> sendCount(ctx, row),
          err -> handleError(ctx, err));
    } catch (DateTimeException | NumberFormatException e) {
      sendBadRequest(ctx);
    }
  }
}
