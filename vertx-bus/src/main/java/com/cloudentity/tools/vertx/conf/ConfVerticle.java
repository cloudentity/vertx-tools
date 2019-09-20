package com.cloudentity.tools.vertx.conf;

import com.syntegrity.tools.init.logging.InitLog;
import com.cloudentity.tools.vertx.bus.ServiceVerticle;
import com.cloudentity.tools.vertx.bus.VertxBus;
import com.cloudentity.tools.vertx.json.JsonExtractor;
import io.vavr.control.Either;
import io.vertx.config.ConfigChange;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This ServiceVerticle implements a ConfService method that returns config by key from {@link io.vertx.config.ConfigRetriever}.
 * It also listens on changes in ConfigRetriever and publishes it on event bus.
 *
 * Use ConfVerticleDeploy to deploy it.
 */
public class ConfVerticle extends ServiceVerticle implements ConfService {

  private static final Logger log = LoggerFactory.getLogger(ConfVerticle.class);

  public static final String CONFIG_CHANGE_ADDRESS = "com.cloudentity.configuration.change";

  private ConfigRetriever retriever;

  public ConfVerticle(ConfigRetriever retriever) {
    this.retriever = retriever;
  }

  JsonObject globalConf;
  JsonObject rawGlobalConf;

  @Override
  public void start(Future<Void> start) {
    toFuture(super::start).compose(x -> {
      retriever.listen(this::configurationChangeListener);

      return Future.succeededFuture();
    }).compose(x -> {
      Future<JsonObject> promise = Future.future();
      retriever.getConfig(promise);

      return promise.compose(this::setInitialConfig);
    }).setHandler(start);
  }

  private Future<Void> setInitialConfig(JsonObject config) {
    log.debug("Unresolved root configuration: {}", config);
    rawGlobalConf = config;

    Either<List<ConfBuilder.MissingModule>, ConfBuilder.Config> configResult = ConfBuilder.buildFinalConfig(config);
    if (configResult.isRight()) {
      ConfBuilder.Config cfg = configResult.get();
      globalConf = cfg.resolvedConfig;

      InitLog.info("Environment variables in root configuration: ");
      ConfPrinter.logEnvVariables(config);

      cfg.validModules.forEach(module -> {
        InitLog.info("Injected '" + module.name + "' classpath module configuration. Environment variables: ");
        ConfPrinter.logEnvVariables(moduleConfigWithEnvFallback(module.rawConfig, config));
        InitLog.debug("Unresolved '" + module.name + "' classpath module configuration: " + module.rawConfig.toString());
        InitLog.info("");
      });

      InitLog.info("Configuration: {}", cfg.resolvedConfigWithMask);
      log.debug("Configuration:\n{}", globalConf.encodePrettily());

      return Future.<Void>succeededFuture();
    } else {
      List<String> missingModules = configResult.getLeft().stream().map(module -> module.name).collect(Collectors.toList());
      InitLog.error("Could not read classpath modules configuration: [{}]", String.join(", ", missingModules));
      return Future.<Void>failedFuture("");
    }
  }

  private JsonObject moduleConfigWithEnvFallback(JsonObject moduleConfig, JsonObject rootConfig) {
    return moduleConfig.copy().put("env", rootConfig.getValue("env", new JsonObject()));
  }

  private void configurationChangeListener(ConfigChange change) {
    log.debug("Configuration changed: {}", change.toJson());
    rawGlobalConf = change.getNewConfiguration();

    JsonObject oldConf = globalConf;
    Either<List<ConfBuilder.MissingModule>, ConfBuilder.Config> newConfResult = ConfBuilder.buildFinalConfig(change.getNewConfiguration());

    if (newConfResult.isRight()) {
      globalConf = newConfResult.get().resolvedConfig;

      log.debug("New configuration with ref resolution: {}. Publishing on '{}' address", globalConf, CONFIG_CHANGE_ADDRESS);
      VertxBus.publish(vertx.eventBus(), CONFIG_CHANGE_ADDRESS, new ConfigChange(oldConf, globalConf));
    } else {
      List<String> missingModules = newConfResult.getLeft().stream().map(module -> module.name).collect(Collectors.toList());
      log.error("Could not read classpath modules configuration: [{}]", String.join(", ", missingModules));
    }
  }

  /**
   * Reads JsonObject by period-separated path.
   *
   * E.g.
   *   global configuration: {
   *     "key1": {
   *       "key2: {
   *         "param": "value"
   *       }
   *     }
   *   }
   *   given 'key1.key2' path the returned value is { "param": "value" }
   */
  @Override
  public Future<JsonObject> getConf(final String path) {
    return Future.succeededFuture(JsonExtractor.resolve(globalConf, path).orElse(null));
  }

  @Override
  public Future<JsonObject> getGlobalConf() {
    return Future.succeededFuture(globalConf);
  }

  @Override
  protected boolean tracingEnabled() {
    return false;
  }
}
