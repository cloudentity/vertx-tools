package com.cloudentity.tools.vertx.server.api

import java.util.Optional

import com.cloudentity.tools.vertx.bus.VertxEndpointClient
import com.cloudentity.tools.vertx.http.HttpService
import com.cloudentity.tools.vertx.scala.bus.ScalaServiceVerticle
import com.cloudentity.tools.vertx.scala.VertxExecutionContext
import com.cloudentity.tools.vertx.server.api.conf.{ApiServerConf, RouteConf}
import com.cloudentity.tools.vertx.server.api.filters.RouteFilter
import com.cloudentity.tools.vertx.server.api.routes.RouteService
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.{Route, Router}
import org.slf4j.LoggerFactory
import io.vertx.core.{Future => VxFuture}

import scala.concurrent.Future

class ApiServer(serverConf: ApiServerConf) extends ScalaServiceVerticle with HttpService {
  val log = LoggerFactory.getLogger(this.getClass)
  var actualPort: Int = _

  implicit lazy val ec = VertxExecutionContext(vertx.getOrCreateContext())

  override def initServiceAsync(): VxFuture[Void] = {
    val allRouteConfs = serverConf.prependRoutes.getOrElse(Nil) ::: serverConf.routes.value ::: serverConf.appendRoutes.getOrElse(Nil)
    val activeRouteConfs = allRouteConfs.filterNot(route => serverConf.disabledRoutes.getOrElse(Nil).contains(route.id))

    val createServer: Future[(List[(RouteConf, RouteService)], Router)] =
      for {
        routeVerticleClients <- Future.successful(activeRouteConfs.map(route => (route, makeRouteServiceClient(route.handler.getOrElse(route.id.value)))))
        router                = Router.router(vertx)
        server               <- asFuture[HttpServer](h => vertx.createHttpServer(serverConf.http).requestHandler(router.accept _).listen(h)).toScala()
        _                     = actualPort = server.actualPort()
        _                     = log.info(s"Started HTTP server on port $actualPort")
      } yield (routeVerticleClients, router)

    val registerHandlers = createServer.map { case (deployedApiVerticles, router) =>
      deployedApiVerticles.foreach {
        case (routeConf, client) =>
          val routeConfWithBasePath = routeConf.copy(urlPath = serverConf.basePath.map(_ + routeConf.urlPath).getOrElse(routeConf.urlPath))
          registerRouteHandler(router, routeConfWithBasePath, client)
      }
    }

    registerHandlers.failed.map { ex =>
      log.error("Error on creating HTTP server", ex)
    }

    registerHandlers.map[Void](_ => null).toJava()
  }

  private def makeRouteServiceClient(routeId: String) = {
    VertxEndpointClient.make(vertx, classOf[RouteService], Optional.of(routeId))
  }

  private def makeFilterServiceClient(filterName: String) = {
    VertxEndpointClient.make(vertx, classOf[RouteFilter], Optional.of(filterName))
  }

  private def registerRouteHandler(router: Router, routeConf: RouteConf, routeClient: RouteService): Unit = {
    val route: String => Route = routeConf.method match {
      case Some(method) => routeConf.regex.getOrElse(false) match {
        case true  => router.routeWithRegex(method, _)
        case false => router.route(method, _)
      }
      case None => routeConf.regex.getOrElse(false) match {
        case true  => router.routeWithRegex(_)
        case false => router.route(_)
      }
    }

    route(routeConf.urlPath).handler { ctx =>
      ctx.put(RouteHandler.urlPathKey, routeConf.urlPath)
      ctx.next()
    }

    if (!routeConf.skipBodyHandler.getOrElse(false)) {
      val bodyHandler = BodyHandlerBuilder.build(routeConf.bodySizeLimitKb, serverConf.bodySizeLimitKb)
      route(routeConf.urlPath).handler(bodyHandler)
    }

    routeConf.filters.getOrElse(Nil).map { filter =>
      route(routeConf.urlPath).handler { ctx =>
        makeFilterServiceClient(filter.name).applyFilter(ctx, filter.conf.noSpaces).setHandler {
          result =>
            if (result.failed()) {
              log.error(s"Filter '${filter.name}' failed for ${routeConf.method} ${routeConf.urlPath}", result.cause())
              ctx.response().setStatusCode(500).end("Internal server error")
            }
        }
      }
    }

    route(routeConf.urlPath).handler { ctx =>
      routeClient.handleRequest(ctx).setHandler {
        result =>
          if (result.failed()) {
            log.error(s"Route handler failed for ${routeConf.method} ${routeConf.urlPath}", result.cause())
            ctx.response().setStatusCode(500).end("Internal server error")
          }
      }
    }
  }

  override def getActualPort(): VxFuture[Int] = VxFuture.succeededFuture(actualPort)

  override def vertxServiceAddressPrefixS: Option[String] = Option(verticleId())
}

object ApiServer {
  val defaultVerticleId = "apiServer"
}

object RouteHandler {
  val urlPathKey = "route.url.path"
}

object BodyHandlerBuilder {
  val defaultBodySizeLimitKb = 10 * 1024 // default 10MB limit

  def build(bodySizeLimitKb: Int) =
    BodyHandler.create().setBodyLimit(1024 * bodySizeLimitKb)

  def build(bodySizeLimitKb: Option[Int], fallbackBodySizeLimitKb: Option[Int]): BodyHandler =
    build(bodySizeLimitKb.orElse(fallbackBodySizeLimitKb).getOrElse(defaultBodySizeLimitKb))
}