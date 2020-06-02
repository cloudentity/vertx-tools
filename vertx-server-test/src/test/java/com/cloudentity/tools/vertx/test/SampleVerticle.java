package com.cloudentity.tools.vertx.test;

import com.cloudentity.tools.vertx.bus.ServiceVerticle;
import io.vertx.core.Future;

public class SampleVerticle extends ServiceVerticle implements SampleService {
  @Override
  public Future<String> getId() {
    return Future.succeededFuture(verticleId());
  }
}
