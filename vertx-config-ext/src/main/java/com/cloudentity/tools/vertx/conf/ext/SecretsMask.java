package com.cloudentity.tools.vertx.conf.ext;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class SecretsMask {
  /**
   * Returns a copy of the input with recursively replaced all primitive values (strings, numbers, booleans) with "***".
   */
  public static JsonObject create(JsonObject o) {
    JsonObject masked = o.copy();
    mask(masked);
    return masked;
  }

  public static void mask(JsonObject o) {
    o.fieldNames().forEach(name -> {
      Object value = o.getValue(name);
      if (value instanceof JsonObject) {
        mask((JsonObject) value);
      } else if (value instanceof JsonArray) {
        mask((JsonArray) value);
      } else {
        o.put(name, "***");
      }
    });
  }

  public static void mask(JsonArray a) {
    List<Object> values = new ArrayList<>();
    a.getList().forEach(value -> {
      if (value instanceof JsonObject) {
        mask((JsonObject) value);
      } else if (value instanceof JsonArray) {
        mask((JsonArray) value);
      } else {
        value = "***";
      }
      values.add(value);
    });
    a.clear();
    a.addAll(new JsonArray(values));
  }
}
