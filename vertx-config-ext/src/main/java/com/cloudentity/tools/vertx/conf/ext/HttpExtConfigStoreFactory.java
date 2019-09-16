package com.cloudentity.tools.vertx.conf.ext;

import io.vertx.config.impl.spi.HttpConfigStoreFactory;
import io.vertx.config.spi.ConfigStore;
import io.vertx.config.spi.ConfigStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class HttpExtConfigStoreFactory implements ConfigStoreFactory {
  public HttpExtConfigStoreFactory() {
  }

  public String name() {
    return "http-ext";
  }

  public ConfigStore create(Vertx vertx, JsonObject configuration) {
    return ExtConfigStoreFactory.create(vertx, new HttpConfigStoreFactory(), configuration);
  }
}