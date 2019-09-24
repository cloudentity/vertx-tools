package com.cloudentity.tools.vertx.server.api.routes

import com.cloudentity.tools.vertx.scala.bus.ScalaServiceVerticle
import com.cloudentity.tools.vertx.server.api.tracing.RoutingWithTracing
import com.cloudentity.tools.vertx.tracing.TracingContext
import io.vertx.core.Future
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory

abstract class ScalaRouteVerticle extends ScalaServiceVerticle with RouteService {
  private val log = LoggerFactory.getLogger(this.getClass)

  override protected def vertxServiceAddressPrefixS: Option[String] =
    Option(verticleId) match {
      case Some(id) => Some(id)
      case None =>
        val errMsg = this.getClass + " does not have 'verticleId'"
        log.error(errMsg)
        throw new RuntimeException(errMsg)
    }

  override protected def vertxService: Class[_] = classOf[RouteService]

  override def handleRequest(ctx: RoutingContext): Future[_] = {
    handle(ctx)
    Future.succeededFuture[Void]
  }

  protected def handle(ctx: RoutingContext): Unit

  protected def getTracingContext(ctx: RoutingContext): TracingContext =
    RoutingWithTracing.getOrCreate(ctx, getTracing)

  implicit class RoutingContextWithTracingContext(ctx: RoutingContext) {
    val tracing: TracingContext = getTracingContext(ctx)
  }
}
