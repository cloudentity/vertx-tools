package com.cloudentity.tools.vertx.test;

import com.cloudentity.tools.vertx.bus.ServiceVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OtherSampleServiceVerticle extends ServiceVerticle implements OtherSampleService {
  @Override
  public Future<String> ping(String param) {
    return Future.succeededFuture(param);
  }
}
