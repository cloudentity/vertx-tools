package com.cloudentity.tools.vertx.test;

import com.cloudentity.tools.vertx.bus.VertxEndpointClient;
import com.cloudentity.tools.vertx.bus.ServiceVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

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
    return (A) VertxEndpointClient.make(vertx(), getGenericClass());
  }

  private Class<A> inferedClass;

  public Class<A> getGenericClass(){
    if(inferedClass == null){
      Type mySuperclass = getClass().getGenericSuperclass();
      Type tType = ((ParameterizedType)mySuperclass).getActualTypeArguments()[1];
      String className = tType.toString().split(" ")[1];
      try {
        inferedClass = (Class<A>) Class.forName(className);
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
    }
    return inferedClass;
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
