package com.cloudentity.tools.vertx.conf.consuljson;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.consul.KeyValue;
import io.vertx.ext.consul.KeyValueConverter;
import io.vertx.ext.consul.KeyValueList;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Optional;

public class ConsulFolderConfigStoreTest {

  @Test
  public void mergeValuesForValidConsulResponseWithJsonObjectTest() {
    KeyValueList keyValueList = givenKeyValueList("prod/apigw/rules/sla/host", "\"{\\\"yoyo\\\": \\\"yoyoyoy\\\"}\"");
    Optional<JsonObject> entries = ConsulFolderConfigStore.mergeValuesForPath("prod/apigw", keyValueList);
    Assert.assertTrue(entries.isPresent());
    Assert.assertEquals("{\"sla\":{\"host\":{\"yoyo\":\"yoyoyoy\"}}}", entries.get().getJsonObject("rules").toString());
  }

  @Test
  public void mergeValuesForValidConsulResponseWithJsonArrayTest() {
    KeyValueList keyValueList = givenKeyValueList("prod/apigw/rules/sla/host", "\"[{\\\"yoyo\\\": \\\"yoyoyoy\\\"}]\"");
    Optional<JsonObject> entries = ConsulFolderConfigStore.mergeValuesForPath("prod/apigw", keyValueList);
    Assert.assertTrue(entries.isPresent());
    Assert.assertEquals("{\"sla\":{\"host\":[{\"yoyo\":\"yoyoyoy\"}]}}", entries.get().getJsonObject("rules").toString());
  }

  private KeyValueList givenKeyValueList(String path, String value) {
    KeyValue kv = keyValue(path, value);


    ArrayList<KeyValue> kvl = new ArrayList<>();
    kvl.add(kv);

    KeyValueList d = new KeyValueList();
    d.setList(kvl);

    return d;
  }

  private KeyValue keyValue(String path, String value) {
    JsonObject kvs = new JsonObject("{\"LockIndex\": 0, \"key\": \"" + path + "\", \"Flags\": 0, \"value\": " + value + ", \"CreateIndex\": 13, \"ModifyIndex\": 251}");
    KeyValue kv = new KeyValue(kvs);
    KeyValueConverter.fromJson(kvs, kv);
    return kv;
  }

  @Test
  public void mergeValuesForValidConsulResponseWithKeyEmptyAfterTrimmingTest() {
    KeyValueList keyValueList = givenKeyValueList("prod/apigw", "\"{\\\"yoyo\\\": \\\"yoyoyoy\\\"}\"");
    Optional<JsonObject> entries = ConsulFolderConfigStore.mergeValuesForPath("prod/apigw", keyValueList);
    Assert.assertTrue(entries.isPresent());
    Assert.assertEquals("{\"yoyo\":\"yoyoyoy\"}", entries.get().toString());
  }

  @Test
  public void excludeLeafPathTest() {
    KeyValueList keyValueList = new KeyValueList();
    ArrayList<KeyValue> keyValues = new ArrayList<>();
    keyValues.add(keyValue("prod/apigw/a", "{}"));
    keyValues.add(keyValue("prod/apigw/b", "\"invalid\""));
    keyValueList.setList(keyValues);

    ArrayList<String> exclude = new ArrayList<>();
    exclude.add("b");

    KeyValueList filtered = ConsulFolderConfigStore.filterOutExcludedPaths(keyValueList, "prod/apigw", exclude);
    Optional<JsonObject> entries = ConsulFolderConfigStore.mergeValuesForPath("prod/apigw", filtered);
    Assert.assertTrue(entries.isPresent());
    Assert.assertEquals("{}", entries.get().toString());
  }

  @Test
  public void excludeFolderPathTest() {
    KeyValueList keyValueList = new KeyValueList();
    ArrayList<KeyValue> keyValues = new ArrayList<>();
    keyValues.add(keyValue("prod/apigw/a", "{}"));
    keyValues.add(keyValue("prod/apigw/b/b1", "\"invalid\""));
    keyValues.add(keyValue("prod/apigw/b/b2", "\"invalid\""));
    keyValueList.setList(keyValues);

    ArrayList<String> exclude = new ArrayList<>();
    exclude.add("b/.*");

    KeyValueList filtered = ConsulFolderConfigStore.filterOutExcludedPaths(keyValueList, "prod/apigw", exclude);
    Optional<JsonObject> entries = ConsulFolderConfigStore.mergeValuesForPath("prod/apigw", filtered);
    Assert.assertTrue(entries.isPresent());
    Assert.assertEquals("{}", entries.get().toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void mergeValuesForValidConsulResponseWithKeyEmptyAfterTrimmingAndValueIsNotJsonObjectTest() {
    KeyValueList keyValueList = givenKeyValueList("prod/apigw", "\"[{\\\"yoyo\\\": \\\"yoyoyoy\\\"}]\"");
    ConsulFolderConfigStore.mergeValuesForPath("prod/apigw", keyValueList);
  }

  @Test
  public void mergeValuesForEmptyConsulResponseTest() {
    KeyValueList d = new KeyValueList();

    Optional<JsonObject> entries = ConsulFolderConfigStore.mergeValuesForPath("prod/apigw", d);
    Assert.assertFalse(entries.isPresent());
  }
}