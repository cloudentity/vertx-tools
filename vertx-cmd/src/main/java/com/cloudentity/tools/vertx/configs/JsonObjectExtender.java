package com.cloudentity.tools.vertx.configs;

import com.google.common.collect.Maps;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JsonObjectExtender {

  public static final Pattern WRAPPED_IN_DOUBLE_QUOTES = Pattern.compile("^\"(.*)\"$");
  protected final Logger log = LoggerFactory.getLogger(this.getClass());

  private static String BOOLEAN = "TRUE|FALSE";
  private static String NUMBER = "^-?\\d+$";
  private static final String JSON_OBJECT = "^\\{.*\\}$";
  private static final String JSON_ARRAY = "^\\[.*\\]$";

  public JsonObject extendConfig(JsonObject currentConfig, List<String> configExtensions) {
    Map<String, String> extensionArguments = getValidExtensionArguments(configExtensions);
    extensionArguments.forEach((path, value) -> replaceValueAtPath(currentConfig, path, value));
    return currentConfig;
  }

  private void replaceValueAtPath(JsonObject currentConfig, String path, String value) {
    try {
      List<String> pathNodes = Arrays.asList(path.split("\\."));

      JsonObject currentNode = currentConfig;
      for (int i = 0; i < pathNodes.size() - 1; i++) {
        currentNode = currentNode.getJsonObject(pathNodes.get(i));
      }

      String lastPathNode = pathNodes.get(pathNodes.size() - 1);
      if (currentNode.getValue(lastPathNode) != null) {
        currentNode.put(lastPathNode, castValue(value));
      } else {
        log.warn("Invalid path " + path);
      }
    } catch (NullPointerException|DecodeException e) {
      log.warn("Invalid path " + path);
    }
  }

  private Map<String, String> getValidExtensionArguments(List<String> configExtensions) {
    return configExtensions.stream()
      .map(this::mapExtensionString)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Object castValue(String value) {

    if (value.toUpperCase().matches(BOOLEAN))
      return Boolean.valueOf(value);
    if (value.matches(NUMBER))
      return Double.valueOf(value);

    Matcher stringMatcher = WRAPPED_IN_DOUBLE_QUOTES.matcher(value);
    if (stringMatcher.find()) {
      return stringMatcher.group(1);
    }
    if (value.matches(JSON_OBJECT)) {
      return new JsonObject(value);
    }
    if (value.matches(JSON_ARRAY)) {
      return new JsonArray(value);
    }

    return value;
  }

  private Optional<Map.Entry<String, String>> mapExtensionString(String extensionString) {
    String[] keyValArray = extensionString.split("=");
    if (keyValArray.length != 2) {
      log.warn("Invalid config extension pattern");
      return Optional.empty();
    }
    return Optional.of(Maps.immutableEntry(keyValArray[0], keyValArray[1]));
  }
}
