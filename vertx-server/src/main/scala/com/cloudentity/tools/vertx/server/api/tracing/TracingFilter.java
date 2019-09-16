package com.cloudentity.tools.vertx.server.api.tracing;

import com.cloudentity.tools.vertx.server.api.routes.RouteVerticle;
import io.vertx.ext.web.RoutingContext;

/**
 * This filter automatically creates a new span and finish it for all routes.
 */
public class TracingFilter extends RouteVerticle {
  @Override
  protected void handle(RoutingContext ctx) {
    if (ctx.failed()) {
      RoutingWithTracing.getOrCreate(ctx, getTracing()).logException(ctx.failure());
      ctx.next();
    } else {
      RoutingWithTracing.getOrCreate(ctx, getTracing());
      ctx.next();
    }
  }
}
