package com.cloudentity.tools.vertx.conf.consuljson;

import io.vertx.config.spi.ConfigStore;
import io.vertx.config.spi.ConfigStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ConsulFolderConfigStoreFactory implements ConfigStoreFactory {

  @Override
  public String name() {
    return "consul-folder";
  }

  @Override
  public ConfigStore create(Vertx vertx, JsonObject configuration) {
    return new ConsulFolderConfigStore(vertx, configuration);
  }
}