package com.cloudentity.tools.vertx.sd

import com.cloudentity.tools.vertx.test.VertxModuleTest
import io.vertx.core.json.{JsonArray, JsonObject}
import io.vertx.ext.unit.TestContext
import org.junit.Test

class FixedSdProviderTest extends VertxModuleTest {
  @Test
  def shouldDeploySuccessfully(ctx: TestContext): Unit = {
    val records =
      new JsonObject("""{
       |  "sd-records": [
       |    {
       |      "name": "service-a",
       |      "location": {
       |        "host": "localhost",
       |        "port": 8443,
       |        "ssl": true
       |      }
       |    }
       |  ]
       |}""".stripMargin)

    deployModule("sd-provider/static", records, "system").onComplete(ctx.asyncAssertSuccess())
  }
}
