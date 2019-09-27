package com.cloudentity.tools.vertx.conf;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

public class ConfNullifier {
  /**
   * Recursively removes JsonObjects that have '_nullify' attribute flag set to 'true' and all other attributes set to null.
   *
   * Required when configuring one-of alternatives with env variables (e.g. HttpServerOptions.trustOptions)
   */
  public static void nullify(JsonObject obj) {
    Iterator<Map.Entry<String, Object>> it = obj.iterator();
    while (it.hasNext()) {
      nullifyEntry(it, it.next(), x -> x.getValue());
    }
  }

  private static void nullify(java.util.Map<String, Object> obj) {
    Iterator<java.util.Map.Entry<String, Object>> it = obj.entrySet().iterator();
    while (it.hasNext()) {
      nullifyEntry(it, it.next(), x -> x.getValue());
    }
  }

  private static void nullify(java.util.List<Object> arr) {
    Iterator<Object> it = arr.iterator();
    while (it.hasNext()) {
      nullifyEntry(it, it.next(), x -> x);
    }
  }

  private static <T> void nullifyEntry(Iterator<T> it, T entry, Function<T, Object> valueF) {
    Object value = valueF.apply(entry);
    if (value != null) {
      if (value instanceof JsonObject) {
        nullify((JsonObject) value);
        if (shouldNullify((JsonObject) value)) {
          it.remove();
        } else {
          removeNullifyFlag((JsonObject) value);
        }
      } else if (value instanceof java.util.Map) {
        nullify((java.util.Map) value);
        if (shouldNullify((java.util.Map) value)) {
          it.remove();
        } else {
          removeNullifyFlag((java.util.Map) value);
        }
      } else if (value instanceof JsonArray) {
        nullify(((JsonArray) value).getList());
      } else if (value instanceof java.util.List) {
        nullify((java.util.List) value);
      }
    }
  }

  private static void removeNullifyFlag(java.util.Map<String, Object> obj) {
    obj.remove("_nullify");
  }

  private static void removeNullifyFlag(JsonObject obj) {
    obj.remove("_nullify");
  }

  private static boolean shouldNullify(JsonObject obj) {
    return obj.getBoolean("_nullify") == Boolean.TRUE && obj.stream().allMatch(entry -> entry.getKey() == "_nullify" || entry.getValue() == null);
  }

  private static boolean shouldNullify(java.util.Map<String, Object> obj) {
    return obj.get("_nullify") == Boolean.TRUE && obj.entrySet().stream().allMatch(entry -> entry.getKey() == "_nullify" || entry.getValue() == null);
  }
}
