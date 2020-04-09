package tenksteps.publicapi;

import io.reactivex.Completable;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.reactivex.sqlclient.Tuple;
import io.vertx.sqlclient.PoolOptions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static io.restassured.RestAssured.given;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Integration tests for the public API")
@Testcontainers
class IntegrationTest {

  @Container
  private static final DockerComposeContainer CONTAINERS = new DockerComposeContainer(new File("../docker-compose.yml"));

  private RequestSpecification requestSpecification;

  @BeforeAll
  void prepareSpec(Vertx vertx, VertxTestContext testContext) {
    requestSpecification = new RequestSpecBuilder()
      .addFilters(asList(new ResponseLoggingFilter(), new RequestLoggingFilter()))
      .setBaseUri("http://localhost:4000/")
      .setBasePath("/api/v1")
      .build();

    String insertQuery = "INSERT INTO stepevent VALUES($1, $2, $3::timestamp, $4)";
    List<Tuple> data = Arrays.asList(
      Tuple.of("a1b2c3", 1, LocalDateTime.of(2019, 6, 16, 10, 3), 250),
      Tuple.of("a1b2c3", 2, LocalDateTime.of(2019, 6, 16, 12, 30), 1000),
      Tuple.of("a1b2c3", 3, LocalDateTime.of(2019, 6, 15, 23, 00), 5005)
    );
    PgPool pgPool = PgPool.pool(vertx, new PgConnectOptions()
      .setHost("localhost")
      .setDatabase("postgres")
      .setUser("postgres")
      .setPassword("vertx-in-action"), new PoolOptions());

    pgPool.preparedQuery(insertQuery)
      .rxExecuteBatch(data)
      .ignoreElement()
      .andThen(vertx.rxDeployVerticle(new PublicApiVerticle()))
      .ignoreElement()
      .andThen(vertx.rxDeployVerticle("tenksteps.userprofiles.UserProfileApiVerticle"))
      .ignoreElement()
      .andThen(vertx.rxDeployVerticle("tenksteps.activities.ActivityApiVerticle"))
      .ignoreElement()
      .andThen(Completable.fromAction(pgPool::close))
      .subscribe(testContext::completeNow, testContext::failNow);
  }

  private final HashMap<String, JsonObject> registrations = new HashMap<String, JsonObject>() {
    {
      put("Foo", new JsonObject()
        .put("username", "Foo")
        .put("password", "foo-123")
        .put("email", "foo@email.me")
        .put("city", "Lyon")
        .put("deviceId", "a1b2c3")
        .put("makePublic", true));

      put("Bar", new JsonObject()
        .put("username", "Bar")
        .put("password", "bar-#$69")
        .put("email", "bar@email.me")
        .put("city", "Tassin-La-Demi-Lune")
        .put("deviceId", "def1234")
        .put("makePublic", false));
    }
  };

  @Test
  @Order(1)
  @DisplayName("Register some users")
  void registerUsers() {
    registrations.forEach((key, registration) -> {
      given(requestSpecification)
        .contentType(ContentType.JSON)
        .body(registration.encode())
        .post("/register")
        .then()
        .assertThat()
        .statusCode(200);
    });
  }

  private final HashMap<String, String> tokens = new HashMap<>();

  @Test
  @Order(2)
  @DisplayName("Get JWT tokens to access the API")
  void obtainToken() {
    registrations.forEach((key, registration) -> {

      JsonObject login = new JsonObject()
        .put("username", key)
        .put("password", registration.getString("password"));

      String token = given(requestSpecification)
        .contentType(ContentType.JSON)
        .body(login.encode())
        .post("/token")
        .then()
        .assertThat()
        .statusCode(200)
        .contentType("application/jwt")
        .extract()
        .asString();

      assertThat(token)
        .isNotNull()
        .isNotBlank();

      tokens.put(key, token);
    });
  }

  @Test
  @Order(3)
  @DisplayName("Fetch a user data")
  void fetchSomeUser() {
    JsonPath jsonPath = given(requestSpecification)
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .get("/Foo")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    JsonObject foo = registrations.get("Foo");
    List<String> props = asList("username", "email", "city", "deviceId");
    props.forEach(prop -> assertThat(jsonPath.getString(prop)).isEqualTo(foo.getString(prop)));
    assertThat(jsonPath.getBoolean("makePublic")).isEqualTo(foo.getBoolean("makePublic"));
  }

  @Test
  @Order(4)
  @DisplayName("Fail at fetching another user data")
  void failToFatchAnotherUser() {
    given(requestSpecification)
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .get("/Bar")
      .then()
      .assertThat()
      .statusCode(403);
  }

  @Test
  @Order(5)
  @DisplayName("Update some user data")
  void updateSomeUser() {
    String originalCity = registrations.get("Foo").getString("city");
    boolean originalMakePublic = registrations.get("Foo").getBoolean("makePublic");
    JsonObject updates = new JsonObject()
      .put("city", "Nevers")
      .put("makePublic", false);

    given(requestSpecification)
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .contentType(ContentType.JSON)
      .body(updates.encode())
      .put("/Foo")
      .then()
      .assertThat()
      .statusCode(200);

    JsonPath jsonPath = given(requestSpecification)
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .get("/Foo")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getString("city")).isEqualTo(updates.getString("city"));
    assertThat(jsonPath.getBoolean("makePublic")).isEqualTo(updates.getBoolean("makePublic"));

    updates
      .put("city", originalCity)
      .put("makePublic", originalMakePublic);

    given(requestSpecification)
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .contentType(ContentType.JSON)
      .body(updates.encode())
      .put("/Foo")
      .then()
      .assertThat()
      .statusCode(200);

    jsonPath = given(requestSpecification)
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .get("/Foo")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getString("city")).isEqualTo(originalCity);
    assertThat(jsonPath.getBoolean("makePublic")).isEqualTo(originalMakePublic);
  }

  @Test
  @Order(6)
  @DisplayName("Check some user stats")
  void checkSomeUserStats() {
    JsonPath jsonPath = given(requestSpecification)
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .get("/Foo/total")
      .then()
      .assertThat()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isNotNull().isEqualTo(6255);

    jsonPath = given(requestSpecification)
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .get("/Foo/2019/06")
      .then()
      .assertThat()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isNotNull().isEqualTo(6255);

    jsonPath = given(requestSpecification)
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .get("/Foo/2019/06/15")
      .then()
      .assertThat()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getInt("count")).isNotNull().isEqualTo(5005);
  }

  @Test
  @Order(7)
  @DisplayName("Check that you cannot access somebody else's stats")
  void cannotAccessSomebodyElseStats() {
    given(requestSpecification)
      .headers("Authorization", "Bearer " + tokens.get("Foo"))
      .get("/Bar/total")
      .then()
      .assertThat()
      .statusCode(403);
  }
}
