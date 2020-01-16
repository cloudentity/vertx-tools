package com.cloudentity.tools.vertx.conf;

import com.cloudentity.tools.vertx.conf.modules.ModuleIdReference;
import com.cloudentity.tools.vertx.conf.modules.ModulesReader;
import com.cloudentity.tools.vertx.conf.modules.TreeModuleDefsCollector;
import io.vavr.control.Either;
import io.vavr.control.Try;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConfBuilder {
  public static class Config {
    public final JsonObject rawRootConfig;
    public final JsonObject resolvedConfig;
    public final JsonObject resolvedConfigWithMask;
    public final List<ValidRawModule> validModules;


    public Config(JsonObject rawRootConfig, JsonObject resolvedConfig, JsonObject resolvedConfigWithMask, List<ValidRawModule> validModules) {
      this.rawRootConfig = rawRootConfig;
      this.resolvedConfig = resolvedConfig;
      this.resolvedConfigWithMask = resolvedConfigWithMask;
      this.validModules = validModules;
    }
  }

  public static class MissingModule {
    public final String name;
    public final Throwable error;

    public MissingModule(String name, Throwable error) {
      this.name = name;
      this.error = error;
    }
  }

  public static class ValidRawModule {
    public final String name;
    public final Optional<String> id;
    public final JsonObject rawConfig;

    public ValidRawModule(String name, Optional<String> id, JsonObject rawConfig) {
      this.name = name;
      this.id = id;
      this.rawConfig = rawConfig;
    }
  }

  public static Either<List<MissingModule>, Config> buildFinalConfig(JsonObject rawRootConfigOriginal) {
    JsonObject rawRootConfig = rawRootConfigOriginal.copy();
    List<Either<MissingModule, ValidRawModule>> modules = readRawModulesConfigs(rawRootConfig);

    List<MissingModule> missingModules = modules.stream().filter(x -> x.isLeft()).map(x -> x.getLeft()).collect(Collectors.toList());
    if (!missingModules.isEmpty()) {
      return Either.left(missingModules);
    }

    List<ValidRawModule> validRawModules = modules.stream().filter(x -> x.isRight()).map(x -> x.get()).collect(Collectors.toList());
    JsonObject rawModulesConf = mergeRawClasspathModules(validRawModules);
    JsonObject result = rawModulesConf.copy().mergeIn(rawRootConfig, true);

    JsonObject mask = popMask(result);
    JsonObject maskedResult = applyMask(result.copy(), mask);
    maskedResult = ConfReference.populateRefs(maskedResult, maskedResult);

    result = ConfReference.populateRefs(result, result);

    ConfNullifier.nullify(result);
    ConfNullifier.nullify(maskedResult);

    return Either.right(new Config(rawRootConfig, result, maskedResult, validRawModules));
  }

  public static List<Either<MissingModule, ValidRawModule>> readRawModulesConfigs(JsonObject rawRootConfig) {
    JsonObject globalEnvFallback = Optional.ofNullable(rawRootConfig.getJsonObject("env")).orElse(new JsonObject());
    JsonArray requiredModuleNames = rawRootConfig.getJsonArray("requiredModules", new JsonArray());
    JsonArray defaultModuleNames  = rawRootConfig.getJsonArray("defaultModules", new JsonArray());

    JsonArray moduleDefs = requiredModuleNames.copy();
    moduleDefs.addAll(Optional.ofNullable(ConfReference.populateEnvRefs(rawRootConfig, globalEnvFallback).getJsonArray("modules")).orElse(defaultModuleNames));

    List<Either<MissingModule, ValidRawModule>> results = new ArrayList();
    moduleDefs.forEach(moduleDef -> {
      if (moduleDef instanceof String){
        results.add(resolveSimpleModule(moduleDef.toString()));
      } else if (moduleDef instanceof JsonObject) {
        JsonObject moduleDefJson = (JsonObject) moduleDef;
        if ("tree".equals(moduleDefJson.getString("collect"))) {
          results.addAll(TreeModuleDefsCollector.collect(rawRootConfig, moduleDefJson).stream().map(def -> resolveModuleInstance(globalEnvFallback, def)).collect(Collectors.toList()));
        } else {
          results.add(resolveModuleInstance(globalEnvFallback, (JsonObject) moduleDef));
        }
      }
    });
    return results;
  }

  private static Either<MissingModule, ValidRawModule> resolveSimpleModule(String moduleName) {
    Try<JsonObject> result = ModulesReader.readModuleConfigFromClasspath(moduleName);
    if (result.isSuccess()) {
      JsonObject moduleConfig = result.get();
      ModuleIdReference.populateModuleIdRefs(moduleConfig, Optional.empty()); // removes module-id references
      return Either.right(new ValidRawModule(moduleName, Optional.empty(), moduleConfig));
    } else {
      return Either.left(new MissingModule(moduleName, result.getCause()));
    }
  }

  private static Either<MissingModule, ValidRawModule> resolveModuleInstance(JsonObject globalEnv, JsonObject moduleDef) {
    return buildModuleInstance(globalEnv, moduleDef, Optional.ofNullable(moduleDef.getString("id")));
  }

  private static Either<MissingModule, ValidRawModule> buildModuleInstance(JsonObject globalEnv, JsonObject moduleDef, Optional<String> moduleId) {
    String moduleName = moduleDef.getString("module");
    JsonObject envFallback = globalEnv.copy().mergeIn(Optional.ofNullable(moduleDef.getJsonObject("env")).orElse(new JsonObject()));

    Try<JsonObject> result = ModulesReader.readModuleConfigFromClasspath(moduleName);
    if (result.isSuccess()) {
      JsonObject moduleWithIdResolved  = ModuleIdReference.populateModuleIdRefs(result.get(), moduleId);
      JsonObject moduleWithEnvResolved = ConfReference.populateEnvRefs(moduleWithIdResolved, envFallback);

      return Either.right(new ValidRawModule(moduleName, Optional.empty(), moduleWithEnvResolved));
    } else {
      return Either.left(new MissingModule(moduleName, result.getCause()));
    }
  }

  private static JsonObject mergeRawClasspathModules(List<ValidRawModule> validRawModules) {
    JsonObject conf = new JsonObject();
    validRawModules.forEach(module -> conf.mergeIn(module.rawConfig.copy(), true));
    return conf;
  }

  private static JsonObject popMask(JsonObject o) {
    JsonObject mask = o.getJsonObject("_mask", new JsonObject());
    o.remove("_mask");
    return mask;
  }

  private static JsonObject applyMask(JsonObject o, JsonObject mask) {
    return o.mergeIn(mask);
  }
}
