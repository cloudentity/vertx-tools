package com.cloudentity.tools.vertx.conf;

import io.vertx.core.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConfNullifierTest {

  @Test
  public void shouldNullifyObjectIfEmpty() {
    JsonObject conf =
      new JsonObject()
        .put("x", new JsonObject().put("_nullify", true));

    ConfNullifier.nullify(conf);

    assertEquals(null, conf.getJsonObject("x"));
  }

  @Test
  public void shouldNullifyObjectIfEmptyValues() {
    JsonObject conf =
      new JsonObject()
        .put("x", new JsonObject().put("_nullify", true).put("y", (String) null));

    ConfNullifier.nullify(conf);

    assertEquals(null, conf.getJsonObject("x"));
  }

  @Test
  public void shouldNotNullifyObjectIfNonEmptyValues() {
    JsonObject conf =
      new JsonObject()
        .put("x", new JsonObject().put("_nullify", true).put("y", "x"));

    ConfNullifier.nullify(conf);

    assertEquals(new JsonObject().put("y", "x"), conf.getJsonObject("x"));
  }

  @Test
  public void shouldNotNullifyDeepObjectIfNonEmptyValues() {
    JsonObject conf =
      new JsonObject()
        .put("x", new JsonObject().put("_nullify", true).put("y", new JsonObject().put("z", "w")));

    ConfNullifier.nullify(conf);
    assertEquals(new JsonObject().put("y", new JsonObject().put("z", "w")), conf.getJsonObject("x"));
  }

  @Test
  public void shouldNullifyChildFirst() {
    JsonObject conf = new JsonObject("{\"x\":{\"y\":{\"_nullify\":true,\"z\":{\"_nullify\":true,\"w\":{\"_nullify\":true,\"a\":null}}}}}");
    ConfNullifier.nullify(conf);
    assertEquals(new JsonObject().put("x", new JsonObject()), conf);
  }
}
