package com.cloudentity.tools.vertx.server.api.filters

import java.util.Optional

import io.circe.{Decoder, Json}
import com.cloudentity.tools.vertx.bus.ServiceClientFactory
import com.cloudentity.tools.vertx.test.VertxUnitTest
import com.cloudentity.tools.vertx.verticles.VertxDeploy
import io.vertx.core.{DeploymentOptions, Future}
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.web.RoutingContext
import org.junit.Test
import com.cloudentity.tools.vertx.conf.fixed.FixedConfVerticle
import com.cloudentity.tools.vertx.scala.FutureConversions
import com.cloudentity.tools.vertx.scala.VertxExecutionContext

class StringConfigFilter(configValid: Boolean) extends ScalaRouteFilterVerticle[String] with RouteFilter {
  override def confDecoder: Decoder[String] = Decoder.decodeString

  override def filter(ctx: RoutingContext, conf: String): Unit = ctx.next()

  override def checkConfigValid(conf: String): RouteFilterConfigValidation =
    if (configValid) RouteFilterConfigValidation.success() else RouteFilterConfigValidation.failure("failure")
}

class ScalaRouteFilterTest extends VertxUnitTest with FutureConversions {
  implicit lazy val ec = VertxExecutionContext(vertx.getOrCreateContext())

  @Test
  def shouldDecodeAndValidateSuccessfullyConfiguration(ctx: TestContext): Unit = {
    deploy(new StringConfigFilter(true)).toScala()
      .flatMap{ client =>
        client.validateConfig("\"valid-config-string\"").toScala()
      }.map { result =>
        ctx.assertTrue(result.isSuccess)
      }.toJava().setHandler(ctx.asyncAssertSuccess())
  }

  @Test
  def shouldFailDecodingConfigurationWhenStringNotWrappedInQuotes(ctx: TestContext): Unit = {
    deploy(new StringConfigFilter(false)).toScala()
      .flatMap{ client =>
        client.validateConfig("invalid-config").toScala()
      }.map { result =>
        ctx.assertFalse(result.isSuccess)
      }.toJava.setHandler(ctx.asyncAssertSuccess())
  }

  @Test
  def shouldDecodeAndInValidateConfiguration(ctx: TestContext): Unit = {
    deploy(new StringConfigFilter(false)).toScala()
      .flatMap{ client =>
        client.validateConfig("\"valid-config-string\"").toScala()
      }.map { result =>
        ctx.assertTrue(result.isSuccess)
      }.toJava.setHandler(ctx.asyncAssertSuccess())
  }

  @Test
  def shouldFailDecodingConfigurationWhenDifferentJsonType(ctx: TestContext): Unit = {
    deploy(new StringConfigFilter(false)).toScala()
      .flatMap{ client =>
        client.validateConfig("true").toScala()
      }.map { result =>
        ctx.assertFalse(result.isSuccess)
      }.toJava.setHandler(ctx.asyncAssertSuccess())
  }

  def deploy(filter: ScalaRouteFilterVerticle[_]): Future[RouteFilter] =
    FixedConfVerticle.deploy(vertx, new JsonObject())
      .compose { _ =>
        VertxDeploy.deploy(vertx, new StringConfigFilter(true), new DeploymentOptions().setConfig(new JsonObject().put("verticleId", "test")))
      }.compose { _ =>
        Future.succeededFuture(ServiceClientFactory.make(vertx.eventBus(), classOf[RouteFilter], Optional.of("test")))
      }
}
