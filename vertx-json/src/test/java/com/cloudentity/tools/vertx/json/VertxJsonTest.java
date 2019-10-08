package com.cloudentity.tools.vertx.json;

import com.cloudentity.tools.vertx.configs.ConfigFactory;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

public class VertxJsonTest {

  @Before
  public void setUp() throws Exception {
    VertxJson.registerJsonObjectDeserializer();
    VertxJson.configureJsonMapper();
  }

  @Test
  public void deserializeJsonObjectField() throws Exception {
    String serializedConfig = "{\n" +
        "      \"name\": \"test\",\n" +
        "      \"config\": {\n" +
        "        \"int\": 1,\n" +
        "        \"string\": \"value\",\n" +
        "        \"list\": [1, 2, 3],\n" +
        "        \"map\": {\n" +
        "          \"key\": \"value\"\n" +
        "        }\n" +
        "      }\n" +
        "    }";

    JsonObject config = new JsonObject(serializedConfig);
    SamplePojo pojo = ConfigFactory.build(config, SamplePojo.class);

    assertNotNull(pojo);
    assertNotNull(pojo.getConfig());
    assertEquals("value", pojo.getConfig().getString("string"));
    assertEquals(new Integer(1), pojo.getConfig().getInteger("int"));
    assertEquals(Arrays.asList(1, 2, 3), pojo.getConfig().getJsonArray("list").getList());
    assertEquals(Collections.singletonMap("key", "value"), pojo.getConfig().getJsonObject("map").getMap());
  }

}
