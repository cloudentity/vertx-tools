package com.cloudentity.tools.vertx.vault;

import com.cloudentity.tools.vertx.conf.ext.ExtConfigStoreFactory;
import io.vertx.config.spi.ConfigStore;
import io.vertx.config.spi.ConfigStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class VaultKeyCertsExtConfigStoreFactory implements ConfigStoreFactory {
  public String name() {
    return "vault-keycerts-ext";
  }

  public ConfigStore create(Vertx vertx, JsonObject configuration) {
    return ExtConfigStoreFactory.create(vertx, new VaultKeyCertsConfigStoreFactory(), configuration);
  }
}