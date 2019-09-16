package com.cloudentity.tools.vertx.conf.ext;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SecretsMaskTest {
  @Test
  public void shouldMaskSimpleStringValue() {
    JsonObject config =
      new JsonObject().put("secret", "x");

    JsonObject expected =
      new JsonObject().put("secret", "***");

    JsonObject actual = SecretsMask.create(config);
    assertEquals(expected, actual);
  }

  @Test
  public void shouldMaskSimpleBooleanValue() {
    JsonObject config =
      new JsonObject().put("secret", true);

    JsonObject expected =
      new JsonObject().put("secret", "***");

    JsonObject actual = SecretsMask.create(config);
    assertEquals(expected, actual);
  }

  @Test
  public void shouldMaskSimpleNestedValue() {
    JsonObject config =
      new JsonObject().put("secret", new JsonObject().put("nested", true));

    JsonObject expected =
      new JsonObject().put("secret", new JsonObject().put("nested", "***"));

    JsonObject actual = SecretsMask.create(config);
    assertEquals(expected, actual);
  }

  @Test
  public void shouldMaskSimpleArrayValues() {
    JsonObject config =
      new JsonObject().put("secret", new JsonArray().add("x").add("y"));

    JsonObject expected =
      new JsonObject().put("secret", new JsonArray().add("***").add("***"));

    JsonObject actual = SecretsMask.create(config);
    assertEquals(expected, actual);
  }

  @Test
  public void shouldMaskNestedArrayValues() {
    JsonObject config =
      new JsonObject().put("secret", new JsonArray().add(new JsonObject().put("nested", true)).add("y"));

    JsonObject expected =
      new JsonObject().put("secret", new JsonArray().add(new JsonObject().put("nested", "***")).add("***"));

    JsonObject actual = SecretsMask.create(config);
    assertEquals(expected, actual);
  }

}
