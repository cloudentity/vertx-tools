package com.cloudentity.tools.vertx.test;

import com.cloudentity.tools.vertx.conf.fixed.FixedConfVerticle;
import com.cloudentity.tools.vertx.verticles.VertxDeploy;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.UUID;


public class VertxDeployTest {

  public static Future<String> deployWithConfig(Vertx vertx, Verticle verticle, JsonObject config) {
    String verticleId = UUID.randomUUID().toString();
    return deployWithConfig(vertx, verticle, verticleId, config);
  }

  /**
   * Deploys given verticle with corresponding conf verticle which holds verticleConfig configuration
   *
   * @param vertx - vertx instance
   * @param verticle - verticle instance
   * @param verticleId - verticleId - id of verticle, use another signature if you want to use auto generated id
   * @param verticleConfig - config of the verticle
   * @return
   */
  public static Future<String> deployWithConfig(Vertx vertx, Verticle verticle, String verticleId, JsonObject verticleConfig) {
    DeploymentOptions opts = new DeploymentOptions().setConfig(new JsonObject().put("verticleId", verticleId));
    return FixedConfVerticle.deploy(vertx, verticleId, verticleConfig)
        .compose(x -> VertxDeploy.deploy(vertx, verticle, opts));
  }

}
