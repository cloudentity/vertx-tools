package com.cloudentity.tools.vertx.conf.consuljson;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.Optional;

public class ConsulConfigUtils {

  private static Logger log = LoggerFactory.getLogger(ConsulConfigUtils.class);

  public static Object folderToJson(JsonObject output, Deque<String> list, Object value) {
    if (list.isEmpty()) {
        return value;
    } else {
      return output.put(list.pop(), folderToJson(new JsonObject(), list, value));
    }
  }

  public static Optional<String> getPathFromPrefixAndKey(JsonObject configuration) {
    return Optional.ofNullable(configuration.getString("prefix"))
      .flatMap(prefix ->
        Optional.ofNullable(configuration.getString("key"))
          .map(key -> prefix + key)
      );
  }

  public static Object determineJsonStructure(String path, String value) {
    try {
      return new JsonObject(value);
    } catch (DecodeException iae) {
      try {
        return new JsonArray(value);
      } catch (DecodeException de) {
        log.error("Could not recognize JSON structure in {} for path {}", value, path);
        throw new IllegalArgumentException(String.format("Neither JSON object nor JSON array in %s", path), de);
      }
    }
  }
}
