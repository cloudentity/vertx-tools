package com.cloudentity.tools.vertx.json;

import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class JsonExtractorTest {
  @Test
  public void shouldResolveTopObject() {
    // given
    JsonObject object = new JsonObject().put("x", new JsonObject().put("key", "value"));

    // when
    Optional<JsonObject> result = JsonExtractor.resolve(object, "x");

    // then
    Assert.assertTrue(result.isPresent());
    Assert.assertEquals("value", result.get().getString("key"));
  }

  @Test
  public void shouldNotResolveNonJsonObject() {
    // given
    JsonObject object = new JsonObject().put("x", "value");

    // when
    Optional<JsonObject> result = JsonExtractor.resolve(object, "x");

    // then
    Assert.assertTrue(!result.isPresent());
  }

  @Test
  public void shouldNotResolveMissingObject() {
    // given
    JsonObject object = new JsonObject();

    // when
    Optional<JsonObject> result = JsonExtractor.resolve(object, "x");

    // then
    Assert.assertTrue(!result.isPresent());
  }

  @Test
  public void shouldResolveNestedObject() {
    // given
    JsonObject object = new JsonObject().put("x", new JsonObject().put("y", new JsonObject().put("key", "value")));

    // when
    Optional<JsonObject> result = JsonExtractor.resolve(object, "x.y");

    // then
    Assert.assertTrue(result.isPresent());
    Assert.assertEquals("value", result.get().getString("key"));
  }
}
