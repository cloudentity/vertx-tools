package com.cloudentity.tools.vertx.conf.classpath;

import io.vavr.control.Try;
import io.vertx.config.spi.ConfigStore;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;



/**
 * Reads configuration `JsonObject` from a file on classpath.
 *
 * .meta-config.json
 * [source,json]
 * ----
 * {
 *   "scanPeriod": 5000,
 *   "stores": [
 *     {
 *       "type": "classpath",
 *       "format": "json",
 *       "config": {
 *         "path": "config.json"
 *       }
 *     }
 *   ]
 * }
 * ----
 *
 */
public class ClasspathConfigStore implements ConfigStore {

  private final String path;

  ClasspathConfigStore(Vertx vertx, JsonObject configuration) {
    path = configuration.getString("path");
  }

  @Override
  public void get(Handler<AsyncResult<Buffer>> completionHandler) {
    Try<JsonObject> jsonObjectTry = readJsonObjectFromClasspath(this.path);
    if (jsonObjectTry.isSuccess()) {
      completionHandler.handle(Future.succeededFuture(jsonObjectTry.get().toBuffer()));
    } else {
      completionHandler.handle(Future.failedFuture(jsonObjectTry.getCause()));
    }
  }

  public static Try<JsonObject> readJsonObjectFromClasspath(String path) {
    try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
      if (stream != null) {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(stream))) {
          JsonObject conf = new JsonObject(buffer.lines().collect(Collectors.joining("\n")));
          return Try.success(conf);
        }
      } else {
        return Try.failure(new Exception("Could not find resource '" + path + "' on classpath"));
      }
    } catch (Throwable e) {
      return Try.failure(e);
    }
  }

  @Override
  public void close(Handler<Void> completionHandler) {
    completionHandler.handle(null);
  }
}


