package com.cloudentity.tools.vertx.bus;

import com.cloudentity.tools.vertx.bus.ComponentVerticle.ConfigChanged;
import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

public class ConfigChangedTest {

  @Test
  public void hasChangedShouldReturnTrueOnSimpleAttrChange() {
    // given
    ConfigChanged configChanged = new ConfigChanged(new JsonObject().put("a", 100), new JsonObject().put("a", 200));

    // when then
    Assert.assertEquals(true, configChanged.hasChanged("a"));
  }

  @Test
  public void hasChangedShouldReturnTrueOnNestedAttrChange() {
    // given
    ConfigChanged configChanged = new ConfigChanged(new JsonObject().put("a", new JsonObject().put("x", 100)), new JsonObject().put("a", new JsonObject().put("x", 200)));

    // when then
    Assert.assertEquals(true, configChanged.hasChanged("a"));
    Assert.assertEquals(true, configChanged.hasChanged("a.x"));
  }

  @Test
  public void hasChangedShouldReturnFalseOnNonExistingAttr() {
    // given
    ConfigChanged configChanged = new ConfigChanged(new JsonObject().put("a", 100), new JsonObject().put("a", 200));

    // when then
    Assert.assertEquals(false, configChanged.hasChanged("b"));
  }

  @Test
  public void hasChangedShouldReturnOnNotChangedAttribute() {
    // given
    ConfigChanged configChanged = new ConfigChanged(new JsonObject().put("a", 100), new JsonObject().put("a", 100));

    // when then
    Assert.assertEquals(false, configChanged.hasChanged("a"));
  }

  @Test
  public void hasChangedShouldReturnTrueOnAddedAttribute() {
    // given
    ConfigChanged configChanged = new ConfigChanged(new JsonObject(), new JsonObject().put("a", 200));

    // when then
    Assert.assertEquals(true, configChanged.hasChanged("a"));
  }

  @Test
  public void hasChangedShouldReturnTrueOnRemovedAttribute() {
    // given
    ConfigChanged configChanged = new ConfigChanged(new JsonObject().put("a", 100), new JsonObject());

    // when then
    Assert.assertEquals(true, configChanged.hasChanged("a"));
  }
}
