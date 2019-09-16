package com.cloudentity.tools.vertx.server.api.tracing

import com.cloudentity.tools.vertx.tracing.{TracingContext, TracingManager}
import io.vertx.ext.web.RoutingContext

object RoutingWithTracingS {
  def getOrCreate(ctx: RoutingContext, tracing: TracingManager): TracingContext =
    RoutingWithTracing.getOrCreate(ctx, tracing)
}
