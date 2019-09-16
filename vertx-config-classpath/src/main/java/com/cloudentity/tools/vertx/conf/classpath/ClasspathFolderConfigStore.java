package com.cloudentity.tools.vertx.conf.classpath;

import com.cloudentity.tools.vertx.conf.common.JsonUtils;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vertx.config.spi.ConfigStore;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.launcher.commands.FileSelector;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.vavr.control.Either.*;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Reads configuration JsonObject from files in folder on classpath, merges them and puts in output config at `outputPath`.
 *
 * Example store configuration:
 *
 * .meta-config
 * [source,json]
 * ----
 * {
 *   "type": "classpath-folder",
 *   "format": "json",
 *   "config": {
 *     "path": "config",
 *     "filesets": [
 *         {"pattern": "*json"},
 *         {"pattern": "*.properties"}
 *     ],
 *     "outputPath": "some.output.path"
 *   }
 * }
 * ----
 */
public class ClasspathFolderConfigStore implements ConfigStore {
  private static Logger log = LoggerFactory.getLogger(ClasspathFolderConfigStore.class);

  private final String path;
  private final List<Fileset> filesets;
  private final String outputPath;

  ClasspathFolderConfigStore(Vertx vertx, JsonObject configuration) {
    path = ofNullable(configuration.getString("path")).orElse("");
    filesets = ofNullable(configuration.getJsonArray("filesets")).orElse(new JsonArray()).stream()
        .map(e -> Json.decodeValue(((JsonObject)e).toBuffer(), Fileset.class)).collect(toList());
    outputPath = ofNullable(configuration.getString("outputPath")).orElse(".");
  }

  @Override
  public void get(Handler<AsyncResult<Buffer>> completionHandler) {
    try {
      log.debug("Searching for config in directory: " + path + " with filesets " + filesets);
      String pathToPackage = path.replace('/', '.');
      Set<String> resourceFilePaths = getMatchingResourceFilePaths(pathToPackage);
      log.debug("Found resource files: " + resourceFilePaths);
      List<Either<Throwable, Tuple2<String, Either<JsonArray, JsonObject>>>> resources = loadResources(resourceFilePaths);
      Optional<Throwable> errorOpt = getFirstFailedIfAny(resources);
      if (errorOpt.isPresent()) {
        completionHandler.handle(Future.failedFuture(errorOpt.get()));
      } else {
        JsonObject config = new JsonObject();
        loadResourcesToConfig(getSuccessfullyLoaded(resources), config);
        log.debug("Config object: " + config);
        JsonObject finalConfig = JsonUtils.wrapJsonConfig(config, outputPath);
        log.debug("Final config object: " + finalConfig);
        completionHandler.handle(Future.succeededFuture(finalConfig.toBuffer()));
      }
    } catch (Throwable e) {
      log.error("Handling failed", e);
      completionHandler.handle(Future.failedFuture(e));
    }
  }

  private Set<String> getMatchingResourceFilePaths(String pathToPackage) {
    String concurrentReadExceptionMessage = "zip file closed";
    try {
      Reflections reflections = new Reflections(pathToPackage, new ResourcesScanner());
      return reflections.getResources(Pattern.compile(".*")).stream()
          .filter(filepath -> !filesets.stream()
              .map(fS -> fS.pattern)
              .filter(pattern -> FileSelector.match(pattern, filepath))
              .collect(toList())
              .isEmpty()
          ).collect(toSet());
      /**
       * it may occur that more then one thread will try to read JarFile - as it's not thread safe below try/catch was
       * added to retry hoping that JarFile is free now (SCMO-5758)
       */
    } catch (IllegalStateException ex) {
      if (concurrentReadExceptionMessage.equals(ex.getMessage())) {
        log.warn("Failed to scan package. IllegalStateException: {} occurred. Retrying...", concurrentReadExceptionMessage);
        return getMatchingResourceFilePaths(pathToPackage);
      } else {
        log.error("Unexpected IllegalStateException occur during jarfile scanning. Rethrowing exception");
        throw ex;
      }
    } catch (Exception ex) {
      log.error("Unexpected occur during jarfile scanning. Rethrowing exception");
      throw ex;
    }
  }

  private List<Either<Throwable, Tuple2<String, Either<JsonArray, JsonObject>>>> loadResources(Set<String> resourceFilePaths) {
    return resourceFilePaths.stream()
        .map(file -> {
          String key = getKeyFromFileName(file);
          log.debug("Storing config under key " + key);
          return readFileAsJson(file).map(e -> Tuple.of(key, e));
        })
        .collect(toList());
  }

  private Optional<Throwable> getFirstFailedIfAny(List<Either<Throwable, Tuple2<String, Either<JsonArray, JsonObject>>>> resources) {
    return resources.stream()
        .filter(e -> e.isLeft())
        .map(e -> e.getLeft())
        .findAny();
  }

  private List<Tuple2<String, Either<JsonArray, JsonObject>>> getSuccessfullyLoaded(List<Either<Throwable, Tuple2<String, Either<JsonArray, JsonObject>>>> resources) {
    return resources.stream()
        .filter(e -> e.isRight())
        .map(e -> e.get())
        .collect(toList());
  }

  private void loadResourcesToConfig(List<Tuple2<String, Either<JsonArray, JsonObject>>> resources, JsonObject config) {
    resources.forEach(t -> {
      String configKey = t._1;
      Either<JsonArray, JsonObject> jsonResource = t._2;
      if (jsonResource.isRight()) config.put(configKey, jsonResource.get());
      else config.put(configKey, jsonResource.getLeft());
    });
  }

  private String getKeyFromFileName(String fileName) {
    log.debug("Making key out from filepath " + fileName);
    String name = new File(fileName).getName();
    log.debug("Making key out from file name " + name);
    if (name.endsWith(".json")) {
      return name.substring(0, name.length() - 5);
    } else return name;
  }

  private Either<Throwable, Either<JsonArray, JsonObject>> readFileAsJson(String filepath) {
    log.debug("Getting resource file " + filepath);
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(filepath)) {
      if (stream != null) {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(stream))) {
          String content = buffer.lines().collect(Collectors.joining("\n"));
          try {
            return right(right(new JsonObject(content)));
          } catch (DecodeException e) {
            return right(left(new JsonArray(content)));
          }
        }
      } else {
        return right(right(new JsonObject()));
      }
    } catch (Throwable e) {
      log.error("Failed to read resource", e);
      return left(e);
    }
  }

  @Override
  public void close(Handler<Void> completionHandler) {
    completionHandler.handle(null);
  }

  private static class Fileset {
    public String pattern;

    @Override
    public String toString() {
      return "Fileset{" +
          "pattern='" + pattern + '\'' +
          '}';
    }
  }
}


