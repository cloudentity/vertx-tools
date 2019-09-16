package com.cloudentity.tools.vertx.test;

import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;

import java.util.HashMap;

public class SampleVerticleIntegrationTest extends ServiceVerticleIntegrationTest {

  @Test
  public void verticlesIntegrationTest(TestContext ctx) throws Exception {
    JsonObject globalConfig =
      new JsonObject()
        .put("cutter", new JsonObject().put("length", 3))
        .put("pinger", new JsonObject());

    Future<String> deployment = deployVerticles(globalConfig,
      verticle("cutter", new SampleServiceVerticle()),
      verticle("pinger", new OtherSampleServiceVerticle())
    );

    deployment
      .compose(x -> client(SampleService.class).cut("cut-length-letters-from-this-string"))
      .compose(result -> {
        ctx.assertEquals("cut", result);
        return Future.succeededFuture();
      })
      .compose(x -> client(OtherSampleService.class).ping("message"))
      .compose(result -> {
        ctx.assertEquals("message", result);
        return Future.succeededFuture();
      })
      .setHandler(ctx.asyncAssertSuccess());


    HashMap<String, Verticle> verticles = new HashMap<>();
    verticles.put("cutter", new SampleServiceVerticle());
    verticles.put("pinger", new OtherSampleServiceVerticle());

    deployment = deployVerticles(globalConfig, verticles);

    deployment
      .compose(x -> client(SampleService.class).cut("cut-length-letters-from-this-string"))
      .compose(result -> {
        ctx.assertEquals("cut", result);
        return Future.succeededFuture();
      })
      .compose(x -> client(OtherSampleService.class).ping("message"))
      .compose(result -> {
        ctx.assertEquals("message", result);
        return Future.succeededFuture();
      })
      .setHandler(ctx.asyncAssertSuccess());
  }


  public JsonObject defaultConfig() {
    return new JsonObject()
      .put("cutter", new JsonObject().put("length", 3))
      .put("pinger", new JsonObject());
  }
}