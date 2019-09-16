package com.cloudentity.tools.vertx.tracing.internals;

import io.opentracing.propagation.TextMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

public class MapTextMap implements TextMap {
  private Map<String, String> map = new HashMap<>();

  @Override
  public Iterator<Map.Entry<String, String>> iterator() {
    return map.entrySet().iterator();
  }

  @Override
  public void put(String key, String value) {
    map.put(key, value);
  }

  public String get(String key) {
    return map.get(key);
  }

  public String print() {
    return map.entrySet().stream()
      .map(e -> e.getKey() + "=" + e.getValue())
      .collect(Collectors.joining(" "));
  }
}
