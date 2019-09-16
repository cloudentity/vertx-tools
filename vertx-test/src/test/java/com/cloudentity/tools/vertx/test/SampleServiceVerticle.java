package com.cloudentity.tools.vertx.test;

import com.cloudentity.tools.vertx.bus.ServiceVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleServiceVerticle extends ServiceVerticle implements SampleService {

  private static final Logger log = LoggerFactory.getLogger(SampleServiceVerticle.class);

  @Override
  public Future<String> cut(String param) {
    log.debug("config: {}", getConfig());
    int length = getConfig().getInteger("length");
    log.debug("Substring: {}, length: {}", param, length);
    return Future.succeededFuture(param.substring(0, length));
  }
}
