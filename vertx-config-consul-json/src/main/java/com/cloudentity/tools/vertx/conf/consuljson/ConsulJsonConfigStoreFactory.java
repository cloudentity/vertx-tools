package com.cloudentity.tools.vertx.conf.consuljson;


import io.vertx.config.spi.ConfigStore;
import io.vertx.config.spi.ConfigStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ConsulJsonConfigStoreFactory implements ConfigStoreFactory {
  @Override
  public String name() {
    return "consul-json";
  }

  @Override
  public ConfigStore create(Vertx vertx, JsonObject configuration) {
    return new ConsulJsonConfigStore(vertx, configuration);
  }
}