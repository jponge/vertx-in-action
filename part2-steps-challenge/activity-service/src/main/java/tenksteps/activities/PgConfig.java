package tenksteps.activities;

import io.reactiverse.pgclient.PgPoolOptions;

class PgConfig {

  public static PgPoolOptions pgOpts() {
    return new PgPoolOptions()
      .setHost("localhost")
      .setDatabase("postgres")
      .setUser("postgres")
      .setPassword("vertx-in-action");
  }
}
