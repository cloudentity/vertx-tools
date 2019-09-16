package com.cloudentity.tools.vertx.scala.bus

import com.cloudentity.tools.vertx.bus.VertxBus
import com.cloudentity.tools.vertx.conf.fixed.FixedConfVerticle
import com.cloudentity.tools.vertx.verticles.VertxDeploy
import io.vertx.core.json.JsonObject
import io.vertx.core.{Verticle, Future => VxFuture}
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.{RunTestOnContext, VertxUnitRunner}
import org.junit.runner.RunWith
import org.junit.{Before, Rule, Test}

import scala.concurrent.Future

/**
  * Test for ScalaVerticleTemplate initialization logic.
  */
@RunWith(classOf[VertxUnitRunner])
class ScalaVerticleTemplateTest {
  val r: RunTestOnContext = new RunTestOnContext

  @Rule def rule: RunTestOnContext = r

  @Before
  def setup(): Unit = {
    VertxBus.registerPayloadCodec(rule.vertx().eventBus())
  }

  @Test
  def shouldDeployComponentWithInit(ctx: TestContext): Unit = {
    val verticle = new ComponentWithInit
    deploy(ctx, verticle)
      .map { (_: String) =>
        ctx.assertEquals("value", verticle.initValue)
      }.setHandler(ctx.asyncAssertSuccess())
  }

  @Test
  def shouldDeployComponentWithInitFuture(ctx: TestContext): Unit = {
    val verticle = new ComponentWithInitFuture
    deploy(ctx, verticle)
      .map { (_: String) =>
        ctx.assertEquals("value", verticle.initFutureValue)
      }.setHandler(ctx.asyncAssertSuccess())
  }

  @Test
  def shouldDeployComponentWithInitAndInitFuture(ctx: TestContext): Unit = {
    val verticle = new ComponentWithInitAndInitFuture
    deploy(ctx, verticle)
      .map { (_: String) =>
        ctx.assertEquals("value", verticle.initFutureValue)
        ctx.assertEquals("value", verticle.initValue)
      }.setHandler(ctx.asyncAssertSuccess())
  }

  def deploy(ctx: TestContext, verticle: Verticle): VxFuture[String] = {
    FixedConfVerticle.deploy(rule.vertx(), "verticleId", new JsonObject().put("key", "value"))
      .compose(_ => VertxDeploy.deploy(rule.vertx(), verticle))
  }
}

class ComponentWithInit extends ScalaComponentVerticle {
  var initValue: String = ""

  override def initComponent(): Unit = {
    initValue = getConfig.getString("key")
  }

  override def verticleId(): String = "verticleId"
}

class ComponentWithInitFuture extends ScalaComponentVerticle {
  var initFutureValue: String = ""

  override def initComponentAsyncS(): Future[Unit] = {
    initFutureValue = getConfig.getString("key")
    Future.successful(())
  }

  override def verticleId(): String = "verticleId"
}

class ComponentWithInitAndInitFuture extends ScalaComponentVerticle {
  var initValue: String = ""
  var initFutureValue: String = ""

  override def initComponent(): Unit = {
    initValue = getConfig.getString("key")
  }

  override def initComponentAsyncS(): Future[Unit] = {
    initFutureValue = getConfig.getString("key")
    Future.successful(())
  }

  override def verticleId(): String = "verticleId"
}