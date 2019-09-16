package com.cloudentity.tools.vertx.server.api;

import com.cloudentity.tools.vertx.server.api.routes.RouteVerticle;
import io.vertx.ext.web.RoutingContext;

public class ExceptionThrowingRoute extends RouteVerticle {
  @Override
  protected void handle(RoutingContext ctx) {
    throw new RuntimeException();
  }
}
