package com.cloudentity.tools.vertx.conf.ext;

import io.vertx.config.spi.ConfigStore;
import io.vertx.config.spi.ConfigStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

public class ExtConfigStoreFactory {

  public static ConfigStore create(Vertx vertx, ConfigStoreFactory factory, JsonObject configuration) {
    JsonObject extConf = configuration.getJsonObject("ext");
    setSslFromScheme(extConf.getString("scheme"), configuration);

    Optional<String> outputPathOpt = Optional.ofNullable(extConf.getString("outputPath"));
    Optional<String> sourceFormatOpt = Optional.ofNullable(extConf.getString("sourceFormat"));
    Optional<String> sourcePathOpt = Optional.ofNullable(extConf.getString("sourcePath"));
    boolean encodeBase64 = Optional.ofNullable(extConf.getBoolean("base64Encode")).orElse(false);
    boolean maskSecrets = Optional.ofNullable(extConf.getBoolean("maskSecrets")).orElse(false);
    boolean cache = Optional.ofNullable(extConf.getBoolean("cache")).orElse(false);

    ConfigStore configStore = factory.create(vertx, configuration);
    return new ExtConfigStore(configStore, outputPathOpt, sourceFormatOpt, sourcePathOpt, encodeBase64, maskSecrets, cache);
  }

  private static void setSslFromScheme(String scheme, JsonObject configuration) {
    if (scheme != null) {
      if (configuration.getValue("ssl") != null) {
        throw new IllegalArgumentException("both 'ext.scheme' and 'ssl' cannot be set");
      } else {
        if ("https".equals(scheme)) {
          configuration.put("ssl", true);
        } else if ("http".equals(scheme)) {
          configuration.put("ssl", false);
        } else {
          throw new IllegalArgumentException("'ext.scheme' should be 'https' or 'http', was " + scheme);
        }
      }
    }
  }
}