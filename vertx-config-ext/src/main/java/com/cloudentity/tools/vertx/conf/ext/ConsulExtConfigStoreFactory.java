package com.cloudentity.tools.vertx.conf.ext;

import io.vertx.config.consul.ConsulConfigStoreFactory;
import io.vertx.config.spi.ConfigStore;
import io.vertx.config.spi.ConfigStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ConsulExtConfigStoreFactory  implements ConfigStoreFactory {

  @Override
  public String name() { return "consul-ext"; }

  @Override
  public ConfigStore create(Vertx vertx, JsonObject configuration) {
    return ExtConfigStoreFactory.create(vertx, new ConsulConfigStoreFactory(), configuration);
  }
}
