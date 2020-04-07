package tenksteps.activities;

import io.reactivex.Completable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;
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
    router.get("/ranking-last-24-hours").handler(this::ranking);

    return vertx.createHttpServer()
      .requestHandler(router)
      .rxListen(HTTP_PORT)
      .ignoreElement();
  }

  private void totalSteps(RoutingContext ctx) {
    String deviceId = ctx.pathParam("deviceId");
    Tuple params = Tuple.of(deviceId);
    pgPool
      .preparedQuery(SqlQueries.totalStepsCount())
      .rxExecute(params)
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
        Integer.parseInt(ctx.pathParam("year")),
        Integer.parseInt(ctx.pathParam("month")),
        1, 0, 0);
      Tuple params = Tuple.of(deviceId, dateTime);
      pgPool
        .preparedQuery(SqlQueries.monthlyStepsCount())
        .rxExecute(params)
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
        Integer.parseInt(ctx.pathParam("year")),
        Integer.parseInt(ctx.pathParam("month")),
        Integer.parseInt(ctx.pathParam("day")), 0, 0);
      Tuple params = Tuple.of(deviceId, dateTime);
      pgPool
        .preparedQuery(SqlQueries.dailyStepsCount())
        .rxExecute(params)
        .map(rs -> rs.iterator().next())
        .subscribe(
          row -> sendCount(ctx, row),
          err -> handleError(ctx, err));
    } catch (DateTimeException | NumberFormatException e) {
      sendBadRequest(ctx);
    }
  }

  private void ranking(RoutingContext ctx) {
    pgPool
      .preparedQuery(SqlQueries.rankingLast24Hours())
      .rxExecute()
      .subscribe(
        rows -> sendRanking(ctx, rows),
        err -> handleError(ctx, err));
  }

  private void sendRanking(RoutingContext ctx, RowSet<Row> rows) {
    JsonArray data = new JsonArray();
    for (Row row : rows) {
      data.add(new JsonObject()
        .put("deviceId", row.getValue("device_id"))
        .put("stepsCount", row.getValue("steps")));
    }
    ctx.response()
      .putHeader("Content-Type", "application/json")
      .end(data.encode());
  }
}
