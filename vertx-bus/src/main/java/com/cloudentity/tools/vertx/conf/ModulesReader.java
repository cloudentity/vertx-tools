package com.cloudentity.tools.vertx.conf;

import io.vavr.control.Try;
import io.vertx.core.json.JsonObject;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ModulesReader {
  private static final String MODULES_FOLDER = "modules";
  private static Pattern moduleNamePattern = Pattern.compile(MODULES_FOLDER + "/(.+)\\..+");

  public static List<String> readAvailableModuleNames() {
    Reflections r = new Reflections(MODULES_FOLDER, new ResourcesScanner());

    ArrayList<String> result = new ArrayList<>();
    new ArrayList(r.getResources(x -> true)).forEach(module -> {
      Matcher matcher = moduleNamePattern.matcher(module.toString());
      if (matcher.find()) {
        result.add(matcher.group(1));
      }
    });
    return result;
  }

  public static Try<JsonObject> readModuleConfigFromClasspath(String moduleName) {
    String path = MODULES_FOLDER + "/" + moduleName + ".json";

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
}
