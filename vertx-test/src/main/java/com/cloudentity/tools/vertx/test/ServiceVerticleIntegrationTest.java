package com.cloudentity.tools.vertx.test;

import com.google.common.collect.Lists;
import com.cloudentity.tools.vertx.bus.ServiceClientFactory;
import com.cloudentity.tools.vertx.conf.fixed.FixedConfVerticle;
import com.cloudentity.tools.vertx.verticles.VertxDeploy;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;

import java.util.*;

/**
 * Use it when you have interdependent verticles and you want to test either real implementations of all verticles
 * or a mix of real and mocked verticles.
 * 
 * For sample usage look at the SampleVerticleIntegrationTest.
 */
abstract public class ServiceVerticleIntegrationTest extends VertxUnitTest {
  public <T> T client(Class<T> vertxService) {
    return ServiceClientFactory.make(vertx().eventBus(), vertxService);
  }

  public <T> T client(Class<T> vertxService, String addressPrefix) {
    return ServiceClientFactory.make(vertx().eventBus(), vertxService, Optional.ofNullable(addressPrefix));
  }

  public JsonObject defaultConfig() {
    return new JsonObject();
  }

  private <A> LinkedList<A> toLinkedList(A... verticles) {
    return Lists.newLinkedList(Lists.newArrayList(verticles));
  }

  private <A> LinkedList<A> toLinkedList(Set<A> verticles) {
    return Lists.newLinkedList(verticles);
  }

  Map.Entry<String, Verticle> verticle(String verticleId, Verticle verticle) {
    return new AbstractMap.SimpleEntry<>(verticleId, verticle);
  }

  public Future<String> deployVerticles(Map<String, Verticle> verticles) {
    return FixedConfVerticle.deploy(vertx(), defaultConfig())
      .compose(x -> deployRecursively(toLinkedList(verticles.entrySet()), Future.succeededFuture()));
  }

  public Future<String> deployVerticles(String verticleId, JsonObject config, Map<String, Verticle> verticles) {
    return FixedConfVerticle.deploy(vertx(), verticleId, config)
      .compose(x -> deployRecursively(toLinkedList(verticles.entrySet()), Future.succeededFuture()));
  }

  public Future<String> deployVerticles(JsonObject config, Map<String, Verticle> verticles) {
    return FixedConfVerticle.deploy(vertx(), config)
      .compose(x -> deployRecursively(toLinkedList(verticles.entrySet()), Future.succeededFuture()));
  }

  public Future<String> deployVerticles(Map.Entry<String, Verticle>... verticles) {
    return FixedConfVerticle.deploy(vertx(), defaultConfig())
    .compose(x -> deployRecursively(toLinkedList(verticles), Future.succeededFuture()));
  }

  public Future<String> deployVerticles(String verticleId, JsonObject config, Map.Entry<String, Verticle>... verticles) {
    return FixedConfVerticle.deploy(vertx(), verticleId, config)
    .compose(x -> deployRecursively(toLinkedList(verticles), Future.succeededFuture()));
  }

  public Future<String> deployVerticles(JsonObject config, Map.Entry<String, Verticle>... verticles) {
    return FixedConfVerticle.deploy(vertx(), config)
    .compose(x -> deployRecursively(toLinkedList(verticles), Future.succeededFuture()));
  }

  public Future<String> deployRecursively(LinkedList<Map.Entry<String, Verticle>> verticles, Future<String> agg) {
    if (verticles.isEmpty()) {
      return agg;
    } else {
      Map.Entry<String, Verticle> verticle = verticles.poll();
      return deployRecursively(verticles, agg.compose(x -> {
        DeploymentOptions opts = new DeploymentOptions().setConfig(new JsonObject().put("verticleId", verticle.getKey()));
        return VertxDeploy.deploy(vertx(), verticle.getValue(), opts);
      }));
    }
  }
}
