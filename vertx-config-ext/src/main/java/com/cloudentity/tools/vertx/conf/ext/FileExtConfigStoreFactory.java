package com.cloudentity.tools.vertx.conf.ext;

import io.vertx.config.impl.spi.FileConfigtoreFactory;
import io.vertx.config.spi.ConfigStore;
import io.vertx.config.spi.ConfigStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * Extension of {@link io.vertx.config.impl.spi.FileConfigStore}.
 * See {@link ExtConfigStore} for details.
 */
public class FileExtConfigStoreFactory implements ConfigStoreFactory {
  public String name() {
    return "file-ext";
  }

  public ConfigStore create(Vertx vertx, JsonObject configuration) {
    return ExtConfigStoreFactory.create(vertx, new FileConfigtoreFactory(), configuration);
  }
}