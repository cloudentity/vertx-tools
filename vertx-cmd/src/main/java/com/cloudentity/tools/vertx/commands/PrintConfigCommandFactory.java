package com.cloudentity.tools.vertx.commands;

import com.cloudentity.tools.vertx.configs.OrchisConfigExtension;
import com.cloudentity.tools.vertx.configs.VertxOptionsExtensionImpl;
import io.vertx.core.cli.CommandLine;
import io.vertx.core.spi.launcher.DefaultCommandFactory;

public class PrintConfigCommandFactory extends DefaultCommandFactory<PrintConfigCommand> {

  public PrintConfigCommandFactory() {
    super(PrintConfigCommand.class);
  }

  @Override
  public PrintConfigCommand create(CommandLine cl) {
    PrintConfigCommand orchisRunCommand = super.create(cl);
    orchisRunCommand.setConfigExtension(new OrchisConfigExtension());
    orchisRunCommand.setVertxExtension(new VertxOptionsExtensionImpl());
    return orchisRunCommand;
  }
}