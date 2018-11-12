package chapter4.parsetools;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.parsetools.RecordParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FetchDatabaseReader {

  private static final Logger logger = LoggerFactory.getLogger(FetchDatabaseReader.class);

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    AsyncFile file = vertx.fileSystem().openBlocking("sample.db",
      new OpenOptions().setRead(true));

    RecordParser parser = RecordParser.newFixed(4, file);
    parser.pause();
    parser.fetch(1);
    parser.handler(header -> readMagicNumber(header, parser));
    parser.endHandler(v -> vertx.close());
  }

  private static void readMagicNumber(Buffer header, RecordParser parser) {
    logger.info("Magic number: {}:{}:{}:{}", header.getByte(0), header.getByte(1), header.getByte(2), header.getByte(3));
    parser.handler(version -> readVersion(version, parser));
    parser.fetch(1);
  }

  private static void readVersion(Buffer header, RecordParser parser) {
    logger.info("Version: {}", header.getInt(0));
    parser.delimitedMode("\n");
    parser.handler(name -> readName(name, parser));
    parser.fetch(1);
  }

  private static void readName(Buffer name, RecordParser parser) {
    logger.info("Name: {}", name.toString());
    parser.fixedSizeMode(4);
    parser.handler(keyLength -> readKey(keyLength, parser));
    parser.fetch(1);
  }

  private static void readKey(Buffer keyLength, RecordParser parser) {
    parser.fixedSizeMode(keyLength.getInt(0));
    parser.handler(key -> readValue(key.toString(), parser));
    parser.fetch(1);
  }

  private static void readValue(String key, RecordParser parser) {
    parser.fixedSizeMode(4);
    parser.handler(valueLength -> finishEntry(key, valueLength, parser));
    parser.fetch(1);
  }

  private static void finishEntry(String key, Buffer valueLength, RecordParser parser) {
    parser.fixedSizeMode(valueLength.getInt(0));
    parser.handler(value -> {
      logger.info("Key: {} / Value: {}", key, value);
      parser.fixedSizeMode(4);
      parser.handler(keyLength -> readKey(keyLength, parser));
      parser.fetch(1);
    });
    parser.fetch(1);
  }
}
