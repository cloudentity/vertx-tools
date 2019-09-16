package com.cloudentity.tools.vertx.commands;

import com.cloudentity.tools.vertx.clusters.ExternalCluster;
import com.cloudentity.tools.vertx.configs.ConfigExtension;
import com.cloudentity.tools.vertx.configs.VertxOptionsExtension;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.cli.annotations.Name;
import io.vertx.core.cli.annotations.Option;
import io.vertx.core.cli.annotations.Summary;
import io.vertx.core.impl.launcher.commands.RunCommand;

import java.util.Base64;
import java.util.List;

@Name("run")
@Summary("Runs a verticle called <main-verticle> in its own instance of vert.x.")
public class OrchisRunCommand extends RunCommand {

  protected boolean externalClusterEnabled;
  protected List<String> configExtensions;

  private ExternalCluster externalCluster;
  private ConfigExtension configExtension;
  private VertxOptionsExtension vertxExtension;

  @Option(longName = "extconf", argName = "configextension", acceptMultipleValues = true)
  @Description("Specifies configuration overwrite rules. " +
    "Accepts a json path as a key which value will get overwritten with a given value")
  public void setExtendConfig(List<String> extensions) {
    this.configExtensions = extensions;
  }

  @Option(longName = "externalcluster", acceptValue = false, flag = true )
  @Description("If specified then the vert.x instance will connect to external cluster")
  public void enableExternalCluster(boolean externalCluster) {
    this.externalClusterEnabled = externalCluster;
  }

  @Option(longName = "base64Conf")
  @Description("Base64-encoded value of 'conf' argument. If specified then it is decoded and set as 'conf' argument value")
  public void setBase64Conf(String value) {
    String conf = new String(Base64.getDecoder().decode(value));
    setConfig(conf);
  }

  public void setExternalCluster(ExternalCluster externalCluster) {
    this.externalCluster = externalCluster;
  }

  public void setConfigExtension(ConfigExtension configExtension) {
    this.configExtension = configExtension;
  }
  public void setVertxExtension(VertxOptionsExtension vertxExtension) {
    this.vertxExtension = vertxExtension;
  }

  @Override
  protected void beforeStartingVertx(VertxOptions options) {
    vertxExtension.extendVertxOptions(getConfiguration(), options);
    super.beforeStartingVertx(externalCluster.appendClusterOptions(externalClusterEnabled, options));
  }

  @Override
  protected void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {
    DeploymentOptions opts = configExtension.extendConfig(configExtensions, deploymentOptions);
    super.beforeDeployingVerticle(opts);
  }


}