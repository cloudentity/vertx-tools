package com.cloudentity.tools.vertx.test;

import com.cloudentity.tools.vertx.bus.VertxEndpoint;
import io.vertx.core.Future;

public interface SampleService {
  @VertxEndpoint(address = "cut")
  Future<String> cut(String param);
}
