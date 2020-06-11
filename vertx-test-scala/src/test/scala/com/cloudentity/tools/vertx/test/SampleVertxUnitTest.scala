package com.cloudentity.tools.vertx.test

import com.cloudentity.tools.vertx.bus.{ServiceVerticle, VertxEndpoint, VertxEndpointClient}
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import org.junit.Test
import org.slf4j.LoggerFactory

class SampleVertxUnitTest extends ScalaVertxUnitTest {

  trait SampleService {
    @VertxEndpoint(address = "cut")
    def cut(param: String): Future[String]
  }

  class SampleServiceVerticle extends ServiceVerticle with SampleService {
    val log = LoggerFactory.getLogger(this.getClass)

    def cut(param: String): Future[String] = {
      log.debug(s"config: $getConfig")
      val length = getConfig.getInteger("length")
      log.debug(s"Substring: $param, length: $length")

      Future.succeededFuture(param.substring(0, length))
    }
  }

  @Test
  def testSampleVerticleMethodWithScalaFuture(ctx: TestContext): Unit = {
    val client = VertxEndpointClient.make(vertx, classOf[SampleService])
    val verticle = new SampleServiceVerticle
    val config = new JsonObject().put("length", 3)

    VertxDeployTest.deployWithConfig(vertx, verticle, config).toScala()
      .flatMap(_ => client.cut("cut-length-letters-from-this-string").toScala())
      .map { result =>
        ctx.assertEquals("cut", result)
      }.toJava().setHandler(ctx.asyncAssertSuccess())
  }

  @Test
  def testSampleVerticleMethodWithVertx(ctx: TestContext): Unit = {
    val client = VertxEndpointClient.make(vertx, classOf[SampleService])
    val verticle = new SampleServiceVerticle
    val config = new JsonObject().put("length", 3)

    val async = ctx.async
    VertxDeployTest.deployWithConfig(vertx, verticle, config)
      .compose(x => client.cut("cut-length-letters-from-this-string"))
      .compose(result => {
        ctx.assertEquals("cut", result)
        async.complete()
        Future.succeededFuture(())
      }).setHandler(ctx.asyncAssertSuccess())
  }
}
