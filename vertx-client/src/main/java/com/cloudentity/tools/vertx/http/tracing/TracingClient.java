package com.cloudentity.tools.vertx.http.tracing;

import com.cloudentity.tools.vertx.http.builder.RequestCtxBuilder;
import com.cloudentity.tools.vertx.tracing.TracingContext;

public class TracingClient {
  public static RequestCtxBuilder inject(RequestCtxBuilder builder, TracingContext ctx) {
    return ctx.foldOverContext(builder, (e, b) -> b.putHeader(e.getKey(), e.getValue()));
  }
}
