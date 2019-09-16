package com.cloudentity.tools.vertx.server.api;

import com.cloudentity.tools.vertx.bus.ServiceVerticle;
import com.cloudentity.tools.vertx.server.api.filters.RouteFilter;
import com.cloudentity.tools.vertx.server.api.filters.RouteFilterConfigValidation;
import com.cloudentity.tools.vertx.server.api.routes.RouteService;
import com.cloudentity.tools.vertx.server.api.routes.RouteVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

import java.util.Optional;

public class AbortingFilter extends ServiceVerticle implements RouteFilter {
  @Override
  public Optional<String> vertxServiceAddressPrefix() {
    return Optional.ofNullable(verticleId());
  }

  @Override
  public Future<Void> applyFilter(RoutingContext ctx, String confRaw) {
    ctx.response().putHeader("aborting-filter", "aborting-filter");
    ctx.response().setStatusCode(400).end();

    return Future.succeededFuture();
  }

  @Override
  public Future<RouteFilterConfigValidation> validateConfig(String rawConf) {
    return Future.succeededFuture(RouteFilterConfigValidation.success());
  }
}
