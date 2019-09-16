package com.cloudentity.tools.vertx.conf.retriever;

import io.vertx.core.json.JsonObject;

import java.util.Optional;

public class ConfigStoreConf {
  private String type;
  private String format;
  private JsonObject config;
  private Boolean enabled;

  public ConfigStoreConf() {

  }

  public ConfigStoreConf(String type, String format, JsonObject config, Boolean enabled) {
    this.type = type;
    this.format = format;
    this.config = config;
    this.enabled = enabled;
  }

  public String getType() {
    return type;
  }

  public JsonObject getConfig() {
    return config;
  }

  public String getFormat() {
    return format;
  }

  public Boolean getEnabled() {
    return enabled != null ? enabled : true;
  }
}
