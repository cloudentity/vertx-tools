package com.cloudentity.tools.vertx.commands;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Summary;

@Name("print-config")
@Summary("Prints vertx-app configuration")
public class PrintConfigCommand extends OrchisRunCommand {
  @Override
  protected void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {
    super.beforeDeployingVerticle(deploymentOptions);
    deploymentOptions.getConfig().put("mode", "dry");
  }
}