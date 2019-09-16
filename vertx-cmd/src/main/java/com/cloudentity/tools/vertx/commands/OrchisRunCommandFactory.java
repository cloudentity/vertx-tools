package com.cloudentity.tools.vertx.commands;

import com.cloudentity.tools.vertx.configs.OrchisConfigExtension;
import com.cloudentity.tools.vertx.configs.VertxOptionsExtensionImpl;
import io.vertx.core.cli.CommandLine;
import io.vertx.core.spi.launcher.DefaultCommandFactory;

public class OrchisRunCommandFactory extends DefaultCommandFactory<OrchisRunCommand> {

  public OrchisRunCommandFactory() {
    super(OrchisRunCommand.class);
  }

  @Override
  public OrchisRunCommand create(CommandLine cl) {
    OrchisRunCommand orchisRunCommand = super.create(cl);
    orchisRunCommand.setConfigExtension(new OrchisConfigExtension());
    orchisRunCommand.setVertxExtension(new VertxOptionsExtensionImpl());
    return orchisRunCommand;
  }
}