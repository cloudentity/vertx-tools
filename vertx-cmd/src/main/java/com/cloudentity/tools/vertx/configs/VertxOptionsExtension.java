package com.cloudentity.tools.vertx.configs;

import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;

public interface VertxOptionsExtension {
  VertxOptions extendVertxOptions(JsonObject conf, VertxOptions opts);
}
