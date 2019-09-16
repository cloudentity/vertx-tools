package com.cloudentity.tools.vertx.conf;

import io.vertx.core.json.JsonObject;

import java.util.Iterator;
import java.util.Map;

public class ConfNullifier {
  /**
   * Recursively removes JsonObjects that have '_nullify' attribute flag set to 'true' and all other attributes set to null.
   *
   * Required when configuring one-of alternatives with env variables (e.g. HttpServerOptions.trustOptions)
   */
  public static void nullify(JsonObject obj) {
    Iterator<Map.Entry<String, Object>> it = obj.iterator();
    while (it.hasNext()) {
      nullifyEntry(it, it.next());
    }
  }

  private static void nullify(java.util.Map<String, Object> obj) {
    Iterator<java.util.Map.Entry<String, Object>> it = obj.entrySet().iterator();
    while (it.hasNext()) {
      nullifyEntry(it, it.next());
    }
  }

  private static void nullifyEntry(Iterator<java.util.Map.Entry<String, Object>> it, java.util.Map.Entry<String, Object> entry) {
    if (entry.getValue() != null) {
      if (entry.getValue() instanceof JsonObject) {
        if (shouldNullify((JsonObject) entry.getValue())) {
          it.remove();
        } else {
          removeNullifyFlag((JsonObject) entry.getValue());
          nullify((JsonObject) entry.getValue());
        }
      } else if (entry.getValue() instanceof java.util.Map) {
        if (shouldNullify((java.util.Map) entry.getValue())) {
          it.remove();
        } else {
          removeNullifyFlag((java.util.Map) entry.getValue());
          nullify((java.util.Map) entry.getValue());
        }
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
