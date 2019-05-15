package tenksteps.userprofiles;

import io.reactivex.Single;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(VertxExtension.class)
class ApiTest {

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
      .flatMap(res -> vertx.rxDeployVerticle(new UserProfileApiVerticle()))
      .subscribe(
        ok -> testContext.completeNow(),
        testContext::failNow);
  }

  private Single<MongoClientDeleteResult> dropAllUsers() {
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
      .put("city", "Lyon")
      .put("deviceId", "a1b2c3")
      .put("makePublic", true);
  }

  @Test
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
  void registerExistingDeviceId() {
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
  }

  @Test
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
    assertThat(jsonPath.getString("deviceId")).isEqualTo(user.getString("deviceId"));
    assertThat(jsonPath.getString("city")).isEqualTo(user.getString("city"));
    assertThat(jsonPath.getBoolean("makePublic")).isEqualTo(user.getBoolean("makePublic"));
  }

  @Test
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
    assertThat(jsonPath.getBoolean("makePublic")).isEqualTo(updated.getBoolean("makePublic"));
  }
}
