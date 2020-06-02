package com.cloudentity.tools.vertx.test;

import com.cloudentity.tools.vertx.bus.VertxEndpointClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;

public class SampleVertxModuleTest extends VertxModuleTest {

  JsonObject extraVerticleConfig = new JsonObject().put("main", "com.cloudentity.tools.vertx.test.SampleVerticle").put("prefix", true);
  JsonObject extraConfig = new JsonObject().put("registry:components", new JsonObject().put("extra", extraVerticleConfig));

  @Test
  public void shouldDeployModule(TestContext ctx) {
    deployModule("module-a", "components")
      .compose(x -> VertxEndpointClient.make(vertx(), SampleService.class, Optional.of("a")).getId().map(id -> ctx.assertEquals("a", id)))
      .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void shouldDeployModuleWithFileConfig(TestContext ctx) {
    deployModuleWithFileConfig("module-a", "src/test/resources/extra.json", "components")
      .compose(x -> VertxEndpointClient.make(vertx(), SampleService.class, Optional.of("a")).getId().map(id -> ctx.assertEquals("a", id)))
      .compose(x -> VertxEndpointClient.make(vertx(), SampleService.class, Optional.of("extra")).getId().map(id -> ctx.assertEquals("extra", id)))
      .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void shouldDeployModuleWithJsonObject(TestContext ctx) {
    deployModule("module-a", extraConfig, "components")
      .compose(x -> VertxEndpointClient.make(vertx(), SampleService.class, Optional.of("a")).getId().map(id -> ctx.assertEquals("a", id)))
      .compose(x -> VertxEndpointClient.make(vertx(), SampleService.class, Optional.of("extra")).getId().map(id -> ctx.assertEquals("extra", id)))
      .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void shouldDeployModules(TestContext ctx) {
    deployModules(Arrays.asList("module-a", "module-b"), "components")
      .compose(x -> VertxEndpointClient.make(vertx(), SampleService.class, Optional.of("a")).getId().map(id -> ctx.assertEquals("a", id)))
      .compose(x -> VertxEndpointClient.make(vertx(), SampleService.class, Optional.of("b")).getId().map(id -> ctx.assertEquals("b", id)))
      .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void shouldDeployModulesWithFileConfig(TestContext ctx) {
    deployModulesWithFileConfig(Arrays.asList("module-a", "module-b"), "src/test/resources/extra.json", "components")
      .compose(x -> VertxEndpointClient.make(vertx(), SampleService.class, Optional.of("a")).getId().map(id -> ctx.assertEquals("a", id)))
      .compose(x -> VertxEndpointClient.make(vertx(), SampleService.class, Optional.of("b")).getId().map(id -> ctx.assertEquals("b", id)))
      .compose(x -> VertxEndpointClient.make(vertx(), SampleService.class, Optional.of("extra")).getId().map(id -> ctx.assertEquals("extra", id)))
      .onComplete(ctx.asyncAssertSuccess());
  }

  @Test
  public void shouldDeployModulesWithJsonObject(TestContext ctx) {
    deployModules(Arrays.asList("module-a", "module-b"), extraConfig, "components")
      .compose(x -> VertxEndpointClient.make(vertx(), SampleService.class, Optional.of("a")).getId().map(id -> ctx.assertEquals("a", id)))
      .compose(x -> VertxEndpointClient.make(vertx(), SampleService.class, Optional.of("b")).getId().map(id -> ctx.assertEquals("b", id)))
      .compose(x -> VertxEndpointClient.make(vertx(), SampleService.class, Optional.of("extra")).getId().map(id -> ctx.assertEquals("extra", id)))
      .onComplete(ctx.asyncAssertSuccess());
  }

}

