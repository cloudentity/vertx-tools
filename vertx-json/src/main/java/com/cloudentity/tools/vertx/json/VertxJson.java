package com.cloudentity.tools.vertx.json;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class VertxJson {
  public static void registerJsonObjectDeserializer() {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(JsonObject.class, new JsonObjectDeserializer());
    Json.mapper.registerModule(module);
  }
}