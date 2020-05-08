package com.cloudentity.tools.vertx.sd.test

import com.cloudentity.tools.vertx.bus.VertxEndpointClient
import com.cloudentity.tools.vertx.conf.fixed.FixedConfVerticle
import com.cloudentity.tools.vertx.sd.SdVerticle
import com.cloudentity.tools.vertx.sd.provider.FixedSdProvider
import com.cloudentity.tools.vertx.verticles.VertxDeploy
import io.vertx.core.json.{JsonArray, JsonObject}
import io.vertx.core.{Future, Vertx}

object SdTestTools {
  def deployStaticDiscovery(vertx: Vertx, staticSdProviderConf: JsonObject): Future[String] =
    FixedConfVerticle.deploy(vertx, FixedSdProvider.VERTICLE_ID, new JsonObject().put("records", new JsonArray().add(staticSdProviderConf)))
      .compose(_ => VertxDeploy.deploy(vertx, new SdVerticle))
      .compose(_ => VertxDeploy.deploy(vertx, new FixedSdProvider))

  def deployEventBusDiscovery(vertx: Vertx): Future[EventBusSdProviderService] =
    FixedConfVerticle.deploy(vertx, new JsonObject())
      .compose(_ => VertxDeploy.deploy(vertx, new SdVerticle))
      .compose(_ => VertxDeploy.deploy(vertx, new EventBusSdProvider))
      .compose(_ => Future.succeededFuture(eventBusSdProvider(vertx)))

  def eventBusSdProvider(vertx: Vertx): EventBusSdProviderService = VertxEndpointClient.make(vertx, classOf[EventBusSdProviderService])
}
