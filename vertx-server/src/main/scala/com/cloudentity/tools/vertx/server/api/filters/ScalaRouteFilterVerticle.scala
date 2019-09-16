package com.cloudentity.tools.vertx.server.api.filters

import io.circe.Decoder
import io.circe.parser._
import com.cloudentity.tools.vertx.scala.bus.ScalaServiceVerticle
import com.cloudentity.tools.vertx.server.api.tracing.RoutingWithTracing
import com.cloudentity.tools.vertx.tracing.TracingContext
import io.vertx.core.Future
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory

abstract class ScalaRouteFilterVerticle[C](implicit m: Manifest[C]) extends ScalaServiceVerticle with RouteFilter {
  private val log = LoggerFactory.getLogger(this.getClass)

  var cache: Map[String, C] = Map()

  def confDecoder: Decoder[C]
  def filter(ctx: RoutingContext, conf: C): Unit
  def checkConfigValid(conf: C): RouteFilterConfigValidation

  override protected def vertxServiceAddressPrefixS: Option[String] =
    Option(verticleId) match {
      case Some(id) => Some(id)
      case None =>
        val errMsg = this.getClass + " does not have 'verticleId'"
        log.error(errMsg)
        throw new RuntimeException(errMsg)
    }

  override def applyFilter(ctx: RoutingContext, rawJsonConf: String): Future[Void] = {
    cache.get(rawJsonConf) match {
      case Some(conf) =>
        filter(ctx, conf)
      case None =>
        decode[C](rawJsonConf)(confDecoder) match {
          case Right(conf) => filter(ctx, conf)
          case Left(error) => ctx.fail(error)
        }
    }

    Future.succeededFuture(null)
  }

  override def validateConfig(rawJsonConf: String): Future[RouteFilterConfigValidation] = {
    val result =
      decode[C](rawJsonConf)(confDecoder) match {
        case Right(conf) =>
          checkConfigValid(conf)
        case Left(error) =>
          val errMsg = s"Could not decode configuration '$rawJsonConf' of ${this.getClass.getName}. Reason: ${error}"
          RouteFilterConfigValidation.failure(errMsg)
      }

    Future.succeededFuture(result)
  }

  protected def getTracingContext(ctx: RoutingContext): TracingContext =
    RoutingWithTracing.getOrCreate(ctx, getTracing)

  implicit class RoutingContextWithTracingContext(ctx: RoutingContext) {
    val tracing: TracingContext = getTracingContext(ctx)
  }

}
