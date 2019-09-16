package com.cloudentity.tools.vertx.conf;

import com.cloudentity.tools.vertx.verticles.VertxDeploy;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ConfVerticleDeploy {
  /**
   * Deploys {@link ConfVerticle} with {@link ConfigRetriever} with configuration from JSON object.
   * The configuration schema matches {@link com.cloudentity.tools.vertx.conf.retriever.ConfigRetrieverConf}.
   *
   * ConfigRetrieverConf is converted to {@link io.vertx.config.ConfigRetrieverOptions} and {@link io.vertx.config.ConfigStoreOptions}
   * that are used to initialize {@link io.vertx.config.ConfigRetriever}
   *
   * E.g. configuration:
   * {
   *   "scanPeriod": 3000,
   *   "stores": [
   *     {
   *       "type": "file",
   *       "format": "json",
   *       "config": {
   *         "path": "src/test/resources/config-test.json"
   *       }
   *     }
   *   ]
   * }
   */
  public static Future<String> deployVerticleFromConf(Vertx vertx, JsonObject conf) {
    try {
      JsonObject resolvedConf = ConfReference.populateRefs(conf, conf);
      ConfigRetriever retriever = ConfigRetrieverFactory.buildFromJson(vertx, resolvedConf);
      return VertxDeploy.deploy(vertx, new ConfVerticle(retriever));
    } catch (Throwable ex) {
      return Future.failedFuture(ex);
    }
  }

  public static Future<String> deployFileConfVerticle(Vertx vertx, String configFilePath) {
    return deployFileConfVerticle(vertx, configFilePath, new ConfigRetrieverOptions());
  }

  public static Future<String> deployFileConfVerticle(Vertx vertx, String configFilePath, ConfigRetrieverOptions retOpts) {
    return VertxDeploy.deploy(vertx, new ConfVerticle(ConfigRetrieverFactory.buildFileRetriever(vertx, configFilePath, retOpts)));
  }

  public static Future<String> deployDirectoryConfVerticle(Vertx vertx, String configDirectoryPath) {
    return deployDirectoryConfVerticle(vertx, configDirectoryPath, new ConfigRetrieverOptions());
  }

  public static Future<String> deployDirectoryConfVerticle(Vertx vertx, String configDirectoryPath, ConfigRetrieverOptions retOpts) {
    return VertxDeploy.deploy(vertx, new ConfVerticle(ConfigRetrieverFactory.buildDirectoryRetriever(vertx, configDirectoryPath, retOpts)));
  }
}
