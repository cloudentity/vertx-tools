package com.cloudentity.tools.vertx.server.api.filters;

import com.cloudentity.tools.vertx.bus.VertxEndpoint;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

public interface RouteFilter {
  @VertxEndpoint
  Future applyFilter(RoutingContext ctx, String rawJsonConf);

  @VertxEndpoint
  Future<RouteFilterConfigValidation> validateConfig(String rawJsonConf);
}
