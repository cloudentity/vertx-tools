package com.cloudentity.tools.vertx.server.api.routes.impl;

import com.cloudentity.tools.vertx.server.api.handlers.AccessLogHandler;
import com.cloudentity.tools.vertx.server.api.routes.RouteVerticle;
import io.vertx.ext.web.RoutingContext;

public class AccessLogRoute extends RouteVerticle {
  private AccessLogHandler accessLogHandler;

  @Override
  protected void initService() {
    accessLogHandler = new AccessLogHandler(getTracing());
  }

  @Override
  public void handle(RoutingContext routingContext) {
    accessLogHandler.handle(routingContext);
  }
}
