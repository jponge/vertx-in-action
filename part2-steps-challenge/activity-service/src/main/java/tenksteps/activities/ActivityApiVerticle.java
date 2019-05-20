package tenksteps.activities;

import io.reactiverse.reactivex.pgclient.PgClient;
import io.reactiverse.reactivex.pgclient.PgConnection;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivityApiVerticle extends AbstractVerticle {

  static final int HTTP_PORT = 3001;
  private static final Logger logger = LoggerFactory.getLogger(ActivityApiVerticle.class);

  private PgClient pgClient;

  @Override
  public Completable rxStart() {
    Router router = Router.router(vertx);

    Single<HttpServer> serverStart = vertx.createHttpServer()
      .requestHandler(router)
      .rxListen(8080);

    Single<PgConnection> dbConnect = PgClient.rxConnect(vertx, PgConfig.pgOpts());

    return Single.zip(serverStart, dbConnect, (http, db) -> setPgClient(db))
      .ignoreElement();
  }

  private PgClient setPgClient(PgConnection dbClient) {
    pgClient = dbClient;
    return pgClient;
  }

}
