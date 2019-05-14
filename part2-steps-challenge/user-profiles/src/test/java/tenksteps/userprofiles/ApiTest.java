package tenksteps.userprofiles;

import io.reactivex.Single;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.IndexOptions;
import io.vertx.ext.mongo.MongoClientDeleteResult;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.mongo.MongoClient;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

import static io.restassured.RestAssured.*;
import static io.restassured.matcher.RestAssuredMatchers.*;
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
      .put("login", "abc")
      .put("password", "123")
      .put("city", "Lyon")
      .put("deviceId", "a1b2c3")
      .put("makePublic", true);
  }

  @Test
  void register() {
    JsonPath jsonPath = given()
      .spec(requestSpecification)
      .contentType(ContentType.JSON)
      .accept(ContentType.JSON)
      .body(basicUser().encode())
      .when()
      .post("/register")
      .then()
      .assertThat()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .and()
      .extract()
      .jsonPath();

    assertThat(jsonPath.getString("id")).isNotNull();
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
      .statusCode(200)
      .contentType(ContentType.JSON);

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
}
