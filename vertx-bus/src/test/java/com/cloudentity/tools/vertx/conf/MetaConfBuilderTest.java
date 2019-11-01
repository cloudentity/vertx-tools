package com.cloudentity.tools.vertx.conf;

import io.vavr.control.Either;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class MetaConfBuilderTest {
  @Test
  public void shouldInjectSingleConfigStoreModuleAndResolveReferences() {
    // given
    JsonArray stores = new JsonArray().add(new JsonObject().put("module", "store-single"));
    JsonObject meta = new JsonObject().put("stores", stores);

    System.setProperty("A", "a");

    // when
    Either<List<ConfBuilder.MissingModule>, MetaConfBuilder.MetaConfig> result = MetaConfBuilder.buildFinalMetaConfig(meta);

    // then
    Assert.assertTrue(result.isRight());
    Assert.assertTrue(result.get().resolvedMetaConfig.getJsonArray("stores").size() == 1);
    Assert.assertTrue(result.get().resolvedMetaConfig.getJsonArray("stores").getJsonObject(0).getString("x").equals("a"));
  }

  @Test
  public void shouldInjectSingleConfigStoreModuleBetweenOtherStores() {
    // given
    JsonArray stores =
      new JsonArray()
        .add(new JsonObject())
        .add(new JsonObject().put("module", "store-single"))
        .add(new JsonObject());

    JsonObject meta = new JsonObject().put("stores", stores);

    // when
    Either<List<ConfBuilder.MissingModule>, MetaConfBuilder.MetaConfig> result = MetaConfBuilder.buildFinalMetaConfig(meta);

    // then
    Assert.assertTrue(result.isRight());
    Assert.assertTrue(result.get().resolvedMetaConfig.getJsonArray("stores").size() == 3);
  }

  @Test
  public void shouldInjectMultiConfigStoreModuleAndResolveReferences() {
    // given
    JsonArray stores = new JsonArray().add(new JsonObject().put("module", "store-multi"));
    JsonObject meta = new JsonObject().put("stores", stores);

    System.setProperty("A", "a");
    System.setProperty("B", "b");

    // when
    Either<List<ConfBuilder.MissingModule>, MetaConfBuilder.MetaConfig> result = MetaConfBuilder.buildFinalMetaConfig(meta);

    // then
    Assert.assertTrue(result.isRight());
    Assert.assertTrue(result.get().resolvedMetaConfig.getJsonArray("stores").size() == 2);
    Assert.assertTrue(result.get().resolvedMetaConfig.getJsonArray("stores").getString(0).equals("a"));
    Assert.assertTrue(result.get().resolvedMetaConfig.getJsonArray("stores").getString(1).equals("b"));
  }

  @Test
  public void shouldInjectMultiConfigStoreModuleBetweenOtherStores() {
    // given
    JsonArray stores =
      new JsonArray()
        .add(new JsonObject())
        .add(new JsonObject().put("module", "store-multi"))
        .add(new JsonObject());

    JsonObject meta = new JsonObject().put("stores", stores);

    // when
    Either<List<ConfBuilder.MissingModule>, MetaConfBuilder.MetaConfig> result = MetaConfBuilder.buildFinalMetaConfig(meta);

    // then
    Assert.assertTrue(result.isRight());
    Assert.assertTrue(result.get().resolvedMetaConfig.getJsonArray("stores").size() == 4);
  }
}
