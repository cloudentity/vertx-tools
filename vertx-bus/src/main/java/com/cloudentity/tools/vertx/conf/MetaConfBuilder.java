package com.cloudentity.tools.vertx.conf;

import com.cloudentity.tools.vertx.conf.ConfBuilder.MissingModule;
import io.vavr.control.Either;
import io.vavr.control.Try;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class MetaConfBuilder {
  public static class MetaConfig {
    public final JsonObject rawMetaConfig;
    public final JsonObject resolvedMetaConfig;


    public MetaConfig(JsonObject rawMetaConfig, JsonObject resolvedMetaConfig) {
      this.rawMetaConfig = rawMetaConfig;
      this.resolvedMetaConfig = resolvedMetaConfig;
    }
  }

  public static Either<List<MissingModule>, MetaConfig> buildFinalMetaConfig(JsonObject rawMetaConfig) {
    JsonArray rawStores = rawMetaConfig.getJsonArray("stores");
    JsonArray stores = new JsonArray();
    List<MissingModule> missingModules = new ArrayList<>();

    for (int i = 0; i < rawStores.size(); i++) {
      JsonObject store = rawStores.getJsonObject(i);
      if (store.containsKey("module")) {
        String moduleName = store.getValue("module").toString();
        Try<Either<JsonObject, JsonArray>> result = StoreModulesReader.readStoreModuleConfigFromClasspath(moduleName);

        if (result.isSuccess()) {
          result.get().mapLeft(obj -> stores.add(obj)).map(arr -> stores.addAll(arr));
        } else {
          missingModules.add(new MissingModule(moduleName, result.getCause()));
        }
      } else {
        stores.add(store);
      }
    }

    if (!missingModules.isEmpty()) {
      return Either.left(missingModules);
    }

    rawMetaConfig.put("stores", stores);
    JsonObject resolvedMetaConfig = ConfReference.populateRefs(rawMetaConfig, rawMetaConfig);
    ConfNullifier.nullify(resolvedMetaConfig);
    return Either.right(new MetaConfig(rawMetaConfig.copy(), resolvedMetaConfig));
  }
}
