package com.cloudentity.tools.vertx.json;

import io.vertx.core.json.JsonObject;

public class SamplePojo {
  private String name;
  private JsonObject config;

  public SamplePojo() {
  }

  public SamplePojo(String name, JsonObject config) {
    this.name = name;
    this.config = config;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public JsonObject getConfig() {
    return config;
  }

  public void setConfig(JsonObject config) {
    this.config = config;
  }

  @Override
  public String toString() {
    return "SamplePojo{" +
        "name='" + name + '\'' +
        ", config=" + config +
        '}';
  }
}
