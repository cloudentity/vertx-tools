package com.cloudentity.tools.vertx.conf;

import com.cloudentity.tools.vertx.bus.VertxEndpoint;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface ConfService {
  @VertxEndpoint
  Future<JsonObject> getConf(String path);

  @VertxEndpoint
  Future<JsonObject> getGlobalConf();
}
