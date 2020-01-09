package com.cloudentity.tools.vertx.conf.modules;

import io.vavr.control.Either;
import io.vavr.control.Try;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class StoreModulesReader {
  private static final String MODULES_FOLDER = "store-modules";

  /**
   *
   * @param moduleName
   * @return JsonObject or JsonArray containing config-store[s] configuration
   */
  public static Try<Either<JsonObject, JsonArray>> readStoreModuleConfigFromClasspath(String moduleName) {
    String path = MODULES_FOLDER + "/" + moduleName + ".json";

    try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
      if (stream != null) {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(stream))) {
          String content = buffer.lines().collect(Collectors.joining("\n"));
          Try<JsonObject> tryObj = Try.of(() -> new JsonObject(content));
          if (tryObj.isSuccess()) {
            return Try.success(Either.left(tryObj.get()));
          } else {
            return Try.success(Either.right(new JsonArray(content)));
          }
        }
      } else {
        return Try.failure(new Exception("Could not find resource '" + path + "' on classpath"));
      }
    } catch (Throwable e) {
      return Try.failure(e);
    }
  }
}
