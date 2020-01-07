package com.cloudentity.tools.vertx.conf;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModuleIdReference {
  static Pattern moduleIdPattern = Pattern.compile("(.*)(\\{!!.?\\})(.*)");

  public static JsonObject populateModuleIdRefs(JsonObject conf, Optional<String> moduleIdOpt) {
    Function<String, String> replaceModuleId = value -> {
      Matcher matcher = moduleIdPattern.matcher(value);
      if (matcher.matches()) {
        String idPlaceholder = matcher.group(2);
        if (moduleIdOpt.isPresent()) {
          String separator = idPlaceholder.substring(3, 4);

          if ("}".equals(separator)) separator = "";
          return value.replace(idPlaceholder, moduleIdOpt.get() + separator);
        } else {
          return value.replace(idPlaceholder, "");
        }
      } else return value;
    };

    Function<Object, Object> mutateValues = value -> {
      if (value instanceof String) {
        return replaceModuleId.apply((String) value);
      } else return value;
    };

    traverseMutating(conf, mutateValues, replaceModuleId);
    return conf;
  }

  private static void traverseMutating(JsonObject obj, Function<Object, Object> mutateValues, Function<String, String> mutateKeys) {
    Object[] keys = obj.getMap().keySet().toArray();

    for (int i = 0; i < keys.length; i++ ) {
      String key = keys[i].toString();
      Object json = obj.getValue(key);
      if (json instanceof JsonObject) {
        traverseMutating((JsonObject) json, mutateValues, mutateKeys);
      } else if (json instanceof JsonArray) {
        traverseMutating((JsonArray) json, mutateValues, mutateKeys);
      } else if (json instanceof List) { // internal representation of JsonArray is List
        traverseMutating(new JsonArray((List) json), mutateValues, mutateKeys);
      } else if (json instanceof Map) { // internal representation of JsonObject is Map
        traverseMutating(new JsonObject((Map) json), mutateValues, mutateKeys);
      } else {
        Object newJson = mutateValues.apply(json);
        if (json != null && !json.equals(newJson)) {
          obj.put(key, newJson);
        }
      }

      String newKey = mutateKeys.apply(key);
      if (!newKey.equals(key)) {
        obj.remove(key);
        obj.put(newKey, json);
      }
    }
  }

  private static void traverseMutating(JsonArray arr, Function<Object, Object> mutateValues, Function<String, String> mutateKeys) {
    for (int i = 0; i < arr.size(); i++) {
      Object json = arr.getValue(i);
      if (json instanceof JsonObject) {
        traverseMutating((JsonObject) json, mutateValues, mutateKeys);
      } else if (json instanceof JsonArray) {
        traverseMutating((JsonArray) json, mutateValues, mutateKeys);
      } else if (json instanceof List) { // internal representation of JsonArray is List
        traverseMutating(new JsonArray((List) json), mutateValues, mutateKeys);
      } else if (json instanceof Map) { // internal representation of JsonObject is Map
        traverseMutating(new JsonObject((Map) json), mutateValues, mutateKeys);
      } else {
        Object newJson = mutateValues.apply(json);
        if (json != null && !json.equals(newJson)) {
          arr.getList().set(i, newJson);
        }
      }
    }
  }

}
