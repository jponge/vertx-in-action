package tenksteps.activities;

import io.reactiverse.reactivex.pgclient.PgClient;
import io.reactiverse.reactivex.pgclient.Tuple;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
@DisplayName("HTTP API tests")
@Testcontainers
public class ApiTest {

  @Container
  private static final DockerComposeContainer CONTAINERS = new DockerComposeContainer(new File("../docker-compose.yml"));

  private static RequestSpecification requestSpecification;

  @BeforeAll
  static void prepareSpec() {
    requestSpecification = new RequestSpecBuilder()
      .addFilters(asList(new ResponseLoggingFilter(), new RequestLoggingFilter()))
      .setBaseUri("http://localhost:3001/")
      .build();
  }

  private PgClient pgClient;

  @BeforeEach
    // TODO close pgClient (needs next version)
  void prepareDb(Vertx vertx, VertxTestContext testContext) {
    String insertQuery = "INSERT INTO stepevent VALUES($1, $2, $3::timestamp, $4)";
    List<Tuple> data = Arrays.asList(
      Tuple.of("123", 1, LocalDateTime.of(2019, 4, 1, 23, 0), 6541),
      Tuple.of("123", 2, LocalDateTime.of(2019, 5, 20, 10, 0), 200),
      Tuple.of("123", 3, LocalDateTime.of(2019, 5, 21, 10, 10), 100),
      Tuple.of("456", 1, LocalDateTime.of(2019, 5, 21, 10, 15), 123),
      Tuple.of("123", 4, LocalDateTime.of(2019, 5, 21, 11, 0), 320)
    );
    PgClient.rxConnect(vertx, PgConfig.pgOpts())
      .doOnSuccess(pgConnection -> this.pgClient = pgConnection)
      .flatMap(c -> pgClient.rxQuery("DELETE FROM stepevent"))
      .flatMap(rs -> pgClient.rxPreparedBatch(insertQuery, data))
      .ignoreElement()
      .andThen(vertx.rxDeployVerticle(new ActivityApiVerticle()))
      .ignoreElement()
      .subscribe(testContext::completeNow, testContext::failNow);
  }

  @Test
  @DisplayName("Operate a few successful steps count queries over the dataset")
  void stepsCountQueries() {
    JsonPath jsonPath = given()
      .spec(requestSpecification)
      .accept(ContentType.JSON)
      .get("/456/total")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isEqualTo(123);

    jsonPath = given()
      .spec(requestSpecification)
      .accept(ContentType.JSON)
      .get("/123/total")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isEqualTo(7161);

    jsonPath = given()
      .spec(requestSpecification)
      .accept(ContentType.JSON)
      .get("/123/2019/04")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isEqualTo(6541);

    jsonPath = given()
      .spec(requestSpecification)
      .accept(ContentType.JSON)
      .get("/123/2019/05")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isEqualTo(620);

    jsonPath = given()
      .spec(requestSpecification)
      .accept(ContentType.JSON)
      .get("/123/2019/05/20")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isEqualTo(200);
  }

  @Test
  @DisplayName("Check for HTTP 404 when there is no activity in the dataset")
  void check404() {
    given()
      .spec(requestSpecification)
      .accept(ContentType.JSON)
      .get("/123/2019/05/18")
      .then()
      .assertThat()
      .statusCode(404);

    given()
      .spec(requestSpecification)
      .accept(ContentType.JSON)
      .get("/123/2019/03")
      .then()
      .assertThat()
      .statusCode(404);

    given()
      .spec(requestSpecification)
      .accept(ContentType.JSON)
      .get("/122/total")
      .then()
      .assertThat()
      .statusCode(404);
  }

  @Test
  @DisplayName("Check for bad requests (HTTP 404)")
  void check400() {
    given()
      .spec(requestSpecification)
      .accept(ContentType.JSON)
      .get("/123/a/b/c")
      .then()
      .assertThat()
      .statusCode(400);

    given()
      .spec(requestSpecification)
      .accept(ContentType.JSON)
      .get("/123/a/b")
      .then()
      .assertThat()
      .statusCode(400);
  }
}
