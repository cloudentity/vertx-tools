package com.cloudentity.tools.vertx.bus;

import com.cloudentity.tools.vertx.tracing.TracingContext;
import com.cloudentity.tools.vertx.tracing.TracingManager;

public class ServiceTracing {
  public static ObjectsWithContext createNewSpanForTracingContext(TracingManager tracing,
                                                                   VertxEndpointClient.VertxEndpointInterface endpoint,
                                                                   Object[] objects) {
    return new ObjectsWithContext(tracing, endpoint, objects);
  }

  public static class ObjectsWithContext {
    private Object[] objects;
    private TracingContext ctx;

    public ObjectsWithContext(TracingManager tracing, VertxEndpointClient.VertxEndpointInterface endpoint, Object[] obj) {
      objects = obj;
      for (int i = 0; i < objects.length; i++) {
        if (objects[i] instanceof TracingContext) {
          TracingContext parentCtx = (TracingContext) objects[i];
          ctx = parentCtx.newChild(tracing, getShortEndpointName(endpoint));
          ctx.setTag("class", endpoint.method.toString());
          ctx.setTag("address", endpoint.address);
          objects[i] = ctx;
        }
      }
    }

    public Object[] getObjectsWithContext() {
      return objects;
    }

    public void finishSpan() {
      if (ctx != null) {
        ctx.finish();
      }
    }

    public void logErrorAndFinish(Throwable t) {
      if (ctx != null) {
        ctx.logException(t);
        ctx.finish();
      }
    }
  }

  public static String getShortEndpointName(VertxEndpointClient.VertxEndpointInterface endpoint) {
    return endpoint.method.getDeclaringClass().getSimpleName() + "." + endpoint.method.getName();
  }
}
