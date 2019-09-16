package com.cloudentity.tools.vertx.test;

import com.cloudentity.tools.vertx.bus.VertxEndpoint;
import io.vertx.core.Future;

public interface OtherSampleService {
  @VertxEndpoint(address = "ping")
  Future<String> ping(String param);
}
