package com.cloudentity.tools.vertx.conf.modules;

import com.google.common.collect.Lists;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class TreeModuleDefsCollectorTest {
  @Test
  public void shouldCollectSingleModuleDefWithoutPathId() {
    // given
    JsonObject globalConfig =
      new JsonObject().put("apiGroups", new JsonObject().put("_modules", new JsonObject().put("id", "a")));
    JsonObject treeModuleDef = new JsonObject().put("collect", "tree").put("path", "apiGroups").put("key", "_modules");

    // when
    List<String> moduleIds = TreeModuleDefsCollector.collect(globalConfig, treeModuleDef).stream().map(m -> m.getString("id")).collect(Collectors.toList());

    // then
    assertEquals(Lists.newArrayList("a"), moduleIds);
  }

  @Test
  public void shouldCollectSingleModuleDefWitPathId() {
    // given
    JsonObject globalConfig =
      new JsonObject().put("apiGroups", new JsonObject().put("_modules", new JsonObject().put("id", "a")));
    JsonObject treeModuleDef = new JsonObject().put("collect", "tree").put("path", "apiGroups").put("key", "_modules").put("idWithPath", true);

    // when
    List<String> moduleIds = TreeModuleDefsCollector.collect(globalConfig, treeModuleDef).stream().map(m -> m.getString("id")).collect(Collectors.toList());

    // then
    assertEquals(Lists.newArrayList("apiGroups-a"), moduleIds);
  }

  @Test
  public void shouldCollectModuleDefArrayWithoutPathId() {
    // given
    JsonObject globalConfig =
      new JsonObject().put("apiGroups", new JsonObject().put("_modules", new JsonArray().add(new JsonObject().put("id", "a")).add(new JsonObject().put("id", "b"))));
    JsonObject treeModuleDef = new JsonObject().put("collect", "tree").put("path", "apiGroups").put("key", "_modules");

    // when
    List<String> moduleIds = TreeModuleDefsCollector.collect(globalConfig, treeModuleDef).stream().map(m -> m.getString("id")).collect(Collectors.toList());

    // then
    assertEquals(Lists.newArrayList("a", "b"), moduleIds);
  }

  @Test
  public void shouldCollectModuleDefArrayWitPathId() {
    // given
    JsonObject globalConfig =
      new JsonObject().put("apiGroups", new JsonObject().put("_modules", new JsonArray().add(new JsonObject().put("id", "a")).add(new JsonObject().put("id", "b"))));
    JsonObject treeModuleDef = new JsonObject().put("collect", "tree").put("path", "apiGroups").put("key", "_modules").put("idWithPath", true);

    // when
    List<String> moduleIds = TreeModuleDefsCollector.collect(globalConfig, treeModuleDef).stream().map(m -> m.getString("id")).collect(Collectors.toList());

    // then
    assertEquals(Lists.newArrayList("apiGroups-a", "apiGroups-b"), moduleIds);
  }

  @Test
  public void shouldCollectModuleDefSimpleTreeWithoutPathId() {
    // given
    JsonObject globalConfig =
      new JsonObject().put("apiGroups",
        new JsonObject()
          .put("x", new JsonObject().put("_modules", new JsonArray().add(new JsonObject().put("id", "a"))))
          .put("y", new JsonObject().put("_modules", new JsonArray().add(new JsonObject().put("id", "b"))))
      );
    JsonObject treeModuleDef = new JsonObject().put("collect", "tree").put("path", "apiGroups").put("key", "_modules");

    // when
    List<String> moduleIds = TreeModuleDefsCollector.collect(globalConfig, treeModuleDef).stream().map(m -> m.getString("id")).collect(Collectors.toList());

    // then
    assertEquals(Lists.newArrayList("a", "b"), moduleIds);
  }

  @Test
  public void shouldCollectModuleDefSimpleTreeWitPathId() {
    // given
    JsonObject globalConfig =
      new JsonObject().put("apiGroups",
        new JsonObject()
          .put("x", new JsonObject().put("_modules", new JsonArray().add(new JsonObject().put("id", "a"))))
          .put("y", new JsonObject().put("_modules", new JsonArray().add(new JsonObject().put("id", "b"))))
      );
    JsonObject treeModuleDef = new JsonObject().put("collect", "tree").put("path", "apiGroups").put("key", "_modules").put("idWithPath", true);

    // when
    List<String> moduleIds = TreeModuleDefsCollector.collect(globalConfig, treeModuleDef).stream().map(m -> m.getString("id")).collect(Collectors.toList());

    // then
    assertEquals(Lists.newArrayList("apiGroups-x-a", "apiGroups-y-b"), moduleIds);
  }

  @Test
  public void shouldCollectModuleDefNestedTreeWithoutPathId() {
    // given
    JsonObject globalConfig =
      new JsonObject().put("apiGroups",
        new JsonObject()
          .put("x", new JsonObject().put("xx",
            new JsonObject()
              .put("_modules", new JsonArray().add(new JsonObject().put("id", "a")))
              .put("xxx", new JsonObject().put("_modules", new JsonArray().add(new JsonObject().put("id", "c"))))
            )
          )
          .put("y", new JsonObject().put("yy",
              new JsonObject()
                .put("_modules", new JsonArray().add(new JsonObject().put("id", "b")))
            )
          )
      );
    JsonObject treeModuleDef = new JsonObject().put("collect", "tree").put("path", "apiGroups").put("key", "_modules");

    // when
    List<String> moduleIds = TreeModuleDefsCollector.collect(globalConfig, treeModuleDef).stream().map(m -> m.getString("id")).collect(Collectors.toList());

    // then
    assertEquals(Lists.newArrayList("a", "c", "b"), moduleIds);
  }

  @Test
  public void shouldCollectModuleDefNestedTreeWithPathId() {
    // given
    JsonObject globalConfig =
      new JsonObject().put("apiGroups",
        new JsonObject()
          .put("x", new JsonObject().put("xx",
            new JsonObject()
              .put("_modules", new JsonArray().add(new JsonObject().put("id", "a")))
              .put("xxx", new JsonObject().put("_modules", new JsonArray().add(new JsonObject().put("id", "c"))))
            )
          )
          .put("y", new JsonObject().put("yy",
            new JsonObject()
              .put("_modules", new JsonArray().add(new JsonObject().put("id", "b")))
            )
          )
      );
    JsonObject treeModuleDef = new JsonObject().put("collect", "tree").put("path", "apiGroups").put("key", "_modules").put("idWithPath", true);

    // when
    List<String> moduleIds = TreeModuleDefsCollector.collect(globalConfig, treeModuleDef).stream().map(m -> m.getString("id")).collect(Collectors.toList());

    // then
    assertEquals(Lists.newArrayList("apiGroups-x-xx-a", "apiGroups-x-xx-xxx-c", "apiGroups-y-yy-b"), moduleIds);
  }
}
