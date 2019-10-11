package com.cloudentity.tools.vertx.test;

import com.cloudentity.tools.vertx.server.api.routes.RouteVerticle;
import io.vertx.ext.web.RoutingContext;

public class TestRoute extends RouteVerticle {
  @Override
  public void handle(RoutingContext ctx) {
    ctx.response().end(getConfig().getString("static-response"));
  }
}
