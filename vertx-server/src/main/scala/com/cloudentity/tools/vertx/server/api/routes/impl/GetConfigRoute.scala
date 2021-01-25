package com.cloudentity.tools.vertx.server.api.routes.impl

import com.cloudentity.tools.vertx.conf.ConfService
import com.cloudentity.tools.vertx.json.JsonExtractor
import com.cloudentity.tools.vertx.server.api.ResponseHelpers
import com.cloudentity.tools.vertx.server.api.errors.ApiError
import com.cloudentity.tools.vertx.server.api.routes.ScalaRouteVerticle
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

/**
 * Returns json object containing masked global config or its sub-object if `path` query param provided.
 *
 * `path` should contain comma-separated path to the sub-object - if the path does not exist or the value at given
 * path is not a json object then 'null' string is returned.
 */
class GetConfigRoute extends ScalaRouteVerticle {
  val log = LoggerFactory.getLogger(this.getClass)
  var confService: ConfService = _

  override def initService(): Unit =
    confService = createClient(classOf[ConfService])

  override protected def handle(ctx: RoutingContext): Unit =
    confService.getMaskedGlobalConf().toScala.onComplete {
      case Success(conf) =>
        ctx.response().end(readConfig(ctx, conf))
      case Failure(ex) =>
        log.error("Could not get masked global conf", ex);
        ResponseHelpers.respondWithError(ctx, ApiError.`with`(500, "get-config-error", "Could not get configuration"))
    }

  private def readConfig(ctx: RoutingContext, globalConf: JsonObject): String =
    Option(ctx.request().getParam("path")) match {
      case Some(path) =>
        val objOpt = JsonExtractor.resolve(globalConf, path)
        if (objOpt.isPresent) objOpt.get().encodePrettily() else "null"
      case None =>
        globalConf.encodePrettily()
    }
}
