package tenksteps.activities;

import io.vertx.pgclient.PgConnectOptions;

class PgConfig {

  public static PgConnectOptions pgConnectOpts() {
    return new PgConnectOptions()
      .setHost("localhost")
      .setDatabase("postgres")
      .setUser("postgres")
      .setPassword("vertx-in-action");
  }
}
