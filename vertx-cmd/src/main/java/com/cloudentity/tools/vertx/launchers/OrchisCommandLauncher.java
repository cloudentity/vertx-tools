package com.cloudentity.tools.vertx.launchers;

import com.cloudentity.tools.vertx.commands.OrchisRunCommandFactory;
import com.cloudentity.tools.vertx.commands.PrintConfigCommandFactory;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Launcher;
import io.vertx.core.Vertx;
import io.vertx.core.impl.launcher.commands.ExecUtils;

public class OrchisCommandLauncher extends Launcher {

  public static void main(String[] args) {
    new OrchisCommandLauncher()
      .unregister("run")
      .register(new OrchisRunCommandFactory())
      .unregister("print-config")
      .register(new PrintConfigCommandFactory())
      .dispatch(args);
  }

  @Override
  public void handleDeployFailed(Vertx vertx, String mainVerticle, DeploymentOptions deploymentOptions, Throwable cause) {
    vertx.close(x -> ExecUtils.exitBecauseOfVertxDeploymentIssue());
  }
}
