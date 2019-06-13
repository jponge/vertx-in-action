package tenksteps.publicapi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

class CryptoHelper {

  static String publicKey() throws IOException {
    return read("public_key.pem");
  }

  static String privateKey() throws IOException {
    return read("private_key.pem");
  }

  private static String read(String file) throws IOException {
    return Files
      .readAllLines(Paths.get("public-api", file), StandardCharsets.UTF_8)
      .stream()
      .filter(line -> !line.startsWith("-----"))
      .reduce(String::concat)
      .orElse("OUPS");
  }
}
