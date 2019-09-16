package com.cloudentity.tools.vertx.tracing;

import io.opentracing.Tracer;
import com.cloudentity.tools.vertx.conf.ConfService;
import com.cloudentity.tools.vertx.tracing.internals.JaegerTracing;
import io.vertx.core.Future;

/**
 * TracingManager is our wrapper for open tracing Tracer.
 */
public class TracingManager {
  public final Tracer tracer;
  public final String spanContextKey;
  public final String baggagePrefix;

  private TracingManager(Tracer tracer, String spanContextKey, String baggagePrefix) {
    this.tracer = tracer;
    this.spanContextKey = spanContextKey;
    this.baggagePrefix = baggagePrefix;
  }

  /**
   * Build TracingManager instance using Tracer instance.
   */
  public static TracingManager of(Tracer tracer, String spanContextKey, String baggagePrefix) {
    return new TracingManager(tracer, spanContextKey, baggagePrefix);
  }
}
