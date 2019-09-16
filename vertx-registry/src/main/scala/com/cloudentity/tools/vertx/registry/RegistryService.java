package com.cloudentity.tools.vertx.registry;

import com.cloudentity.tools.vertx.bus.VertxEndpoint;
import io.vertx.core.Future;

import java.util.List;

public interface RegistryService {
  @VertxEndpoint(address = ":registry.verticles")
  Future<List<String>> getVerticleIds();
}
