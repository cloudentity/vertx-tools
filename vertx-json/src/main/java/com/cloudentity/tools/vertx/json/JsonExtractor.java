package com.cloudentity.tools.vertx.json;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class JsonExtractor {
  private static final String safe_unit_s = String.valueOf((char)31);
  private static final Logger log = LoggerFactory.getLogger(JsonExtractor.class);

  private static LinkedList<String> resolvePath(String path) {
    return new LinkedList<>(Arrays.stream(path
            .replaceAll("\\\\.", safe_unit_s)
            .split("\\."))
            .map(k -> k.replaceAll(safe_unit_s, "."))
            .collect(Collectors.toList()));
  }

  public static Optional<JsonObject> resolve(JsonObject conf, String path) {
    return resolve(conf, resolvePath(path), path);
  }

  public static Optional<JsonObject> resolve(JsonObject conf, LinkedList<String> path, String originalPath) {
    if (path.isEmpty() || conf == null) return Optional.ofNullable(conf);
    else {
      String first = path.pollFirst();
      try {
        return resolve(conf.getJsonObject(first), path, originalPath);
      } catch (Exception ex) {
        log.error("Configuration at '{}' for path '{}' must be JsonObject. {}", first, originalPath, ex.getMessage());
        return Optional.empty();
      }
    }
  }

  public static Optional<Object> resolveValue(JsonObject conf, String path) {
    LinkedList<String> pathWithoutLast = resolvePath(path);
    String last = pathWithoutLast.removeLast();

    Optional<JsonObject> resolve = resolve(conf, pathWithoutLast, path);

    if (resolve.isPresent()) {
      return Optional.ofNullable(resolve.get().getValue(last));
    } else {
      return Optional.empty();
    }
  }
}
