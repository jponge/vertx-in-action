package tenksteps.activities;

import io.reactiverse.reactivex.pgclient.PgClient;
import io.reactiverse.reactivex.pgclient.PgConnection;
import io.reactiverse.reactivex.pgclient.Row;
import io.reactiverse.reactivex.pgclient.Tuple;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.LocalDateTime;

public class ActivityApiVerticle extends AbstractVerticle {

  static final int HTTP_PORT = 3001;
  private static final Logger logger = LoggerFactory.getLogger(ActivityApiVerticle.class);

  private PgClient pgClient;

  @Override
  public Completable rxStart() {
    Router router = Router.router(vertx);
    router.get("/:deviceId/total").handler(this::totalSteps);
    router.get("/:deviceId/:year/:month").handler(this::stepsOnMonth);
    router.get("/:deviceId/:year/:month/:day").handler(this::stepsOnDay);

    Single<HttpServer> serverStart = vertx.createHttpServer()
      .requestHandler(router)
      .rxListen(HTTP_PORT);

    Single<PgConnection> dbConnect = PgClient.rxConnect(vertx, PgConfig.pgOpts());

    return Single.zip(serverStart, dbConnect, (http, db) -> setPgClient(db))
      .ignoreElement();
  }

  private void totalSteps(RoutingContext ctx) {
    String deviceId = ctx.pathParam("deviceId");
    Tuple params = Tuple.of(deviceId);
    pgClient.rxPreparedQuery(SqlQueries.totalStepsCount(), params)
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
      pgClient.rxPreparedQuery(SqlQueries.monthlyStepsCount(), params)
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
      pgClient.rxPreparedQuery(SqlQueries.dailyStepsCount(), params)
        .map(rs -> rs.iterator().next())
        .subscribe(
          row -> sendCount(ctx, row),
          err -> handleError(ctx, err));
    } catch (DateTimeException | NumberFormatException e) {
      sendBadRequest(ctx);
    }
  }

  private PgClient setPgClient(PgConnection dbClient) {
    pgClient = dbClient;
    return pgClient;
  }

}
