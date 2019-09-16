package com.cloudentity.tools.vertx.shutdown;

import com.cloudentity.tools.vertx.bus.VertxEndpoint;
import io.vertx.core.Future;

import java.util.function.Supplier;

public interface ShutdownService {
  @VertxEndpoint
  Future<Void> registerShutdownAction(Supplier<Future> action);
}
