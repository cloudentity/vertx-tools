package com.cloudentity.tools.vertx.configs;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class OrchisConfigExtension implements ConfigExtension {

  private JsonObjectExtender extender = new JsonObjectExtender();

  @Override
  public DeploymentOptions extendConfig(List<String> configExtensions, DeploymentOptions other) {
    JsonObject currentConfig = other.getConfig();
    other.setConfig(extender.extendConfig(currentConfig, configExtensions));
    return other;
  }
}