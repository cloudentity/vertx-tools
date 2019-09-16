package com.cloudentity.tools.vertx.vault;

import io.vertx.config.spi.ConfigStore;
import io.vertx.config.spi.ConfigStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class VaultKeyCertsConfigStoreFactory implements ConfigStoreFactory {
  
  @Override
  public String name() {
    return "vault-keycerts";
  }
  
  @Override
  public ConfigStore create(Vertx vertx, JsonObject configuration) {
    return new VaultKeyCertsConfigStore(vertx, configuration);
  }
}