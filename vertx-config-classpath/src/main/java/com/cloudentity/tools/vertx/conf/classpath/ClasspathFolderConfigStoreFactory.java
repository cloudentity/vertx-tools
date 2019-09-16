package com.cloudentity.tools.vertx.conf.classpath;


import io.vertx.config.spi.ConfigStore;
import io.vertx.config.spi.ConfigStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ClasspathFolderConfigStoreFactory implements ConfigStoreFactory {
  @Override
  public String name() {
    return "classpath-folder";
  }

  @Override
  public ConfigStore create(Vertx vertx, JsonObject configuration) {
    return new ClasspathFolderConfigStore(vertx, configuration);
  }
}