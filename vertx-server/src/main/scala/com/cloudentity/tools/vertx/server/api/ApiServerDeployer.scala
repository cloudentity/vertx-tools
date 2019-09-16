package com.cloudentity.tools.vertx.server.api

import com.cloudentity.tools.vertx.verticles.VertxDeploy
import io.vertx.core.{Vertx, Future => VxFuture}

object ApiServerDeployer {
  def deployServer(vertx: Vertx): VxFuture[String] =
    VertxDeploy.deploy(vertx, new ApiServerRoot())

  def deployServer(vertx: Vertx, verticleId: String): VxFuture[String] =
    VertxDeploy.deploy(vertx, new ApiServerRoot(), verticleId)
}
