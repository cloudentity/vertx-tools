package com.cloudentity.tools.vertx.conf;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConfReferenceTest {
  @Test
  public void shouldReplaceWithSimpleReference() {
    // given
    JsonObject conf =
      new JsonObject()
        .put("obj",
          new JsonObject()
            .put("port", "$ref:defaults.ref-port")
            .put("hosts", new JsonArray().add("$ref:defaults.ref-host1").add("$ref:defaults.ref-host2"))
        );

    JsonObject globalConf =
      new JsonObject()
        .put("defaults",
          new JsonObject()
            .put("ref-port", 9000)
            .put("ref-host1", "localhost")
            .put("ref-host2", "127.0.0.1")
        ).put("conf", conf);

    // when
    JsonObject result = ConfReference.populateInnerRefs(conf, globalConf);

    // then
    JsonObject obj = result.getJsonObject("obj");
    assertEquals(new Integer(9000), obj.getInteger("port"));
    assertEquals("localhost", obj.getJsonArray("hosts").getString(0));
    assertEquals("127.0.0.1", obj.getJsonArray("hosts").getString(1));
  }

  @Test
  public void shouldReplaceWithNestedReference() {
    // given
    JsonObject conf =
      new JsonObject()
        .put("obj",
          new JsonObject()
            .put("port", "$ref:defaults.ref-port")
            .put("hosts", new JsonArray().add("$ref:defaults.ref-host1").add("$ref:defaults.ref-host2"))
        );

    JsonObject globalConf =
      new JsonObject()
        .put("defaults",
          new JsonObject()
            .put("ref-port", 9000)
            .put("ref-host1", "localhost")
            .put("ref-host2", "$ref:defaults.ref-host3")
            .put("ref-host3", "127.0.0.1")
        ).put("conf", conf);

    // when
    JsonObject result = ConfReference.populateInnerRefs(conf, globalConf);

    // then
    JsonObject obj = result.getJsonObject("obj");
    assertEquals(new Integer(9000), obj.getInteger("port"));
    assertEquals("localhost", obj.getJsonArray("hosts").getString(0));
    assertEquals("127.0.0.1", obj.getJsonArray("hosts").getString(1));
  }

  @Test
  public void shouldReplaceWithNullIfReferenceMissing() {
    // given
    JsonObject conf =
      new JsonObject()
        .put("obj",
          new JsonObject().put("port", "$ref:missing-reference")
        );

    JsonObject globalConf =
      new JsonObject().put("conf", conf);

    // when
    JsonObject result = ConfReference.populateInnerRefs(conf, globalConf);

    // then
    JsonObject obj = result.getJsonObject("obj");
    assertEquals(null, obj.getInteger("port"));
  }

  @Test
  public void shouldReplaceWithDefaultValueIfReferenceMissing() {
    // given
    JsonObject conf =
      new JsonObject()
        .put("obj",
          new JsonObject().put("port", "$ref:missing-reference:int:80")
        );

    JsonObject globalConf =
      new JsonObject().put("conf", conf);

    // when
    JsonObject result = ConfReference.populateInnerRefs(conf, globalConf);

    // then
    JsonObject obj = result.getJsonObject("obj");
    assertEquals(new Integer(80), obj.getInteger("port"));
  }

  @Test
  public void shouldReplaceWithEmptyDefaultValueIfReferenceMissing() {
    // given
    JsonObject conf =
      new JsonObject()
        .put("obj",
          new JsonObject().put("host", "$ref:missing-reference:string:")
        );

    JsonObject globalConf =
      new JsonObject().put("conf", conf);

    // when
    JsonObject result = ConfReference.populateInnerRefs(conf, globalConf);

    // then
    JsonObject obj = result.getJsonObject("obj");
    assertEquals("", obj.getString("host"));
  }

  @Test
  public void shouldReplaceWithDefaultValueWithColonIfReferenceMissing() {
    // given
    JsonObject conf =
      new JsonObject()
        .put("obj",
          new JsonObject().put("host", "$ref:missing-reference:string:localhost\\:8080")
        );

    JsonObject globalConf =
      new JsonObject().put("conf", conf);

    // when
    JsonObject result = ConfReference.populateInnerRefs(conf, globalConf);

    // then
    JsonObject obj = result.getJsonObject("obj");
    assertEquals("localhost:8080", obj.getString("host"));
  }

  @Test
  public void shouldReplaceReferenceInArray() {
    // given
    JsonObject conf =
      new JsonObject()
        .put("x", "value")
        .put("values", new JsonArray().add("$ref:x"));

    // when
    JsonObject result = ConfReference.populateInnerRefs(conf, conf);

    // then
    JsonArray arr = result.getJsonArray("values");
    assertEquals("value", arr.getString(0));
  }

  @Test(expected = StackOverflowError.class)
  public void shouldThrowStackOverflow() {
    // given
    JsonObject globalConf =
      new JsonObject()
        .put("conf1",
          new JsonObject().put("ref", "$ref:conf2.ref")
        ).put("conf2",
          new JsonObject().put("ref", "$ref:conf1.ref")
        );

    // when
    JsonObject result = ConfReference.populateInnerRefs(globalConf, globalConf);
  }

  @Test
  public void shouldReplaceStringSysValue() {
    // given
    System.setProperty("X", "value");
    JsonObject conf = new JsonObject().put("x","$sys:X:string");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals("value", result.getValue("x"));
  }

  @Test
  public void shouldReplaceBooleanSysValue() {
    // given
    System.setProperty("X", "true");
    JsonObject conf = new JsonObject().put("x","$sys:X:boolean");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals(true, result.getValue("x"));
  }

  @Test
  public void shouldReplaceIntSysValue() {
    // given
    System.setProperty("X", "10");
    JsonObject conf = new JsonObject().put("x","$sys:X:int");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals(10, result.getValue("x"));
  }

  @Test
  public void shouldReplaceDoubleSysValue() {
    // given
    System.setProperty("X", "1.0");
    JsonObject conf = new JsonObject().put("x","$sys:X:double");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals(1.0d, result.getValue("x"));
  }

  @Test
  public void shouldReplaceSingleStringSysArrayValue() {
    // given
    System.setProperty("X", "[\"value\"]");
    JsonObject conf = new JsonObject().put("x","$sys:X:array");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals(new JsonObject().put("x", new JsonArray().add("value")), result);
  }

  @Test
  public void shouldReplaceMultiStringSysArrayValue() {
    // given
    System.setProperty("X", "[\"value1\",\"value2\"]");
    JsonObject conf = new JsonObject().put("x","$sys:X:array");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals(new JsonObject().put("x", new JsonArray().add("value1").add("value2")), result);
  }

  @Test
  public void shouldReplaceBooleanSysArrayValue() {
    // given
    System.setProperty("X", "[true,false]");
    JsonObject conf = new JsonObject().put("x","$sys:X:array");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals(new JsonObject().put("x", new JsonArray().add(true).add(false)), result);
  }

  @Test
  public void shouldReplaceIntSysArrayValue() {
    // given
    System.setProperty("X", "[1,2]");
    JsonObject conf = new JsonObject().put("x","$sys:X:array");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals(new JsonObject().put("x", new JsonArray().add(1).add(2)), result);
  }

  @Test
  public void shouldReplaceDoubleSysArrayValue() {
    // given
    System.setProperty("X", "[1.0,2.0]");
    JsonObject conf = new JsonObject().put("x","$sys:X:array");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals(new JsonObject().put("x", new JsonArray().add(1.0d).add(2.0d)), result);
  }

  @Test
  public void shouldSetSysArrayValueToNullIfMissing() {
    // given
    JsonObject conf = new JsonObject().put("x","$sys:X_NON_EXISTING:array");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals(null, result.getValue("x"));
  }

  @Test
  public void shouldSetSysArrayValueToDefaultValueIfMissing() {
    // given
    JsonObject conf = new JsonObject().put("x","$sys:X_MISSING:array:[\"value\\:1\",\"value\\:2\"]");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    assertEquals(new JsonObject().put("x", new JsonArray().add("value:1").add("value:2")), result);
  }

  @Test
  public void shouldReplaceSysObjectValue() {
    // given
    System.setProperty("X", "{ \"a\": { \"value\": 1 }, \"b\": { \"value\": [2, 3] } }");
    JsonObject conf = new JsonObject().put("x","$sys:X:object");

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, new JsonObject());

    // then
    JsonObject expected =
      new JsonObject()
        .put("x", new JsonObject()
          .put("a", new JsonObject()
            .put("value", 1)
          ).put("b", new JsonObject()
            .put("value", new JsonArray().add(2).add(3))
          )
        );
    assertEquals(expected, result);
  }

  @Test
  public void shouldFallbackSysReferenceIfValueMissing() {
    // given
    JsonObject sysFallback = new JsonObject().put("X_NON_EXISTING", "[\"X\"]");
    JsonObject conf = new JsonObject().put("x","$sys:X_NON_EXISTING:array").put("sys", sysFallback);

    // when
    JsonObject result = ConfReference.populateSysRefs(conf, sysFallback);

    // then
    assertEquals(new JsonArray().add("X"), result.getValue("x"));
  }

  @Test
  public void shouldFallbackEnvReferenceIfValueMissing() {
    // given
    JsonObject envFallback = new JsonObject().put("X_NON_EXISTING", "[\"X\"]");
    JsonObject conf = new JsonObject().put("x","$env:X_NON_EXISTING:array").put("env", envFallback);

    // when
    JsonObject result = ConfReference.populateEnvRefs(conf, envFallback);

    // then
    assertEquals(new JsonArray().add("X"), result.getValue("x"));
  }
}
