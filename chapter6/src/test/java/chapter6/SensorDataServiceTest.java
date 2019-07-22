package chapter6;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

@ExtendWith(VertxExtension.class)
class SensorDataServiceTest {

  private SensorDataService dataService;

  @BeforeEach
  void prepare(Vertx vertx, VertxTestContext ctx) {
    vertx.deployVerticle(new DataVerticle(), ctx.succeeding(id -> {
      dataService = SensorDataService.createProxy(vertx, "sensor.data-service");
      ctx.completeNow();
    }));
  }

  @Test
  void noSensor(VertxTestContext ctx) {
    Checkpoint failsToGet = ctx.checkpoint();
    Checkpoint zeroAvg = ctx.checkpoint();

    dataService.valueFor("abc", ctx.failing(err -> ctx.verify(() -> {
      assertThat(err.getMessage()).startsWith("No value has been observed");
      failsToGet.flag();
    })));

    dataService.average(ctx.succeeding(data -> ctx.verify(() -> {
      double avg = data.getDouble("average");
      assertThat(avg).isCloseTo(0.0d, withPercentage(1.0d));
      zeroAvg.flag();
    })));
  }

  @Test
  void withSensors(Vertx vertx, VertxTestContext ctx) {
    Checkpoint getValue = ctx.checkpoint();
    Checkpoint goodAvg = ctx.checkpoint();

    JsonObject m1 = new JsonObject()
      .put("id", "abc").put("temp", 21.0d);
    JsonObject m2 = new JsonObject()
      .put("id", "def").put("temp", 23.0d);
    vertx.eventBus()
      .publish("sensor.updates", m1)
      .publish("sensor.updates", m2);

    dataService.valueFor("abc", ctx.succeeding(data -> ctx.verify(() -> {
      assertThat(data.getString("sensorId")).isEqualTo("abc");
      assertThat(data.getDouble("value")).isEqualTo(21.0d);
      getValue.flag();
    })));

    dataService.average(ctx.succeeding(data -> ctx.verify(() -> {
      assertThat(data.getDouble("average")).isCloseTo(22.0, withPercentage(1.0d));
      goodAvg.flag();
    })));
  }
}
