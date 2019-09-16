package com.cloudentity.tools.vertx.server.api;

import com.cloudentity.tools.vertx.bus.ServiceVerticle;
import com.cloudentity.tools.vertx.server.api.filters.RouteFilter;
import com.cloudentity.tools.vertx.server.api.filters.RouteFilterConfigValidation;
import com.cloudentity.tools.vertx.server.api.filters.ScalaRouteFilterVerticle;
import com.cloudentity.tools.vertx.server.api.routes.RouteService;
import com.cloudentity.tools.vertx.server.api.routes.RouteVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

import java.util.Optional;

public class PassingFilter extends ServiceVerticle implements RouteFilter {
  @Override
  public Optional<String> vertxServiceAddressPrefix() {
    return Optional.ofNullable(verticleId());
  }

  @Override
  public Future<Void> applyFilter(RoutingContext ctx, String confRaw) {
    ctx.response().putHeader("passing-filter", "passing-filter");
    ctx.next();

    return Future.succeededFuture();
  }

  @Override
  public Future<RouteFilterConfigValidation> validateConfig(String rawConf) {
    return Future.succeededFuture(RouteFilterConfigValidation.success());
  }
}
