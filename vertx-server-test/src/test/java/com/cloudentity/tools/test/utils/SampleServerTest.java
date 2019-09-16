package com.cloudentity.tools.test.utils;

import io.restassured.RestAssured;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertTrue;

public class SampleServerTest extends VertxServerTest {

  @Test
  public void shouldSetupApp() {
    assertTrue(true);
  }

  @Test
  public void shouldReturnStaticResponse() {
    RestAssured.port = 7788;
    RestAssured
      .given()
        .get("/test")
      .then().assertThat()
        .body(equalTo("OK"));
  }

  @Override
  protected String getMainVerticle() {
    return "com.cloudentity.tools.vertx.server.VertxBootstrap";
  }
}
