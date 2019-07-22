package chapter3;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class SensorDataTest {

  @Test
  void testAverage(Vertx vertx, VertxTestContext ctx) {
    EventBus bus = vertx.eventBus();
    vertx.deployVerticle(new SensorData(), ctx.succeeding(id -> {
      bus.publish("sensor.updates", new JsonObject()
        .put("id", "a").put("temp", 20.0d));
      bus.publish("sensor.updates", new JsonObject()
        .put("id", "b").put("temp", 22.0d));
      bus.request("sensor.average", "", ctx.succeeding(reply -> ctx.verify(() -> {
        JsonObject json = (JsonObject) reply.body();
        assertEquals(21.0d, (double) json.getDouble("average"));
        ctx.completeNow();
      })));
    }));
  }
}
