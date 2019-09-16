package com.cloudentity.services.authz.app.validators;

import com.cloudentity.tools.vertx.configs.JsonObjectExtender;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class JsonObjectExtenderTest {

  private JsonObjectExtender extender = new JsonObjectExtender();

  @Test
  public void itModifiesRootLevelProperty() {
    JsonObject original = new JsonObject("{\"property\": \"value\"}");
    String extension = "property=\"newvalue\"";
    JsonObject extended = extender.extendConfig(original, singletonList(extension));
    JsonObject expected = new JsonObject("{\"property\": \"newvalue\"}");
    assertEquals(expected, extended);
  }

  @Test
  public void itModifiesNestedProperty() {
    JsonObject original = new JsonObject("{\"root\":{\"first\":{\"second\":\"value\"}}}");
    String extension = "root.first.second=\"newvalue\"";
    JsonObject extended = extender.extendConfig(original, singletonList(extension));
    JsonObject expected = new JsonObject("{\"root\":{\"first\":{\"second\":\"newvalue\"}}}");
    assertEquals(expected, extended);
  }

  @Test
  public void itModifiesNestedPositiveBooleanPropertyToNegative() {
    JsonObject original = new JsonObject("{\"root\":{\"first\":{\"second\":true}}}");
    String extension = "root.first.second=false";
    JsonObject extended = extender.extendConfig(original, singletonList(extension));
    JsonObject expected = new JsonObject("{\"root\":{\"first\":{\"second\":false}}}");
    assertEquals(expected, extended);
  }

  @Test
  public void itModifiesNestedNegativeBooleanPropertyToPositive() {
    JsonObject original = new JsonObject("{\"root\":{\"first\":{\"second\":false}}}");
    String extension = "root.first.second=true";
    JsonObject extended = extender.extendConfig(original, singletonList(extension));
    JsonObject expected = new JsonObject("{\"root\":{\"first\":{\"second\":true}}}");
    assertEquals(expected, extended);
  }

  @Test
  public void itModifiesNestedNumberProperty() {
    JsonObject original = new JsonObject("{\"root\":{\"first\":{\"second\":9999}}}");
    String extension = "root.first.second=1000";
    JsonObject extended = extender.extendConfig(original, singletonList(extension));
    JsonObject expected = new JsonObject("{\"root\":{\"first\":{\"second\":1000}}}");
    assertEquals(expected, extended);
  }

  @Test
  public void itModifiesNestedNumberPropertyToNegativeNumber() {
    JsonObject original = new JsonObject("{\"root\":{\"first\":{\"second\":9999}}}");
    String extension = "root.first.second=-1000";
    JsonObject extended = extender.extendConfig(original, singletonList(extension));
    JsonObject expected = new JsonObject("{\"root\":{\"first\":{\"second\":-1000}}}");
    assertEquals(expected, extended);
  }

  @Test
  public void itModifiesNestedNumberPropertyToExceededInteger() {
    JsonObject original = new JsonObject("{\"root\":{\"first\":{\"second\":2147483647}}}");
    String extension = "root.first.second=3147483647";
    JsonObject extended = extender.extendConfig(original, singletonList(extension));
    JsonObject expected = new JsonObject("{\"root\":{\"first\":{\"second\":3147483647}}}");
    assertEquals(expected, extended);
  }

  @Test
  public void itModifiesNestedNumberPropertyToFloatingPointStringNumber() {
    JsonObject original = new JsonObject("{\"root\":{\"first\":{\"second\":9999}}}");
    String extension = "root.first.second=99.99";
    JsonObject extended = extender.extendConfig(original, singletonList(extension));
    JsonObject expected = new JsonObject("{\"root\":{\"first\":{\"second\":\"99.99\"}}}");
    assertEquals(expected, extended);
  }


  @Test
  public void itSkipsExtensionWhenThePathPointsToALeafAndItDoesNotExistInOriginalEndpoint() {
    JsonObject original = new JsonObject("{\"root\":{\"first\":{\"second\":9999}}}");
    String extension = "root.first.third=100";
    JsonObject extended = extender.extendConfig(original, singletonList(extension));
    JsonObject expected = new JsonObject("{\"root\":{\"first\":{\"second\":9999}}}");
    assertEquals(expected, extended);
  }


  @Test
  public void itSkipsExtensionWhenThePathPointsToANonLeafNodeAndItDoesNotExistInOriginalEndpoint() {
    JsonObject original = new JsonObject("{\"root\":{\"first\":{\"second\":9999}}}");
    String extension = "root.notexisting.second=100";
    JsonObject extended = extender.extendConfig(original, singletonList(extension));
    JsonObject expected = new JsonObject("{\"root\":{\"first\":{\"second\":9999}}}");
    assertEquals(expected, extended);
  }

  @Test
  public void itSkipsExtensionWhenThePathPointsToANonLeafNodeWithAtLeast2NodesBelowAndItDoesNotExistInOriginalEndpoint() {
    JsonObject original = new JsonObject("{\"root\":{\"first\":{\"second\":{\"third\":{\"fourth\":{\"fifth\":{\"sixth\":9999}}}}}}}");
    String extension = "root.first.second.somerandomstring.third.fourth.fifth.sixth=100";
    JsonObject extended = extender.extendConfig(original, singletonList(extension));
    JsonObject expected = new JsonObject("{\"root\":{\"first\":{\"second\":{\"third\":{\"fourth\":{\"fifth\":{\"sixth\":9999}}}}}}}");
    assertEquals(expected, extended);
  }

  @Test
  public void itOverwritesNodeValueWhenItsCurrentValueIsAJsonObject() {
    JsonObject original = new JsonObject("{\"root\":{\"first\":{\"second\":{\"third\":{\"fourth\":{\"fifth\":{\"sixth\":9999}}}}}}}");
    String extension = "root.first.second=100";
    JsonObject extended = extender.extendConfig(original, singletonList(extension));
    JsonObject expected = new JsonObject("{\"root\":{\"first\":{\"second\":100}}}");
    assertEquals(expected, extended);
  }


  @Test
  public void itOverwritesAValueWithJsonObject() {
    JsonObject original = new JsonObject("{\"root\":{\"first\":{\"second\":{}}}}");
    String extension = "root.first.second={\"x\":\"y\"}";
    JsonObject extended = extender.extendConfig(original, singletonList(extension));
    JsonObject expected = new JsonObject("{\"root\":{\"first\":{\"second\":{\"x\":\"y\"}}}}");
    assertEquals(expected, extended);
  }

  @Test
  public void itOverwritesAValueWithJsonArray() {
    JsonObject original = new JsonObject("{\"root\":{\"first\":{\"second\":{}}}}");
    String extension = "root.first.second=[\"a\", \"b\"]";
    JsonObject extended = extender.extendConfig(original, singletonList(extension));
    JsonObject expected = new JsonObject("{\"root\":{\"first\":{\"second\":[\"a\", \"b\"]}}}");
    assertEquals(expected, extended);
  }


  @Test
  public void itSkipsExtensionWhenINvalidJsonLikeValueIsProvided() {
    JsonObject original = new JsonObject("{\"root\":{\"first\":{\"second\":{}}}}");
    String extension = "root.first.second={x:\"y\"}";
    JsonObject extended = extender.extendConfig(original, singletonList(extension));
    JsonObject expected = new JsonObject("{\"root\":{\"first\":{\"second\":{}}}}");
    assertEquals(expected, extended);
  }

}