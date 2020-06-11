package com.cloudentity.tools.vertx.conf.shared;

import io.vertx.config.spi.ConfigStore;
import io.vertx.config.spi.ConfigStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class SharedLocalMapConfigStoreFactory implements ConfigStoreFactory {
  @Override
  public String name() {
    return "shared-local-map";
  }

  @Override
  public ConfigStore create(Vertx vertx, JsonObject config) {
    return new SharedLocalMapConfigStore(config);
  }
}
