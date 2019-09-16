package com.cloudentity.tools.vertx.conf.consuljson;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;

public class ConsulConfigUtilsTest {

  @Test
  public void folderAsAValidJsonObjectTest() {
    LinkedList<String> keys = new LinkedList<>(Arrays.asList(new String[]{ "parent", "theFormerChild", "theLatterChild" }));
    JsonObject rec = JsonObject.mapFrom(ConsulConfigUtils.folderToJson(new JsonObject(), keys, new JsonObject("{ \"value\": \"result\" }")));

    Assert.assertEquals("{\"parent\":{\"theFormerChild\":{\"theLatterChild\":{\"value\":\"result\"}}}}", rec.toString());
  }

  @Test
  public void folderHasValidJsonArrayTest() {
    LinkedList<String> keys = new LinkedList<>(Arrays.asList(new String[]{ "parent", "theFormerChild", "theLatterChild" }));
    JsonObject rec = JsonObject.mapFrom(ConsulConfigUtils.folderToJson(new JsonObject(), keys, new JsonArray("[\"dddd\", \"aaaaa\"]")));

    Assert.assertEquals("{\"parent\":{\"theFormerChild\":{\"theLatterChild\":[\"dddd\",\"aaaaa\"]}}}", rec.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void throwExceptionWhenCannotDetermineJsonStructureTest() {
    ConsulConfigUtils.determineJsonStructure("prod/path","invalid json");
  }

  @Test
  public void returnJsonStructureWhenStringLooksLikeValidJsonTest() {
    Object jsonStructure = ConsulConfigUtils.determineJsonStructure("prod/path", "[\"a\", \"b\"]");
    Assert.assertFalse(jsonStructure instanceof JsonObject);
    Assert.assertTrue(jsonStructure instanceof JsonArray);

    Object jsonStructure2 = ConsulConfigUtils.determineJsonStructure("prod/path", "{\"k\": \"v\"}");
    Assert.assertFalse(jsonStructure2 instanceof JsonArray);
    Assert.assertTrue(jsonStructure2 instanceof JsonObject);
  }
}
