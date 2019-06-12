package tenksteps.publicapi;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static java.util.Arrays.asList;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Integration tests for the public API")
@Testcontainers
class IntegrationTest {

  @Container
  private static final DockerComposeContainer CONTAINERS = new DockerComposeContainer(new File("../docker-compose.yml"));

  private RequestSpecification requestSpecification;

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

  @BeforeAll
  void prepareSpec(Vertx vertx, VertxTestContext testContext) {
    requestSpecification = new RequestSpecBuilder()
      .addFilters(asList(new ResponseLoggingFilter(), new RequestLoggingFilter()))
      .setBaseUri("http://localhost:4000/")
      .setBasePath("/api/v1")
      .build();

    vertx
      .rxDeployVerticle(new PublicApiVerticle())
      .ignoreElement()
      .andThen(vertx.rxDeployVerticle("tenksteps.userprofiles.UserProfileApiVerticle"))
      .ignoreElement()
      .subscribe(testContext::completeNow, testContext::failNow);
  }

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
}
