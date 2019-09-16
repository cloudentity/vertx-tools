package com.cloudentity.tools.vertx.tracing;

import com.cloudentity.tools.vertx.bus.VertxEndpoint;
import io.vertx.core.Future;

public interface TracingService {
  @VertxEndpoint
  Future<TracingManager> getTracingManager();
}
