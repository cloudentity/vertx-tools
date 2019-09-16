package com.cloudentity.tools.vertx.launchers;

import com.cloudentity.tools.vertx.commands.OrchisRunCommandFactory;
import com.cloudentity.tools.vertx.commands.PrintConfigCommandFactory;
import io.vertx.core.Launcher;

public class OrchisCommandLauncher extends Launcher {

  public static void main(String[] args) {
    new OrchisCommandLauncher()
      .unregister("run")
      .register(new OrchisRunCommandFactory())
      .unregister("print-config")
      .register(new PrintConfigCommandFactory())
      .dispatch(args);
  }
}
