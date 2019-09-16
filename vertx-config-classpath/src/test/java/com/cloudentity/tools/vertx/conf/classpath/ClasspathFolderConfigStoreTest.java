package com.cloudentity.tools.vertx.conf.classpath;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.ArrayList;

import static com.google.common.collect.Lists.newArrayList;
import static io.vertx.core.impl.launcher.commands.FileSelector.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClasspathFolderConfigStoreTest {

  @Test
  public void verifyFileselectorMatcher() {
    assertTrue(match("*json", "some/test/a.json"));
    assertTrue(match("*.json", "some/test/a.json"));
    assertTrue(match("some/test/*.json", "some/test/a.json"));
    assertTrue(match("some/**/*.json", "some/test/a.json"));
    assertTrue(match("**/test/*.json", "some/test/a.json"));

    assertFalse(match("*json", "some/test/a.class"));
    assertFalse(match("test/*.json", "some/test/a.json"));
  }

  @Test
  public void shouldLoadAllFilesIfFilesetMatchesEverything() {
    new ClasspathFolderConfigStore(null,
        config("some", newArrayList("*"), "some.output.path"))
    .get(assertionHandler("{\"some\":{\"output\":{\"path\":{\"b\":[\"a\",\"b\"],\"a\":{\"x\":\"y\"}}}}}"));
  }

  @Test
  public void shouldLoadAllJsonFiles() {
    new ClasspathFolderConfigStore(null,
        config("some", newArrayList("*json"), "some.output.path"))
        .get(assertionHandler("{\"some\":{\"output\":{\"path\":{\"b\":[\"a\",\"b\"],\"a\":{\"x\":\"y\"}}}}}"));
  }

  @Test
  public void shouldLoadAllJsonFilesFromAllFolders() {
    new ClasspathFolderConfigStore(null,
        config("", newArrayList("*json"), "some.output.path"))
        .get(assertionHandler("{\"some\":{\"output\":{\"path\":{\"c\":{\"q\":\"z\"},\"b\":[\"a\",\"b\"],\"a\":{\"x\":\"y\"}}}}}"));
  }

  private Handler<AsyncResult<Buffer>> assertionHandler(String expectedJson) {
    return event -> {
      assertTrue(event.succeeded());
      assertEquals(new JsonObject(expectedJson), new JsonObject(event.result()));
    };
  }

  private JsonObject config(String path, ArrayList<String> patterns, String outputPath) {
    JsonObject config = new JsonObject().put("path", path);
    JsonArray filesets = new JsonArray();
    config.put("filesets", filesets);
    patterns.stream().forEach(p -> filesets.add(new JsonObject().put("pattern", p)));
    config.put("outputPath", outputPath);
    return config;
  }

}
