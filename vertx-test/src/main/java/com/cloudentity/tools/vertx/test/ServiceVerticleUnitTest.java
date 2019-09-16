package com.cloudentity.tools.vertx.test;

import com.cloudentity.tools.vertx.bus.ServiceClientFactory;
import com.cloudentity.tools.vertx.bus.ServiceVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

/**
 * Use it when you want to test ServiceVerticle that doesn't depend on other ServiceVerticles.
 * Otherwise use {@link ServiceVerticleIntegrationTest}
 *
 * For sample usage look at the SampleVerticleUnitTest.
 */
abstract public class ServiceVerticleUnitTest<B extends ServiceVerticle, A> extends VertxUnitTest {
  abstract protected B createVerticle();

  public JsonObject defaultConfig() {
    return new JsonObject();
  }

  public A client() {
    return (A) ServiceClientFactory.make(vertx().eventBus(), createVerticle().vertxServices().get(0));
  }

  public Future<String> deployVerticle() {
    return VertxDeployTest.deployWithConfig(vertx(), createVerticle(), defaultConfig());
  }

  public Future<String> deployVerticle(String verticleId, JsonObject config) {
    return VertxDeployTest.deployWithConfig(vertx(), createVerticle(), verticleId, config);
  }

  public Future<String> deployVerticle(JsonObject config) {
    return VertxDeployTest.deployWithConfig(vertx(), createVerticle(), config);
  }
}
