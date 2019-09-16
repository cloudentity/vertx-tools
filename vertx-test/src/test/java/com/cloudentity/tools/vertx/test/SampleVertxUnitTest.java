package com.cloudentity.tools.vertx.test;

import com.cloudentity.tools.vertx.bus.ServiceClientFactory;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;

public class SampleVertxUnitTest extends VertxUnitTest {

  @Test
  public void testSampleVerticleMethod(TestContext ctx) throws Exception {
    SampleService client = ServiceClientFactory.make(vertx().eventBus(), SampleService.class);

    Verticle verticle = new SampleServiceVerticle();
    JsonObject config = new JsonObject().put("length", 3);

    VertxDeployTest.deployWithConfig(vertx(), verticle, config)
        .compose(x -> client.cut("cut-length-letters-from-this-string"))
        .compose(result -> {
          ctx.assertEquals("cut", result);
          return Future.succeededFuture();
        })
        .setHandler(ctx.asyncAssertSuccess());
  }

}
