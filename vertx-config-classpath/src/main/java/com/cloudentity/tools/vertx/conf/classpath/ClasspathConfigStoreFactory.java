package com.cloudentity.tools.vertx.conf.classpath;


import io.vertx.config.spi.ConfigStore;
import io.vertx.config.spi.ConfigStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ClasspathConfigStoreFactory implements ConfigStoreFactory {
  @Override
  public String name() {
    return "classpath";
  }

  @Override
  public ConfigStore create(Vertx vertx, JsonObject configuration) {
    return new ClasspathConfigStore(vertx, configuration);
  }
}