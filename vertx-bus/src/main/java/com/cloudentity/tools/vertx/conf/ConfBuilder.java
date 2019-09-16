package com.cloudentity.tools.vertx.conf;

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
    public final JsonObject rawConfig;

    public ValidRawModule(String name, JsonObject rawConfig) {
      this.name = name;
      this.rawConfig = rawConfig;
    }
  }

  public static Either<List<MissingModule>, Config> buildFinalConfig(JsonObject rawRootConfig) {
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
    JsonArray requiredModuleNames = rawRootConfig.getJsonArray("requiredModules", new JsonArray());
    JsonArray defaultModuleNames  = rawRootConfig.getJsonArray("defaultModules", new JsonArray());

    JsonArray moduleNames = requiredModuleNames.copy();
    moduleNames.addAll(Optional.ofNullable(ConfReference.populateEnvRefs(rawRootConfig).getJsonArray("modules")).orElse(defaultModuleNames));

    List<Either<MissingModule, ValidRawModule>> results = new ArrayList();
    moduleNames.forEach(moduleName -> {
      Try<JsonObject> result = ModulesReader.readModuleConfigFromClasspath(moduleName.toString());
      if (result.isSuccess()) results.add(Either.right(new ValidRawModule(moduleName.toString(), result.get())));
      else                    results.add(Either.left(new MissingModule(moduleName.toString(), result.getCause())));
    });
    return results;
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
