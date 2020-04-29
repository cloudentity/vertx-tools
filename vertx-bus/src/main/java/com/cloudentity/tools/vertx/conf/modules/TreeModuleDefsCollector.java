package com.cloudentity.tools.vertx.conf.modules;

import com.cloudentity.tools.vertx.json.JsonExtractor;
import com.google.common.collect.Lists;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TreeModuleDefsCollector {
  public static List<JsonObject> collect(JsonObject globalConfig, JsonObject moduleDefJson) {
    String rootPath = moduleDefJson.getString("path");
    String key = moduleDefJson.getString("key");
    boolean idWithPath = Optional.ofNullable(moduleDefJson.getBoolean("idWithPath")).orElse(false);

    Optional<JsonObject> rootOpt = JsonExtractor.resolve(globalConfig, rootPath);
    if (rootOpt.isPresent()) {
      io.vavr.collection.List<String> rootPathList = io.vavr.collection.List.of(rootPath.split("\\."));
      return collectRec(rootOpt.get(), rootPathList, key).stream()
        .map(entry -> {
          if (idWithPath) prependPathToId(entry._1, entry._2);
          return entry._2;
        }).collect(Collectors.toList());
    } else {
      return Lists.newArrayList();
    }
  }

  private static void prependPathToId(io.vavr.collection.List<String> path, JsonObject moduleDef) {
    Optional<String> idOpt = Optional.ofNullable(moduleDef.getString("id"));
    moduleDef.put("id", String.join("-", idOpt.map(id -> path.append(id)).orElse(path)));
  }

  private static List<Tuple2<io.vavr.collection.List<String>, JsonObject>> collectRec(Object obj, io.vavr.collection.List<String> path, String moduleKey) {
    if (obj instanceof JsonObject) {
      JsonObject json = (JsonObject) obj;
      return json.getMap().keySet().stream().flatMap(key -> {
        if (key.equals(moduleKey)) {
          return collectSingle(json.getValue(key), path).stream();
        } else {
          return collectRec(json.getValue(key), path.append(key), moduleKey).stream();
        }
      }).collect(Collectors.toList());

    }

    return new ArrayList<>();
  }

  private static List<Tuple2<io.vavr.collection.List<String>, JsonObject>> collectSingle(Object json, io.vavr.collection.List<String> path) {
    if (json instanceof JsonObject) {
      JsonObject obj = (JsonObject) json;
      return Lists.newArrayList(Tuple.of(path, obj));
    } else if (json instanceof JsonArray) {
      JsonArray arr = (JsonArray) json;
      return arr.stream()
        .filter(x -> x instanceof JsonObject)
        .map(x -> Tuple.of(path, (JsonObject) x))
        .collect(Collectors.toList());
    } else return Lists.newArrayList();
  }
}
