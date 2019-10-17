package com.cloudentity.tools.vertx.server.api;

import io.vertx.ext.unit.TestContext;
import org.junit.Test;

import java.io.IOException;

public class ApiServerWithRoutesMapTest extends ApiServerTestBase {
  String configPath = "src/test/resources/api-server/conf-routes-map.json";

  @Test
  public void shouldDeployWorkingJavaRouteVerticleWithBasePath(TestContext ctx) throws IOException {
    shouldDeployWorkingRouteVerticle(ctx, "/test", configPath);
  }

  @Test
  public void shouldDeployWorkingScalaRouteVerticleWithBasePath(TestContext ctx) throws IOException {
    shouldDeployWorkingRouteVerticle(ctx, "/scala-test", configPath);
  }
}
