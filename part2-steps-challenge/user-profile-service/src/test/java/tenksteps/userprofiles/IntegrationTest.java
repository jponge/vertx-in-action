package tenksteps.userprofiles;

import io.reactivex.Maybe;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.IndexOptions;
import io.vertx.ext.mongo.MongoClientDeleteResult;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.mongo.MongoClient;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
@DisplayName("User profile API integration tests")
@Testcontainers
class IntegrationTest {

  @Container
  private static final DockerComposeContainer CONTAINERS = new DockerComposeContainer(new File("../docker-compose.yml"));

  private static RequestSpecification requestSpecification;

  @BeforeAll
  static void prepareSpec() {
    requestSpecification = new RequestSpecBuilder()
      .addFilters(asList(new ResponseLoggingFilter(), new RequestLoggingFilter()))
      .setBaseUri("http://localhost:3000/")
      .build();
  }

  private MongoClient mongoClient;

  @BeforeEach
  void setup(Vertx vertx, VertxTestContext testContext) {
    JsonObject mongoConfig = new JsonObject()
      .put("host", "localhost")
      .put("port", 27017)
      .put("db_name", "profiles");

    mongoClient = MongoClient.createShared(vertx, mongoConfig);

    mongoClient
      .rxCreateIndexWithOptions("user", new JsonObject().put("username", 1), new IndexOptions().unique(true))
      .andThen(mongoClient.rxCreateIndexWithOptions("user", new JsonObject().put("deviceId", 1), new IndexOptions().unique(true)))
      .andThen(dropAllUsers())
      .flatMapSingle(res -> vertx.rxDeployVerticle(new UserProfileApiVerticle()))
      .subscribe(
        ok -> testContext.completeNow(),
        testContext::failNow);
  }

  private Maybe<MongoClientDeleteResult> dropAllUsers() {
    return mongoClient.rxRemoveDocuments("user", new JsonObject());
  }

  @AfterEach
  void cleanup(VertxTestContext testContext) {
    dropAllUsers()
      .doFinally(mongoClient::close)
      .subscribe(
        ok -> testContext.completeNow(),
        testContext::failNow);
  }

  private JsonObject basicUser() {
    return new JsonObject()
      .put("username", "abc")
      .put("password", "123")
      .put("email", "abc@email.me")
      .put("city", "Lyon")
      .put("deviceId", "a1b2c3")
      .put("makePublic", true);
  }

  @Test
  @DisplayName("Register a user")
  void register() {
    String response = given()
      .spec(requestSpecification)
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(basicUser().encode())
      .when()
      .post("/register")
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .asString();
    assertThat(response).isEmpty();
  }

  @Test
  @DisplayName("Failing to register with an existing user name")
  void registerExistingUser() {
    given()
      .spec(requestSpecification)
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(basicUser().encode())
      .when()
      .post("/register")
      .then()
      .assertThat()
      .statusCode(200);

    given()
      .spec(requestSpecification)
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(basicUser().encode())
      .when()
      .post("/register")
      .then()
      .assertThat()
      .statusCode(409);
  }

  @Test
  @DisplayName("Failing to register a user with an already existing device id")
  void registerExistingDeviceId(VertxTestContext testContext) {
    given()
      .spec(requestSpecification)
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(basicUser().encode())
      .when()
      .post("/register")
      .then()
      .assertThat()
      .statusCode(200);

    JsonObject user = basicUser()
      .put("username", "Bean");

    given()
      .spec(requestSpecification)
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(user.encode())
      .when()
      .post("/register")
      .then()
      .assertThat()
      .statusCode(409);

    mongoClient
      .rxFindOne("user", new JsonObject().put("username", "Bean"), new JsonObject())
      .subscribe(
        found -> testContext.failNow(new IllegalStateException("Incomplete document still inserted: " + found.encode())),
        testContext::failNow,
        testContext::completeNow);
  }

  @Test
  @DisplayName("Failing to register with missing fields")
  void registerWIthMissingFields() {
    given()
      .spec(requestSpecification)
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(new JsonObject().encode())
      .when()
      .post("/register")
      .then()
      .assertThat()
      .statusCode(400);
  }

  @Test
  @DisplayName("Failing to register with incorrect field data")
  void registerWithWrongFields() {
    JsonObject user = basicUser().put("username", "a b c  ");
    given()
      .spec(requestSpecification)
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(user.encode())
      .when()
      .post("/register")
      .then()
      .assertThat()
      .statusCode(400);

    user = basicUser().put("deviceId", "@123");
    given()
      .spec(requestSpecification)
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(user.encode())
      .when()
      .post("/register")
      .then()
      .assertThat()
      .statusCode(400);

    user = basicUser().put("password", "    ");
    given()
      .spec(requestSpecification)
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(user.encode())
      .when()
      .post("/register")
      .then()
      .assertThat()
      .statusCode(400);
  }

  @Test
  @DisplayName("Register a user then fetch it")
  void registerThenFetch() {
    JsonObject user = basicUser();

    with()
      .spec(requestSpecification)
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(user.encode())
      .post("/register");

    JsonPath jsonPath = given()
      .spec(requestSpecification)
      .accept(ContentType.JSON)
      .when()
      .get("/" + user.getString("username"))
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getString("username")).isEqualTo(user.getString("username"));
    assertThat(jsonPath.getString("email")).isEqualTo(user.getString("email"));
    assertThat(jsonPath.getString("deviceId")).isEqualTo(user.getString("deviceId"));
    assertThat(jsonPath.getString("city")).isEqualTo(user.getString("city"));
    assertThat(jsonPath.getBoolean("makePublic")).isEqualTo(user.getBoolean("makePublic"));
    assertThat(jsonPath.getString("_id")).isNull();
    assertThat(jsonPath.getString("password")).isNull();
  }

  @Test
  @DisplayName("Fetching an unknown user")
  void fetchUnknownUser() {
    given()
      .spec(requestSpecification)
      .accept(ContentType.JSON)
      .when()
      .get("/foo-bar-baz")
      .then()
      .statusCode(404);
  }

  @Test
  @DisplayName("Register then update a user data")
  void update() {
    JsonObject original = basicUser();

    with()
      .spec(requestSpecification)
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(original.encode())
      .post("/register");

    JsonObject updated = basicUser();
    updated
      .put("deviceId", "vertx-in-action-123")
      .put("email", "vertx@email.me")
      .put("city", "Nevers")
      .put("makePublic", false)
      .put("username", "Bean");

    with()
      .spec(requestSpecification)
      .contentType(ContentType.JSON)
      .body(updated.encode())
      .put("/" + original.getString("username"))
      .then()
      .statusCode(200);

    JsonPath jsonPath = given()
      .spec(requestSpecification)
      .accept(ContentType.JSON)
      .when()
      .get("/" + original.getString("username"))
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getString("username")).isEqualTo(original.getString("username"));
    assertThat(jsonPath.getString("deviceId")).isEqualTo(original.getString("deviceId"));

    assertThat(jsonPath.getString("city")).isEqualTo(updated.getString("city"));
    assertThat(jsonPath.getString("email")).isEqualTo(updated.getString("email"));
    assertThat(jsonPath.getBoolean("makePublic")).isEqualTo(updated.getBoolean("makePublic"));
  }

  @Test
  @DisplayName("Authenticate an existing user")
  void authenticate() {
    JsonObject user = basicUser();
    JsonObject request = new JsonObject()
      .put("username", user.getString("username"))
      .put("password", user.getString("password"));

    with()
      .spec(requestSpecification)
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(user.encode())
      .post("/register");

    with()
      .spec(requestSpecification)
      .contentType(ContentType.JSON)
      .body(request.encode())
      .post("/authenticate")
      .then()
      .assertThat()
      .statusCode(200);
  }

  @Test
  @DisplayName("Failing at authenticating an unknown user")
  void authenticateMissingUser() {
    JsonObject request = new JsonObject()
      .put("username", "Bean")
      .put("password", "abc");

    with()
      .spec(requestSpecification)
      .contentType(ContentType.JSON)
      .body(request.encode())
      .post("/authenticate")
      .then()
      .assertThat()
      .statusCode(401);
  }

  @Test
  @DisplayName("Find who owns a device")
  void whoHas() {
    JsonObject user = basicUser();

    with()
      .spec(requestSpecification)
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(user.encode())
      .post("/register");

    JsonPath jsonPath = given()
      .spec(requestSpecification)
      .accept(ContentType.JSON)
      .when()
      .get("/owns/" + user.getString("deviceId"))
      .then()
      .assertThat()
      .statusCode(200)
      .extract()
      .jsonPath();

    assertThat(jsonPath.getString("username")).isEqualTo(user.getString("username"));
    assertThat(jsonPath.getString("deviceId")).isEqualTo(user.getString("deviceId"));
    assertThat(jsonPath.getString("city")).isNull();

    given()
      .spec(requestSpecification)
      .accept(ContentType.JSON)
      .when()
      .get("/owns/404")
      .then()
      .assertThat()
      .statusCode(404);
  }
}
