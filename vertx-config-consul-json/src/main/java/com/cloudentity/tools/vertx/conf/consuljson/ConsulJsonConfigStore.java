package com.cloudentity.tools.vertx.conf.consuljson;

import io.vertx.config.spi.ConfigStore;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.consul.ConsulClient;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.KeyValueList;

import java.util.Optional;

/**
 * Reads configuration JsonObject from Consul value.
 *
 * The store configuration is used to create an instance of ConsulClient.
 * Check the documentation of the Vert.x Consul Client for further details.
 *
 * And this is the parameters specific to the Consul Configuration Store:
 * `path`: Consul key at which JsonObject is stored
 *
 * Example store configuration:
 *
 * .meta-config
 * [source,json]
 * ----
 * {
 *   "type": "consul-json",
 *   "format": "json",
 *   "config": {
 *     "host": "localhost",
 *     "port": 8500,
 *     "path": "cloudentity/config/authz",
 *     "fallback": {
 *       "key1": "value1",
 *       "key2": "value2"
 *     }
 *   }
 * }
 * ----
 *
 * Optionally to `path` we can set `prefix` and `key` that concatenated are used as `path`.
 * `fallback` is optional, defaults to {} - used when the value at `path` is not set
 *
 */
public class ConsulJsonConfigStore implements ConfigStore {

  private final ConsulClient client;
  private final String path;
  private final Optional<JsonObject> fallback;

  ConsulJsonConfigStore(Vertx vertx, JsonObject configuration) {
    client = ConsulClient.create(vertx, new ConsulClientOptions(configuration));
    path = Optional.ofNullable(configuration.getString("path"))
            .orElse(ConsulConfigUtils.getPathFromPrefixAndKey(configuration).orElse(null));
    fallback = Optional.ofNullable(configuration.getJsonObject("fallback"));
  }

  @Override
  public void get(Handler<AsyncResult<Buffer>> completionHandler) {
    client.getValues(path, kv -> {
      if (kv.succeeded()) {
        KeyValueList list = kv.result();
        if (list.isPresent() && list.getList().size() > 0) {
          try {
            JsonObject config = new JsonObject(list.getList().get(0).getValue());
            completionHandler.handle(Future.succeededFuture(Buffer.buffer(config.toString())));
          } catch (Throwable ex) {
            completionHandler.handle(Future.failedFuture(ex));
          }
        } else {
          JsonObject fallbackConf = fallback.orElse(new JsonObject());
          completionHandler.handle(Future.succeededFuture(Buffer.buffer(fallbackConf.toString())));
        }
      } else {
        completionHandler.handle(Future.failedFuture(kv.cause()));
      }
    });
  }

  @Override
  public void close(Handler<Void> completionHandler) {
    client.close();
    completionHandler.handle(null);
  }
}


