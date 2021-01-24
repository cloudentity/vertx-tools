package com.cloudentity.tools.vertx.server.api.routes.impl

import com.cloudentity.tools.vertx.conf.ConfService
import com.cloudentity.tools.vertx.server.api.ResponseHelpers
import com.cloudentity.tools.vertx.server.api.errors.ApiError
import com.cloudentity.tools.vertx.server.api.routes.ScalaRouteVerticle
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

class GetConfigRoute extends ScalaRouteVerticle {
  val log = LoggerFactory.getLogger(this.getClass)
  var confService: ConfService = _

  override def initService(): Unit =
    confService = createClient(classOf[ConfService])

  override protected def handle(ctx: RoutingContext): Unit =
    confService.getMaskedGlobalConf().toScala.onComplete {
      case Success(conf) =>
        ctx.response().end(conf.encodePrettily())
      case Failure(ex) =>
        log.error("Could not get masked global conf", ex);
        ResponseHelpers.respondWithError(ctx, ApiError.`with`(500, "get-config-error", "Could not get configuration"))
    }
}
