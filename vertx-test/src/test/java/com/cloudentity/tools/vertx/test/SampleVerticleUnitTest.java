package com.cloudentity.tools.vertx.test;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;

public class SampleVerticleUnitTest extends ServiceVerticleUnitTest<SampleServiceVerticle, SampleService> {

  @Test
  public void testSampleVerticleMethod(TestContext ctx) throws Exception {
    JsonObject config = new JsonObject().put("length", 3);

    deployVerticle(config)
      .compose(x -> client().cut("cut-length-letters-from-this-string"))
      .compose(result -> {
        ctx.assertEquals("cut", result);
        return Future.succeededFuture();
      })
      .setHandler(ctx.asyncAssertSuccess());
  }

  @Override
  protected SampleServiceVerticle createVerticle() {
    return new SampleServiceVerticle();
  }
}
