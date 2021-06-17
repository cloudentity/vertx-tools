package com.cloudentity.tools.vertx.conf;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConfSpringlikeReferenceTest {

  @Test
  public void shouldReplaceSimpleSingleSpringlikeReferenceWithoutDefaultValue() {
    // given
    JsonObject conf = new JsonObject().put("x","${y}").put("y", 100);

    // when
    JsonObject result = ConfSpringlikeReference.populateRefs(conf);

    // then
    assertEquals("100", result.getValue("x"));
  }

  @Test
  public void shouldReplaceSimpleSingleSpringlikeReferenceWithEnvFallback() {
    // given
    JsonObject env = new JsonObject().put("Y", "100");
    JsonObject conf = new JsonObject().put("x","${Y}").put("env", env);

    // when
    JsonObject result = ConfSpringlikeReference.populateRefs(conf);

    // then
    assertEquals("100", result.getValue("x"));
  }

  @Test
  public void shouldReplaceSimpleSingleSpringlikeReferenceWithDefaultValue() {
    // given
    JsonObject conf = new JsonObject().put("x","${y:200}").put("y", 100);

    // when
    JsonObject result = ConfSpringlikeReference.populateRefs(conf);

    // then
    assertEquals("100", result.getValue("x"));
  }

  @Test
  public void shouldReplaceSimpleMultiSpringlikeReference() {
    // given
    JsonObject conf = new JsonObject().put("x","${host}:${port}").put("host", "localhost").put("port", 8080);

    // when
    JsonObject result = ConfSpringlikeReference.populateRefs(conf);

    // then
    assertEquals("localhost:8080", result.getValue("x"));
  }

  @Test
  public void shouldReplaceNestedSingleSpringlikeReference() {
    // given
    JsonObject conf = new JsonObject().put("x","${y.z}").put("y", new JsonObject().put("z", 100));

    // when
    JsonObject result = ConfSpringlikeReference.populateRefs(conf);

    // then
    assertEquals("100", result.getValue("x"));
  }

  @Test
  public void shouldReplaceTransitiveSingleSpringlikeReference() {
    // given
    JsonObject conf = new JsonObject().put("z", 100).put("x","${y}").put("y", "${z}");

    // when
    JsonObject result = ConfSpringlikeReference.populateRefs(conf);

    // then
    assertEquals("100", result.getValue("x"));
  }

  @Test
  public void shouldReplaceWithEmptyStringIfSimpleReferenceMissing() {
    // given
    JsonObject conf = new JsonObject().put("x","${y}");

    // when
    JsonObject result = ConfSpringlikeReference.populateRefs(conf);

    // then
    assertEquals("", result.getValue("x"));
  }

  @Test
  public void shouldReplaceWithEmptyStringIfMultiReferenceMissing() {
    // given
    JsonObject conf = new JsonObject().put("x","${host}:${port}");

    // when
    JsonObject result = ConfSpringlikeReference.populateRefs(conf);

    // then
    assertEquals(":", result.getValue("x"));
  }

  @Test
  public void shouldReplaceWithDefaultValueIfSimpleReferenceMissing() {
    // given
    JsonObject conf = new JsonObject().put("x","${y:abc}");

    // when
    JsonObject result = ConfSpringlikeReference.populateRefs(conf);

    // then
    assertEquals("abc", result.getValue("x"));
  }

  @Test
  public void shouldReplaceWithDefaultValueStringIfMultiReferenceMissing() {
    // given
    JsonObject conf = new JsonObject().put("x","${host:localhost}:${port:8080}");

    // when
    JsonObject result = ConfSpringlikeReference.populateRefs(conf);

    // then
    assertEquals("localhost:8080", result.getValue("x"));
  }

  @Test
  public void shouldReplaceSimpleSingleSpringlikeReferenceInArrayWithoutDefaultValue() {
    // given
    JsonObject conf = new JsonObject().put("x", new JsonArray().add("${y}")).put("y", 100);

    // when
    JsonObject result = ConfSpringlikeReference.populateRefs(conf);

    // then
    assertEquals("100", result.getJsonArray("x").getValue(0));
  }

  @Test
  public void shouldIgnoreSimplePath() {
    // given
    JsonObject ignoredPaths = new JsonObject().put("some-paths", new JsonArray().add("x"));
    JsonObject conf = new JsonObject()
      .put("x", new JsonArray().add("${y}"))
      .put("y", 100)
      .put(ConfSpringlikeReference.IGNORED_PATHS_KEY, ignoredPaths);

    // when
    JsonObject result = ConfSpringlikeReference.populateRefs(conf);

    // then
    assertEquals("${y}", result.getJsonArray("x").getValue(0));
  }

  @Test
  public void shouldIgnoreNestedPath() {
    // given
    JsonObject ignoredPaths = new JsonObject().put("some-paths", new JsonArray().add("x.a")).put("some-other", new JsonArray().add("x.b"));
    JsonObject conf = new JsonObject().
      put("x", new JsonObject().put("a", "${y}").put("b", "${y}").put("c", "${y}"))
      .put("y", 100)
      .put(ConfSpringlikeReference.IGNORED_PATHS_KEY, ignoredPaths);

    // when
    JsonObject result = ConfSpringlikeReference.populateRefs(conf);

    // then
    assertEquals("${y}", result.getJsonObject("x").getString("a"));
    assertEquals("${y}", result.getJsonObject("x").getString("b"));
    assertEquals("100", result.getJsonObject("x").getString("c"));
  }
}
