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
}
