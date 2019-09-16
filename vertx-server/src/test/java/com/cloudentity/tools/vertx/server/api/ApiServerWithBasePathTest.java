package com.cloudentity.tools.vertx.server.api;

import io.vertx.ext.unit.TestContext;
import org.junit.Test;

import java.io.IOException;

public class ApiServerWithBasePathTest extends ApiServerTestBase {
  String configPath = "src/test/resources/api-server/conf-base-path.json";

  @Test
  public void shouldDeployWorkingJavaRouteVerticleWithBasePath(TestContext ctx) throws IOException {
    shouldDeployWorkingRouteVerticle(ctx, "/api/test", configPath);
  }

  @Test
  public void shouldDeployWorkingScalaRouteVerticleWithBasePath(TestContext ctx) throws IOException {
    shouldDeployWorkingRouteVerticle(ctx, "/api/scala-test", configPath);
  }
}
