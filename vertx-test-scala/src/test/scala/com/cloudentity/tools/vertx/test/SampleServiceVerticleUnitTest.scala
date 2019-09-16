package com.cloudentity.tools.vertx.test

import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import org.junit.Test

class SampleServiceVerticleUnitTest extends ScalaServiceVerticleUnitTest[SampleServiceVerticle, SampleService] {

  @Test
  def testSampleVerticleMethodWithScalaFuture(ctx: TestContext): Unit = {
    val config = new JsonObject().put("length", 3)

    deployVerticle(config).toScala()
      .flatMap(_ => client.cut("cut-length-letters-from-this-string").toScala())
      .map { result =>
        ctx.assertEquals("cut", result)
      }.toJava().setHandler(ctx.asyncAssertSuccess())
  }

  @Test
  def testSampleVerticleMethodWithVertx(ctx: TestContext): Unit = {
    val config = new JsonObject().put("length", 3)

    deployVerticle(config)
      .compose(x => client.cut("cut-length-letters-from-this-string"))
      .compose(result => {
        ctx.assertEquals("cut", result)
        Future.succeededFuture(())
      }).setHandler(ctx.asyncAssertSuccess())
  }

  override def createVerticle(): SampleServiceVerticle = new SampleServiceVerticle
}
