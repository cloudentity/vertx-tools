package com.cloudentity.tools.vertx.server.api

import java.util.Optional

import io.circe.generic.auto._
import io.circe.parser._
import com.cloudentity.tools.vertx.registry.RegistryVerticle
import com.cloudentity.tools.vertx.scala.bus.ScalaComponentVerticle
import com.cloudentity.tools.vertx.server.api.conf.{ApiServerConf, FilterConf}
import com.cloudentity.tools.vertx.server.api.conf.codecs._
import com.cloudentity.tools.vertx.server.api.filters.RouteFilter
import io.vertx.core.{DeploymentOptions, Vertx}
import com.cloudentity.tools.vertx.verticles.VertxDeploy
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class ApiServerRoot() extends ScalaComponentVerticle {
  val log = LoggerFactory.getLogger(this.getClass)

  override def verticleId(): String = Option(super.verticleId()).getOrElse("apiServer")

  override def initComponentAsync(): VxFuture[Void] = {
      decode[ApiServerConf](getConfig().toString).left.map(_.fillInStackTrace()) match {
        case Right(serverConf) =>
          log.info(s"Starting app on port: ${serverConf.http.getPort}")

            for {
              _ <- RegistryVerticle.deploy(vertx, serverConf.routesRegistry.getOrElse("routes")).toScala()
              _ <- RegistryVerticle.deploy(vertx, serverConf.filtersRegistry.getOrElse("filters"), false).toScala()
              _ <- validateFilterConfs(vertx, serverConf.routes.flatMap(_.filters.getOrElse(Nil)))
              _ <- deployServers(vertx, serverConf)
            } yield (())
        case Left(ex) =>
          log.error(s"'${verticleId()}' configuration is wrong. Decoding ApiServerConf failed", ex)
          Future.failed(new Exception(s"'${verticleId()}' configuration is wrong", ex))
      }
  }.map[Void](_ => null).toJava()

  private def validateFilterConfs(vertx: Vertx, filterConfs: List[FilterConf]): Future[Unit] =
    Future.sequence {
      filterConfs.map { conf =>
        createClient(classOf[RouteFilter], Optional.of(conf.name)).validateConfig(conf.conf.noSpaces).toScala()
      }
    }.flatMap { results =>
      val errors = results.filterNot(_.isSuccess)
      if (errors.nonEmpty) {
        Future.failed(new Exception("Invalid filters configuration:\n" + errors.map(_.getError).mkString("\n", "\n", "\n")))
      } else {
        Future.successful(())
      }
    }

  /**
    * Deploys DeploymentOptions.instances of {@link com.cloudentity.tools.vertx.server.ApiServer} along with the route-handler verticles.
    *
    * @param vertx
    * @param serverConf
    * @param ec
    * @return instance of io.vertx.core.Future, will be failed if operation failed, successful otherwise
    */
  def deployServers(vertx: Vertx, serverConf: ApiServerConf)(implicit ec: ExecutionContext): Future[Void] = {
    def deployRec(instances: Int, acc: Future[Unit]): Future[Unit] =
      if (instances > 0) {
        val server  = new ApiServer(serverConf)
        val nextAcc = acc.flatMap(_ => VertxDeploy.deploy(vertx, server, new DeploymentOptions()).toScala.map(_ => ()))

        deployRec(instances - 1, nextAcc)
      } else acc

    deployRec(serverConf.serverInstances.getOrElse(2 * Runtime.getRuntime().availableProcessors()), Future.successful(())).map(_ => null: Void)
  }
}
