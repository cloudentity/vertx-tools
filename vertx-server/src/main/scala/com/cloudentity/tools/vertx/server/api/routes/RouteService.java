package com.cloudentity.tools.vertx.server.api.routes;

import com.cloudentity.tools.vertx.bus.VertxEndpoint;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

public interface RouteService {
  @VertxEndpoint
  Future handleRequest(RoutingContext ctx);
}
