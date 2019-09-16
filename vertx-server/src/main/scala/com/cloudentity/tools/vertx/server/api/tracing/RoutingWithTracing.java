package com.cloudentity.tools.vertx.server.api.tracing;

import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.tag.Tags;
import com.cloudentity.tools.vertx.server.api.RouteHandler;
import com.cloudentity.tools.vertx.tracing.TracingContext;
import com.cloudentity.tools.vertx.tracing.TracingManager;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

import java.util.Iterator;
import java.util.Map;

public class RoutingWithTracing {
  public static final String ROUTING_CONTEXT_SPAN = "routing.context.span";

  public static TracingContext getOrCreate(RoutingContext ctx, TracingManager tm) {
    Object object = ctx.get(ROUTING_CONTEXT_SPAN);
    if (object instanceof TracingContext) {
      TracingContext context = (TracingContext) object;
      context.setOperationName(getOperationName(ctx));
      return context;
    }

    SpanContext extractedContext = tm.tracer.extract(
      Format.Builtin.HTTP_HEADERS,
      new RoutingWithTracing.MultiMapExtractAdapter(ctx.request().headers())
    );

    TracingContext context = TracingContext.ofParent(tm, extractedContext, getOperationName(ctx));
    context.setTag(Tags.HTTP_METHOD.getKey(), ctx.request().method().toString());
    context.setTag(Tags.HTTP_URL.getKey(), ctx.request().absoluteURI());

    ctx.put(ROUTING_CONTEXT_SPAN, context);
    ctx.addBodyEndHandler(finishEndHandler(ctx, context));

    return context;
  }

  public static String getOperationName(RoutingContext ctx) {
    return ctx.request().method() + " " + ctx.get(RouteHandler.urlPathKey());
  }

  private static Handler<Void> finishEndHandler(RoutingContext routingContext, TracingContext tracingContext) {
    return handler -> {
      tracingContext.setTag(Tags.HTTP_STATUS.getKey(), String.valueOf(routingContext.response().getStatusCode()));
      tracingContext.finish();
    };
  }

  public static class MultiMapExtractAdapter implements TextMap {
    private MultiMap headers;

    public MultiMapExtractAdapter(MultiMap headers) {
      this.headers = headers;
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
      return headers.entries().iterator();
    }

    @Override
    public void put(String key, String value) {
      throw new UnsupportedOperationException();
    }
  }
}
