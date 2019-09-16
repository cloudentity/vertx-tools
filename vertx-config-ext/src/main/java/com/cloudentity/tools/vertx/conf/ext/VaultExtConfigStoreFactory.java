package com.cloudentity.tools.vertx.conf.ext;

import io.vertx.config.spi.ConfigStore;
import io.vertx.config.spi.ConfigStoreFactory;
import io.vertx.config.vault.VaultConfigStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class VaultExtConfigStoreFactory implements ConfigStoreFactory {
  public String name() {
    return "vault-ext";
  }

  public ConfigStore create(Vertx vertx, JsonObject configuration) {
    return ExtConfigStoreFactory.create(vertx, new VaultConfigStoreFactory(), configuration);
  }
}