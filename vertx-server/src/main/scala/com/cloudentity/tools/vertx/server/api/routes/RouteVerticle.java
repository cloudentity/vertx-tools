package com.cloudentity.tools.vertx.server.api.routes;

import com.cloudentity.tools.vertx.bus.ServiceVerticle;
import com.cloudentity.tools.vertx.server.api.tracing.RoutingWithTracing;
import com.cloudentity.tools.vertx.tracing.TracingContext;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public abstract class RouteVerticle extends ServiceVerticle implements RouteService {
  private static final Logger log = LoggerFactory.getLogger(RouteVerticle.class);

  @Override
  protected Optional<String> vertxServiceAddressPrefix() {
    if (verticleId() == null) {
      String errMsg = this.getClass() + " does not have 'verticleId'";
      log.error(errMsg);
      throw new RuntimeException(errMsg);
    } else {
      return Optional.of(verticleId());
    }
  }

  @Override
  public Future handleRequest(RoutingContext ctx) {
    handle(ctx);
    return Future.succeededFuture();
  }

  protected TracingContext getTracingContext(RoutingContext ctx) {
    return RoutingWithTracing.getOrCreate(ctx, getTracing());
  }

  abstract protected void handle(RoutingContext ctx);
}
