package com.cloudentity.tools.vertx.conf.consuljson;

import io.vertx.config.spi.ConfigStore;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.KeyValue;
import io.vertx.ext.consul.KeyValueList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Reads sub-tree of configuration from Consul at configured path.
 * Expects Json array or Json object as a leaf in the tree.
 *
 * The store configuration is used to create an instance of ConsulClient.
 * Check the documentation of the Vert.x Consul Client for further details.
 *
 * Parameters specific to the Consul Configuration Store:
 *
 * * `path`: Consul key at which our configuration is stored
 * * `exclude`: optional, an array of regular expressions, if path of value in Consul (relative to `path`) matches at least one of the regular expressions then it's ignored
 *
 * Example store configuration:
 *
 * .meta-config
 * [source,json]
 * ----
 * {
 *   "type": "classpath",
 *   "format": "json",
 *   "config": {
 *     "host": "localhost",
 *     "port": 8500,
 *     "path": "prod/api-gateway",
 *     "exclude": ["hazelcast/.*"]
 *   }
 * }
 * ----
 *
 *
 *
 * This implementation covers 3 cases:
 *
 * * path points to node with value - value must be a valid Json object,
 * * path points to configuration sub tree - value must be a valid Json object or array,
 * * path points to configuration sub tree with value as top level leaf
 *
 * Let's say we have following key in Consul `prod/api-gateway/rules/sla` with
 * value `{ "key": "value"}`. ConsulFolderConfigStore looks in Consul for given path,
 * removes path element from configuration key and creates JSON object from
 * data fetched from Consul.
 *
 * Configuration output for the above example:
 *
 * [source,json]
 * ----
 * {
 *  "rules": {
 *    "SLA": {
 *      "key": "value"
 *    }
 *  }
 * }
 * ----
 *
 * Let's say we have another value in our directory, but this value is assigned to
 * root element (prod/api-gateway) of tree. For example when we have another value
 * `{"topKey": "topValue"}` then the output will look like:
 *
 * [source,json]
 * ----
 * {
 *  "rules": {
 *    "SLA": {
 *      "key": "value"
 *    }
 *  },
 *  "topKey": "topValue"
 * }
 * ----
 *
 * Consider situation when tree's leaf is an array. In this case value looks like
 * `["former", "latter"]`. After all transformations the output will look like:
 *
 * [source,json]
 * ----
 * {
 *  "rules": {
 *    "SLA": [
 *      "former",
 *      "latter"
 *    ]
 *  }
 * }
 * ----
 *
 * When Consul leaf value is empty then the value of corresponding attribute in output is null.
 *
 * When prefix path matches entire directory path then the value must be a valid Json object.
 * For example, when directory path looks like `prod/api-gateway`
 * and value is equal to `{"firstKey": "firstValue", "secKey", "secValue"}`, then the output will look like:
 *
 * [source,json]
 * ----
 * {
 *   "firstKey": "firstValue",
 *   "secKey": "secValue"
 * }
 * ----
 *
 * All matched subtrees are deep merged into JSON object representation of Consul
 * directory configuration.
 *
 * When it's impossible to parse JSON, then IllegalArgumentException will be thrown.
 *
 * The config store can be disabled by setting `disabled` to `true` in the configuration, e.g.
 *
 * [source,json]
 * ----
 * {
 *   "host": "localhost",
 *   "port": 8500,
 *   "path": "prod/api-gateway",
 *   "disabled": true
 * }
 * ----
 */
public class ConsulFolderConfigStore implements ConfigStore {
  private static Logger log = LoggerFactory.getLogger(ConsulFolderConfigStore.class);

  private final ConsulClient client;
  private final String path;
  private final List<String> exclude;
  private final boolean disabled;

  private static Collector<JsonObject, JsonObject, JsonObject> mergeJsonObject = Collector.of(
    () -> new JsonObject(),
    (acc, el) -> acc.mergeIn(el, 100),
    (acc, el) -> acc
  );

  public ConsulFolderConfigStore(Vertx vertx, JsonObject configuration) {
    client = ConsulClient.create(vertx, new ConsulClientOptions(configuration));
    path = Optional.ofNullable(configuration.getString("path"))
      .orElse(ConsulConfigUtils.getPathFromPrefixAndKey(configuration).orElse(null));
    disabled = Optional.ofNullable(configuration.getBoolean("disabled")).orElse(false);
    exclude = configuration.getJsonArray("exclude", new JsonArray()).stream().map(x -> x.toString()).collect(Collectors.toList());
  }

  @Override
  public void get(Handler<AsyncResult<Buffer>> completionHandler) {
    if (disabled) {
      completionHandler.handle(Future.succeededFuture(new JsonObject().toBuffer()));
      return;
    }

    try {
      client.getValues(path, kv -> {
        if (kv.succeeded()) {
          Optional<JsonObject> merged = mergeValuesForPath(path, filterOutExcludedPaths(kv.result(), path, exclude));
          if (merged.isPresent()) {
            completionHandler.handle(Future.succeededFuture(merged.get().toBuffer()));
          } else {
            completionHandler.handle(Future.failedFuture("Empty response from Consul"));
          }
        } else {
          completionHandler.handle(Future.failedFuture(kv.cause()));
        }
      });
    } catch (Exception e) {
      log.error("Could not get config values from Consul. {}", e.getMessage());
      completionHandler.handle(Future.failedFuture(e));
    }
  }

  public static KeyValueList filterOutExcludedPaths(KeyValueList kvl, String path, List<String> exclude) {
    List<KeyValue> keyValues = Optional.ofNullable(kvl.getList()).orElse(Collections.emptyList()).stream().filter(kv -> {
      String key = String.join("/", getChunkedFolderPath(kv, path));
      return !exclude.stream().filter(x -> key.matches(x)).findAny().isPresent();
    }).collect(Collectors.toList());


    KeyValueList newKvl = new KeyValueList();
    newKvl.setList(keyValues);
    return newKvl;
  }

  public static Optional<JsonObject> mergeValuesForPath(String path, KeyValueList kvl) {
    if (kvl.isPresent() && kvl.getList().size() > 0) {
      JsonObject merged = kvl.getList().stream()
        .map(keyValueToJsonObject(path))
        .collect(mergeJsonObject);
      return Optional.of(merged);
    } else {
      log.warn("Empty response from Consul");
      return Optional.empty();
    }
  }

  private static Function<KeyValue, JsonObject> keyValueToJsonObject(String path) {
    return kv -> {
      LinkedList<String> chunkedFolderPath = getChunkedFolderPath(kv, path);
      if (chunkedFolderPath.size() > 1) {
        return JsonObject.mapFrom(ConsulConfigUtils.folderToJson(new JsonObject(), chunkedFolderPath, ConsulConfigUtils.determineJsonStructure(path, kv.getValue())));
      } else if (kv.getValue() == null || kv.getValue().isEmpty()) {
        return new JsonObject();
      } else {
        try {
          return new JsonObject(kv.getValue());
        } catch (DecodeException de) {
          log.error("Root element value must be valid JSON object");
          throw new IllegalArgumentException("Root element value is not valid JSON object", de);
        }
      }
    };
  }

  private static LinkedList<String> getChunkedFolderPath(KeyValue kv, String path) {
    String shortenedKey = kv.getKey().replaceFirst(path, "");
    List<String> filteredKeys = Arrays.asList(shortenedKey.split("/")).stream()
      .filter(sk -> !sk.isEmpty())
      .collect(Collectors.toList());

    return new LinkedList<>(filteredKeys);
  }

  @Override
  public void close(Handler<Void> completionHandler) {
    client.close();
    completionHandler.handle(null);
  }
}