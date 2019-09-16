package com.cloudentity.tools.vertx.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class JsonObjectDeserializer extends StdDeserializer<JsonObject> {

  private static final Logger log = LoggerFactory.getLogger(JsonObjectDeserializer.class);

  public JsonObjectDeserializer() {
    this(null);
  }

  public JsonObjectDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public JsonObject deserialize(JsonParser parser, DeserializationContext content) throws IOException {
    JsonObject obj = new JsonObject();
    TreeNode treeNode = parser.getCodec().readTree(parser);
    if (treeNode.isObject()) {
      treeNode.fieldNames().forEachRemaining(field -> {
        // for some reason this try catch must be here, it's not possible to throw JsonProcessingException from method
        try {
          obj.put(field, parser.getCodec().treeToValue(treeNode.get(field), Object.class));
        } catch (JsonProcessingException e) {
          log.error("Json processing exception: {}", e.getCause());
          log.debug("{}", e);
        }
      });
    }
    return obj;
  }

}
