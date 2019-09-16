package com.cloudentity.tools.vertx.json;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;

public class JsonExtractor {
  private static final Logger log = LoggerFactory.getLogger(JsonExtractor.class);

  public static Optional<JsonObject> resolve(JsonObject conf, String path) {
    return resolve(conf, new LinkedList<>(Arrays.asList(path.split("\\."))), path);
  }

  public static Optional<JsonObject> resolve(JsonObject conf, LinkedList<String> path, String originalPath) {
    if (path.isEmpty() || conf == null) return Optional.ofNullable(conf);
    else {
      try {
        return resolve(conf.getJsonObject(path.pollFirst()), path, originalPath);
      } catch (Exception ex) {
        log.error("Configuration at '{}' path must be JsonObject. {}", originalPath, ex.getMessage());
        return Optional.empty();
      }
    }
  }

  public static Optional<Object> resolveValue(JsonObject conf, String path) {
    LinkedList<String> pathWithoutLast = new LinkedList<>(Arrays.asList(path.split("\\.")));
    String last = pathWithoutLast.removeLast();

    Optional<JsonObject> resolve = resolve(conf, pathWithoutLast, path);

    if (resolve.isPresent()) {
      return Optional.ofNullable(resolve.get().getValue(last));
    } else {
      return Optional.empty();
    }
  }
}
