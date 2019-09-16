package com.cloudentity.tools.vertx.configs;

import io.vertx.core.DeploymentOptions;

import java.util.List;

public interface ConfigExtension {

  public DeploymentOptions extendConfig(List<String> configExtensions, DeploymentOptions other);
}
