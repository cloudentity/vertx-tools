package com.cloudentity.tools.vertx.server.api.routes.impl;

import com.cloudentity.tools.vertx.server.api.routes.RouteService;
import com.cloudentity.tools.vertx.server.api.routes.RouteVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

public class DocsRoute extends RouteVerticle {

  @Override
  public void handle(RoutingContext routingContext) {
    StaticHandler.create("docs").handle(routingContext);
  }
}

