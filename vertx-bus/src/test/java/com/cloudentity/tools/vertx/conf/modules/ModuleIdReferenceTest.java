package com.cloudentity.tools.vertx.conf.modules;

import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class ModuleIdReferenceTest {
  @Test
  public void shouldReplaceModuleIdInValue() {
    // given
    JsonObject conf = new JsonObject().put("x", "{MODULE_ID-}x");

    // when
    ModuleIdReference.populateModuleIdRefs(conf, Optional.of("abc"));

    // then
    assertEquals("abc-x", conf.getValue("x"));
  }

  @Test
  public void shouldReplaceModuleIdInKey() {
    // given
    JsonObject conf = new JsonObject().put("{MODULE_ID-}x","x");

    // when
    ModuleIdReference.populateModuleIdRefs(conf, Optional.of("abc"));

    // then
    assertEquals("x", conf.getValue("abc-x"));
  }

  @Test
  public void shouldReplaceModuleIdInValueWithEmptyStringIfMissing() {
    // given
    JsonObject conf = new JsonObject().put("x", "{MODULE_ID-}x");

    // when
    ModuleIdReference.populateModuleIdRefs(conf, Optional.empty());

    // then
    assertEquals("x", conf.getValue("x"));
  }

  @Test
  public void shouldReplaceModuleIdInKeyWithEmptyStringIfMissing() {
    // given
    JsonObject conf = new JsonObject().put("{MODULE_ID-}x","x");

    // when
    ModuleIdReference.populateModuleIdRefs(conf, Optional.empty());

    // then
    assertEquals("x", conf.getValue("x"));
  }
}
