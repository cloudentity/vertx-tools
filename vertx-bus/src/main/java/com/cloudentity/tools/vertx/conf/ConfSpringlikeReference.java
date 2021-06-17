package com.cloudentity.tools.vertx.conf;

import com.cloudentity.tools.vertx.json.JsonExtractor;
import com.google.common.collect.Lists;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfSpringlikeReference {
  public static final String IGNORED_PATHS_KEY = "_ignoreSpringRefPaths";

  public static JsonObject populateRefs(JsonObject root) {
    JsonObject filteredRoot = filterOutIgnoredPaths(root);
    JsonObject traversedRoot = traverse(filteredRoot, replaceSpringlikeRef(root));
    return root.mergeIn(traversedRoot, true);
  }

  public static List<String> collectIgnoredPaths(JsonObject root) {
    JsonObject obj = root.getJsonObject(IGNORED_PATHS_KEY, new JsonObject());
    return obj.getMap().values().stream().flatMap(paths -> {
      if (paths instanceof List) {
        Stream<String> stream = ((List) paths).stream().map(x -> x.toString());
        return stream;
      } else if (paths instanceof JsonArray) {
        Stream<String> stream = ((JsonArray) paths).getList().stream().map(x -> x.toString());
        return stream;
      } else {
        Stream<String> stream = Stream.empty();
        return stream;
      }
    }).collect(Collectors.toList());
  }

  public static JsonObject filterOutIgnoredPaths(JsonObject root) {
    List<String> ignoredPaths = collectIgnoredPaths(root);
    JsonObject out = root.copy();
    ignoredPaths.forEach(ignoredPath -> {
      LinkedList<String> path = new LinkedList<>(Lists.newArrayList(ignoredPath.split("\\.")));
      remove(out, path);
    });
    return out;
  }

  public static void remove(JsonObject root, Queue<String> path) {
    if (path.isEmpty()) return;
    else {
      String key = path.poll();
      if (root.containsKey(key)) {
        if (path.isEmpty()) root.remove(key);
        else {
          Object newRoot = root.getValue(key);
          if (newRoot instanceof JsonObject) {
            remove((JsonObject) newRoot, path);
          } else if (newRoot instanceof Map) {
            remove(new JsonObject((Map<String, Object>) newRoot), path);
          } else return;
        }
      } else return;
    }
  }

  public static JsonArray populateRefs(JsonArray root, JsonObject refs) {
    return traverse(root, replaceSpringlikeRef(refs));
  }

  private static Pattern springRefPlaceholder = Pattern.compile(".*(\\$\\{[^}]+?\\}).*");
  private static Function<String, String> replaceSpringlikeRef(JsonObject root) {
    JsonObject envFallback = root.getJsonObject("env", new JsonObject());
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

        String refValue = JsonExtractor.resolveValue(root, refPath).map(x -> x.toString())
          .orElseGet(() ->
            Optional.ofNullable(envFallback.getString(refPath))
              .orElse(Optional.ofNullable(System.getenv(refPath))
              .orElse(defaultValue))
          );
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
