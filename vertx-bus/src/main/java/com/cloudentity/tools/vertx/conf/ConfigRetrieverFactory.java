package com.cloudentity.tools.vertx.conf;

import com.google.common.collect.Lists;
import java.util.List;

import com.cloudentity.tools.vertx.conf.retriever.ConfigRetrieverConf;
import com.cloudentity.tools.vertx.configs.ConfigFactory;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.stream.Collectors;

public class ConfigRetrieverFactory {

  public static ConfigRetriever buildFromJson(Vertx vertx, JsonObject obj) {
    ConfigRetrieverConf conf = ConfigFactory.build(obj, ConfigRetrieverConf.class);

    List<ConfigStoreOptions> stores =
      conf.getStores().stream()
      .filter(store -> store.getEnabled())
      .map(store ->
        new ConfigStoreOptions()
          .setType(store.getType())
          .setFormat(store.getFormat())
          .setConfig(store.getConfig())
      ).collect(Collectors.toList());

    ConfigRetrieverOptions retOpts =
      new ConfigRetrieverOptions()
        .setScanPeriod(conf.getScanPeriod())
        .setStores(stores);

    return ConfigRetriever.create(vertx, retOpts);
  }

  public static ConfigRetriever buildDirectoryRetriever(Vertx vertx, String descriptorsDirectoryPath) {
    return buildDirectoryRetriever(vertx, descriptorsDirectoryPath, new ConfigRetrieverOptions());
  }

  public static ConfigRetriever buildDirectoryRetriever(Vertx vertx, String descriptorsDirectoryPath, ConfigRetrieverOptions retOpts) {
    ConfigStoreOptions dir = new ConfigStoreOptions()
      .setType("directory")
      .setConfig(new JsonObject()
        .put("path", descriptorsDirectoryPath)
        .put("filesets",
          new JsonArray().add(new JsonObject().put("pattern", "*.json"))
        )
      );

    retOpts.setStores(Lists.newArrayList(dir));
    return ConfigRetriever.create(vertx, retOpts);
  }

  public static ConfigRetriever buildFileRetriever(Vertx vertx, String descriptorsFilePath) {
    return buildFileRetriever(vertx, descriptorsFilePath, new ConfigRetrieverOptions());
  }

  public static ConfigRetriever buildFileRetriever(Vertx vertx, String descriptorsFilePath, ConfigRetrieverOptions retOpts) {
    ConfigStoreOptions file = new ConfigStoreOptions()
      .setType("file")
      .setConfig(new JsonObject().put("path", descriptorsFilePath));

    retOpts.setStores(Lists.newArrayList(file));
    return ConfigRetriever.create(vertx, retOpts);
  }
}
