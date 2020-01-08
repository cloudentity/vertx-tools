package com.cloudentity.tools.vertx.conf;

import io.vertx.core.json.JsonObject;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SysReferenceTest {
  @After
  public void cleanup() {
    System.clearProperty("PORT");
  }

  @Test
  public void shouldReplaceWithSystemProperty() {
    // given
    JsonObject conf =
      new JsonObject().put("port", "$sys:PORT:int:8000");

    System.setProperty("PORT", "9000");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals(new Integer(9000), result.getInteger("port"));
  }

  @Test
  public void shouldReplaceWithSystemPropertyWhenDefaultValueMissing() {
    // given
    JsonObject conf =
    new JsonObject().put("port", "$sys:PORT:int");

    System.setProperty("PORT", "9000");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals(new Integer(9000), result.getInteger("port"));
  }

  @Test
  public void shouldReplaceWithNullValueIfSystemPropertyMissingAndNoDefaultValue() {
    // given
    JsonObject conf =
    new JsonObject().put("port", "$sys:PORT:int");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals(null, result.getInteger("port"));
  }

  @Test
  public void shouldReplaceWithDefaultValueIfSystemPropertyMissing() {
    // given
    JsonObject conf =
    new JsonObject().put("port", "$sys:PORT:int:8000");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals(new Integer(8000), result.getInteger("port"));
  }

  @Test
  public void shouldReplaceWithPropertyValueInExpression() {
    // given
    JsonObject conf =
      new JsonObject().put("port", "$sys:{PORT}000:int:8000");

    System.setProperty("PORT", "9");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals(new Integer(9000), result.getInteger("port"));
  }

  @Test
  public void shouldReplaceWithDefaultValueInExpression() {
    // given
    JsonObject conf =
      new JsonObject().put("port", "$sys:{PORT}000:int:9");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals(new Integer(9000), result.getInteger("port"));
  }

  @Test
  public void shouldReplaceWithNullIfValueNotSetInExpression() {
    // given
    JsonObject conf =
      new JsonObject().put("port", "$sys:{PORT}000:int");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals(null, result.getInteger("port"));
  }

  @Test
  public void shouldReplaceWithNullIfUnsupportedValueType() {
    // given
    JsonObject conf =
    new JsonObject().put("port", "$sys:PORT:bleh:8000");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals(null, result.getInteger("port"));
  }

  @Test
  public void shouldReplaceWithNullIfInvalidaReference() {
    // given
    JsonObject conf =
    new JsonObject().put("port", "$sys:PORT:");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals(null, result.getInteger("port"));
  }
}
