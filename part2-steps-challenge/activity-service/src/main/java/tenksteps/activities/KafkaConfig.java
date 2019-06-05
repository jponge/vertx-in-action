package tenksteps.activities;

import java.util.HashMap;
import java.util.Map;

class KafkaConfig {

  static Map<String, String> producer() {
    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", "localhost:9092");
    config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    config.put("value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer");
    config.put("acks", "1");
    return config;
  }

  static Map<String, String> consumerOffsetEarliest(String group) {
    return consumerOffsetEarliest(group, "earliest");
  }

  static Map<String, String> consumerOffsetEarliest(String group, String latest) {
    Map<String, String> config = new HashMap<>();
    config.put("bootstrap.servers", "localhost:9092");
    config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    config.put("value.deserializer", "io.vertx.kafka.client.serialization.JsonObjectDeserializer");
    config.put("auto.offset.reset", latest);
    config.put("enable.auto.commit", "false");
    config.put("group.id", group);
    return config;
  }
}
