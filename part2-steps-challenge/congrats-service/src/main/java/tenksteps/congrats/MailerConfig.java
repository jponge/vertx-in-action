package tenksteps.congrats;

import io.vertx.ext.mail.MailConfig;

class MailerConfig {

  static MailConfig config() {
    return new MailConfig()
      .setHostname("localhost")
      .setPort(1025);
  }
}
