package com.cloudentity.tools.vertx.verticles;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class VertxDeploy {
  public static Future<String> deploy(Vertx vertx, Verticle verticle) {
    return deploy(vertx, verticle, new DeploymentOptions());
  }

  public static Future<String> deploy(Vertx vertx, Verticle verticle, String verticleId) {
    JsonObject deploymentConfig = new JsonObject().put("verticleId", verticleId);
    return deploy(vertx, verticle, new DeploymentOptions().setConfig(deploymentConfig));
  }

  public static Future<String> deploy(Vertx vertx, String verticle, String verticleId) {
    JsonObject deploymentConfig = new JsonObject().put("verticleId", verticleId);
    return deploy(vertx, verticle, new DeploymentOptions().setConfig(deploymentConfig));
  }

  public static Future<String> deploy(Vertx vertx, Verticle verticle, String verticleId, String configPath) {
    JsonObject deploymentConfig = new JsonObject().put("verticleId", verticleId).put("configPath", configPath);
    return deploy(vertx, verticle, new DeploymentOptions().setConfig(deploymentConfig));
  }

  public static Future<String> deploy(Vertx vertx, Verticle verticle, DeploymentOptions opts) {
    Future promise = Future.future();
    vertx.deployVerticle(verticle, opts, promise);
    return promise;
  }

  public static Future<String> deploy(Vertx vertx, String verticle, DeploymentOptions opts) {
    Future promise = Future.future();
    vertx.deployVerticle(verticle, opts, promise);
    return promise;
  }

  public static Future<String> deploy(Vertx vertx, String verticle) {
    return deploy(vertx, verticle, new DeploymentOptions());
  }

  public static Future<String> undeploy(Vertx vertx, String deploymentId) {
    Future promise = Future.future();
    vertx.undeploy(deploymentId, promise);
    return promise;
  }
}
