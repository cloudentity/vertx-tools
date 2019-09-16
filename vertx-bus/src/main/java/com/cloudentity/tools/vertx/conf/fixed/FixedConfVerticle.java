package com.cloudentity.tools.vertx.conf.fixed;

import com.cloudentity.tools.vertx.conf.ConfVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class FixedConfVerticle extends ConfVerticle {
  public static Future<String> deploy(Vertx vertx, JsonObject globalConfig) {
    Future promise = Future.future();
    vertx.deployVerticle(new FixedConfVerticle(globalConfig), promise);
    return promise;
  }

  public static Future<String> deploy(Vertx vertx, String verticleId, JsonObject verticleConfig) {
    Future promise = Future.future();
    vertx.deployVerticle(new FixedConfVerticle(verticleId, verticleConfig), promise);
    return promise;
  }

  public FixedConfVerticle(JsonObject globalConfig) {
    super(new FixedConfigRetriever(globalConfig));
  }

  public FixedConfVerticle(String verticleId, JsonObject verticleConfig) {
    super(new FixedConfigRetriever(new JsonObject().put(verticleId, verticleConfig)));
  }
}
