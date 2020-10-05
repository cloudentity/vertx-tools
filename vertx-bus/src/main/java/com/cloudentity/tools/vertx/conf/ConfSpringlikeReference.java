package com.cloudentity.tools.vertx.conf;

import com.cloudentity.tools.vertx.json.JsonExtractor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfSpringlikeReference {
  public static JsonObject populateRefs(JsonObject root) {
    return traverse(root, replaceSpringlikeRef(root));
  }

  private static Pattern springRefPlaceholder = Pattern.compile(".*(\\$\\{[^}]+?\\}).*");
  private static Function<String, String> replaceSpringlikeRef(JsonObject root) {
    return in -> {
      Matcher m = springRefPlaceholder.matcher(in);
      if (m.matches()) {
        String placeholder = m.group(1);
        String[] refParts = placeholder.substring(2, placeholder.length()-1).split(":");
        String refPath = refParts[0];
        String defaultValue;
        if (refParts.length == 1) {
          defaultValue = "";
        } else {
          defaultValue = refParts[1];
        }

        String refValue = JsonExtractor.resolveValue(root, refPath).map(x -> x.toString()).orElse(defaultValue);
        return replaceSpringlikeRef(root).apply(in.replace(placeholder, refValue));
      } else {
        return in;
      }
    };
  }

  private static JsonObject traverse(JsonObject conf, Function<String, String> f) {
    JsonObject out = new JsonObject();
    conf.getMap().forEach((key, value) -> out.put(f.apply(key), traverse(value, f)));
    return out;
  }

  private static JsonArray traverse(JsonArray conf, Function<String, String> f) {
    JsonArray out = new JsonArray();
    conf.getList().forEach(value -> out.add(traverse(value, f)));
    return out;
  }

  private static Object traverse(Object value, Function<String, String> f) {
    if (value instanceof String) {
      return f.apply((String) value);
    } else if (value instanceof JsonObject) {
      return traverse((JsonObject) value, f);
    } else if (value instanceof Map) {
      return traverse(new JsonObject((Map) value), f);
    } else if (value instanceof JsonArray) {
      return traverse((JsonArray) value, f);
    } else if (value instanceof List) {
      return traverse(new JsonArray((List) value), f);
    } else {
      return value;
    }
  }
}
