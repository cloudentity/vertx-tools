package com.cloudentity.tools.vertx.conf;

import io.vertx.core.json.JsonObject;
import org.junit.Test;

public class ConfPrinterTest {
  @Test
  public void testLayout() {
    JsonObject conf =
      new JsonObject()
        .put("w", "$env:W:string")
        .put("x", "$env:X:string:x")
        .put("y", "$env:Y:string")
        .put("z", "$env:?Z:string");

    ConfPrinter.logEnvVariables(conf);
  }
}
