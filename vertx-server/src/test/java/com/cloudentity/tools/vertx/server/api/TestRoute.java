package com.cloudentity.tools.vertx.server.api;

import com.cloudentity.tools.vertx.server.api.routes.RouteVerticle;
import io.vertx.ext.web.RoutingContext;

public class TestRoute extends RouteVerticle {
  @Override
  public void handle(RoutingContext ctx) {
    ctx.response().setStatusCode(201).putHeader("test-header", "test-header-value").end();
  }
}
