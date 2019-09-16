package com.cloudentity.tools.vertx.server.api

import com.cloudentity.tools.vertx.server.api.routes.ScalaRouteVerticle
import io.vertx.ext.web.RoutingContext

class ScalaTestRoute extends ScalaRouteVerticle {
  override def handle (ctx: RoutingContext): Unit = {
    ctx.response.setStatusCode(201).putHeader("test-header", "test-header-value").end()
  }
}
