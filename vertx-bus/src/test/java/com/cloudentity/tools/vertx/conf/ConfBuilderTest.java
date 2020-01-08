package com.cloudentity.tools.vertx.conf;

import io.vavr.control.Either;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConfBuilderTest {
  @Test
  public void shouldMergeSimpleModules() {
    JsonObject raw =
      new JsonObject()
        .put("x", "$env:X:string:x")
        .put("modules", new JsonArray().add("builder/a").add("builder/b"));

    Either<List<ConfBuilder.MissingModule>, ConfBuilder.Config> result = ConfBuilder.buildFinalConfig(raw);

    assertTrue(result.isRight());
    ConfBuilder.Config config = result.get();

    List<ResolvedRef> refsRaw = ConfReference.findResolvedEnvRefs(raw);
    assertEquals(1, refsRaw.size());
    assertEquals("x", refsRaw.get(0).resolvedValue);

    List<ResolvedRef> refsA = ConfReference.findResolvedEnvRefs(config.validModules.get(0).rawConfig);
    assertEquals(1, refsA.size());
    assertEquals("a", refsA.get(0).resolvedValue);

    List<ResolvedRef> refsB = ConfReference.findResolvedEnvRefs(config.validModules.get(1).rawConfig);
    assertEquals(1, refsB.size());
    assertEquals("b", refsB.get(0).resolvedValue);

  }

  @Test
  public void shouldMergeModuleInstanceWithOverwrittenEnv() {
    JsonObject raw =
      new JsonObject()
        .put("env", new JsonObject().put("Y", "123"))
        .put("modules", new JsonArray().add(new JsonObject().put("module", "builder/multi-module").put("env", new JsonObject().put("Y", "abc"))));

    Either<List<ConfBuilder.MissingModule>, ConfBuilder.Config> result = ConfBuilder.buildFinalConfig(raw);

    assertTrue(result.isRight());
    ConfBuilder.Config config = result.get();
    assertEquals("abc", config.resolvedConfig.getString("y"));
  }

  @Test
  public void shouldMergeModuleInstanceWithGlobalEnv() {
    JsonObject raw =
      new JsonObject()
        .put("env", new JsonObject().put("Y", "123"))
        .put("modules", new JsonArray().add(new JsonObject().put("module", "builder/multi-module")));

    Either<List<ConfBuilder.MissingModule>, ConfBuilder.Config> result = ConfBuilder.buildFinalConfig(raw);

    assertTrue(result.isRight());
    ConfBuilder.Config config = result.get();
    assertEquals("123", config.resolvedConfig.getString("y"));
  }

  @Test
  public void shouldMaskSecrets() {
    // given
    JsonObject mask =
      new JsonObject()
        .put("secret", new JsonObject().put("nested", "***"));

    JsonObject config =
      new JsonObject()
        .put("secret", new JsonObject().put("nested", true))
        .put("ref", "$ref:secret.nested")
        .put("_mask", mask);

    // when
    Either<List<ConfBuilder.MissingModule>, ConfBuilder.Config> result = ConfBuilder.buildFinalConfig(config);

    // then
    JsonObject expectedResolved =
      new JsonObject()
        .put("secret", new JsonObject().put("nested", true))
        .put("ref", true);

    JsonObject expectedResolvedWithMask =
      new JsonObject()
        .put("secret", new JsonObject().put("nested", "***"))
        .put("ref", "***");

    assertEquals(expectedResolvedWithMask, result.get().resolvedConfigWithMask);
    assertEquals(expectedResolved, result.get().resolvedConfig);

  }
}
