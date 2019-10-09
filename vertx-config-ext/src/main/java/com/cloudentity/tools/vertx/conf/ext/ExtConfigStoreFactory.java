package com.cloudentity.tools.vertx.conf.ext;

import io.vertx.config.spi.ConfigStore;
import io.vertx.config.spi.ConfigStoreFactory;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

public class ExtConfigStoreFactory {

  public static ConfigStore create(Vertx vertx, ConfigStoreFactory factory, JsonObject configuration) {
    ConfigStore configStore = factory.create(vertx, configuration);
    JsonObject extConf = configuration.getJsonObject("ext");
    Optional<String> outputPathOpt = Optional.ofNullable(extConf.getString("outputPath"));
    Optional<String> sourceFormatOpt = Optional.ofNullable(extConf.getString("sourceFormat"));
    Optional<String> sourcePathOpt = Optional.ofNullable(extConf.getString("sourcePath"));
    boolean encodeBase64 = Optional.ofNullable(extConf.getBoolean("base64Encode")).orElse(false);
    boolean maskSecrets = Optional.ofNullable(extConf.getBoolean("maskSecrets")).orElse(false);
    boolean cache = Optional.ofNullable(extConf.getBoolean("cache")).orElse(false);
    return new ExtConfigStore(configStore, outputPathOpt, sourceFormatOpt, sourcePathOpt, encodeBase64, maskSecrets, cache);
  }
}