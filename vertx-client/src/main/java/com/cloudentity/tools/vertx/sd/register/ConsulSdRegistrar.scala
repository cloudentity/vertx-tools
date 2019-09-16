package com.cloudentity.tools.vertx.sd.register

import io.circe.generic.auto._
import io.circe.syntax._
import com.cloudentity.tools.vertx.bus.ComponentVerticle
import com.cloudentity.tools.vertx.sd.Location
import com.cloudentity.tools.vertx.sd.consul.ConsulConf
import com.cloudentity.tools.vertx.sd.register.ConsulSdRegistrar._
import com.cloudentity.tools.vertx.sd.consul.ConsulConf.CONSUL_CONF_KEY
import io.vertx.core.Future
import io.vertx.core.json.{JsonObject => VxJsonObject}
import io.vertx.ext.consul.{CheckOptions, ConsulClient, ConsulClientOptions, ServiceOptions}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import scalaz._
import Scalaz._
import scala.collection.mutable.ListBuffer

object ConsulSdRegistrar {

  case class ConsulSdRegistrarConf(consul: ConsulClientOptions, register: ConsulRegisterConf)
  case class ConsulRegisterConf(serviceName: String, location: Location, healthCheck: ConsulHttpHealthCheck, deregisterAfter: Option[String], tags: List[String])
  case class ConsulHttpHealthCheck(host: String, port: Int, path: String, interval: String)

  val SSL = "ssl"
  val NOT_SSL = "notssl"
  val ROOT_PATH = "root"
  val REGISTER_CONF_KEY = "register"
  val API_SERVER_CONF_KEY = "apiServer"
}

/**
  * Connects to Consul and registers service node for service-discovery.
  *
  * Configuration should be stored in ConfVerticle with key = {@link ConsulRegistrarVerticle#verticleId()} and following structure:
  *
  * {
  *   "consul": {
  *     "host": "localhost",
  *     "port": 8500,
  *     "ssl": false
  *   },
  *   "register": {
  *     "serviceName": "authz",
  *     "host": "xyz.authz.cloudentity.com",
  *     "port": 9050,
  *     "root": "/authz"
  *     "ssl": false,
  *     "healthCheckHost": "authz-1.cloudentity.com",
  *     "healthCheckPort": 8080,
  *     "healthCheckPath": "/alive",
  *     "healthCheckInterval": "3s",
  *     "deregisterAfter": "3600s",
  *     "tags": ["some-tag"]
  *   }
  * }
  *
  * If 'consul' attribute is missing there is an attempt to read it from ConfVerticle under 'consul' key.
  * If 'register.[host|port\ssl]' attribute is missing there is an attempt to read it from ConfVerticle under 'apiServer.http.[host|port|ssl]' key.
  *
  * 'root' attribute is optional
  * 'tags' is optional, custom Consul tags
  * 'healthCheckInterval' attribute is optional, defaults to '3s'
  * 'healthCheckHost' attribute is optional, defaults to 'host' attribute value
  * 'healthCheckPort' attribute is optional, defaults to 'port' attribute value
  * 'deregisterAfter' attribute is optional, defines max time-span when the node's health-check is failing, when it expires Consul deregisters the node
  */
class ConsulSdRegistrar extends ComponentVerticle {
  val log = LoggerFactory.getLogger(this.getClass)

  private var client: ConsulClient = null
  private var nodeId: String = null

  override protected def verticleId(): String = Option(super.verticleId()).getOrElse("consul-sd-registrar")

  override def start(startFuture: Future[Void]): Unit = {
    val superStart = Future.future[Void]()
    super.start(superStart)

    superStart.compose { _ =>
      getConfService.getConf(CONSUL_CONF_KEY).compose { defaultConsulConf =>
        getConfService.getConf(API_SERVER_CONF_KEY).compose { apiServerConf =>
          val confResult: Either[String, ConsulSdRegistrarConf] =
            for {
              verticleConf <- Option(getConfig).toRight(s"Missing '${verticleId()}' configuration")
              registerConf <- Option(verticleConf.getJsonObject(REGISTER_CONF_KEY)).toRight(s"Missing '${verticleId()}.$REGISTER_CONF_KEY' configuration")
              apiServerHttpConfOpt = Try(Option(apiServerConf.getJsonObject("http")).get).toOption
              conf <- buildConfiguration(Option(verticleConf.getJsonObject(CONSUL_CONF_KEY)), registerConf, apiServerHttpConfOpt, Option(defaultConsulConf))
            } yield (conf)

          confResult match {
            case Right(conf) =>
              log.debug(s"Configuration built: $conf")
              tryRegister(conf)
            case Left(msg) =>
              Future.failedFuture[Void](msg)
          }
        }
      }
    }.setHandler(startFuture)
  }

  private def tryRegister(conf: ConsulSdRegistrarConf): Future[Void] = {
    Try[Future[Void]] {
      client = createConsulClient(conf.consul)
      nodeId = getNodeId(conf)

      register(buildServiceOptions(conf.register, nodeId))
        .compose { _ =>
          log.info(s"Consul service registration succeeded. Configuration: ${conf.register.asJson.noSpaces}")
          Future.succeededFuture[Void]()
        }
    } match {
      case Success(fut) => fut
      case Failure(ex) => Future.failedFuture[Void](ex)
    }
  }

  protected def getNodeId(conf: ConsulSdRegistrarConf): String =
    s"${conf.register.serviceName}:${conf.register.location.host}:${conf.register.location.port}"

  private def createConsulClient(consul: ConsulClientOptions) = {
    ConsulClient.create(vertx, consul)
  }

  private def buildConfiguration(consulConf: Option[VxJsonObject], registerConf: VxJsonObject, apiServerHttpConf: Option[VxJsonObject], defaultConsulConf: Option[VxJsonObject]): Either[String, ConsulSdRegistrarConf] = {
    log.debug(s"Attempting to build Consul SD registrar configuration: ${verticleId()}.consul=$consulConf, ${verticleId()}.register=$registerConf, apiServer.http=$apiServerHttpConf, consul=$defaultConsulConf")

    val consulConfEither: Either[String, Option[ConsulClientOptions]] = consulConf.map(ConsulConf.fromVxJsonObject).sequenceU
    val defaultConsulConfOpt: Option[ConsulClientOptions] = defaultConsulConf.flatMap(ConsulConf.fromVxJsonObject(_).toOption)

    val defaultServiceHost = apiServerHttpConf.flatMap(c => Option(c.getString("host")))
    val defaultServicePort = apiServerHttpConf.flatMap(c => Option(c.getInteger("port")))
    val defaultServiceSsl = apiServerHttpConf.flatMap(c => Option(c.getBoolean("ssl")))

    for {
      consulConfOpt       <- consulConfEither

      consulConf          <- consulConfOpt.orElse(defaultConsulConfOpt).toRight(missingAttr(s"$CONSUL_CONF_KEY"))
      host                <- Option(registerConf.getString("host")).orElse(defaultServiceHost).toRight(missingAttr(s"$REGISTER_CONF_KEY.host"))
      port                <- Option(registerConf.getInteger("port")).orElse(defaultServicePort).toRight(missingAttr(s"$REGISTER_CONF_KEY.port"))
      ssl                 <- Option(registerConf.getBoolean("ssl")).orElse(defaultServiceSsl).toRight(missingAttr(s"$REGISTER_CONF_KEY.ssl"))
      rootPathOpt          = Option(registerConf.getString("rootPath"))

      serviceName         <- Option(registerConf.getString("serviceName")).toRight(missingAttr(s"$REGISTER_CONF_KEY.serviceName"))
      healthCheckHost      = registerConf.getString("healthCheckHost", host)
      healthCheckPort      = registerConf.getInteger("healthCheckPort", port)
      healthCheckPath     <- Option(registerConf.getString("healthCheckPath")).toRight(missingAttr(s"$REGISTER_CONF_KEY.healthCheckPath"))
      healthCheckInterval  = registerConf.getString("healthCheckInterval", "3s")
      deregisterAfter      = Option(registerConf.getString("deregisterAfter"))
      healthCheck          = ConsulHttpHealthCheck(healthCheckHost, healthCheckPort, healthCheckPath, healthCheckInterval)
      tags                 = readTags(registerConf)
    } yield (ConsulSdRegistrarConf(consulConf, ConsulRegisterConf(serviceName, Location(host, port, ssl, rootPathOpt), healthCheck, deregisterAfter, tags)))
  }.left.map(msg => s"Could not register service in Consul. $msg")

  private def readTags(registerConf: VxJsonObject): List[String] =
    Option(registerConf.getJsonArray("tags"))
      .map(_.asScala.toList.map(_.toString)).getOrElse(Nil)

  private def missingAttr(name: String) = s"Missing '${verticleId()}.$name' configuration"

  override def stop(stopFuture: Future[Void]): Unit = {
    this.client.deregisterService(nodeId, stopFuture)
  }

  private def buildServiceOptions(conf: ConsulRegisterConf, nodeId: String): ServiceOptions = {
    val sslTag = if (conf.location.ssl) SSL else NOT_SSL

    val checkOpts =
      new CheckOptions()
        .setHttp(s"${if (conf.location.ssl) "https" else "http"}://${conf.healthCheck.host}:${conf.healthCheck.port}${conf.healthCheck.path}")
        .setInterval(conf.healthCheck.interval)
    conf.deregisterAfter.foreach(checkOpts.setDeregisterAfter)

    val tags: ListBuffer[String] = ListBuffer(conf.serviceName, "http-endpoint", sslTag) ++ conf.tags
    val tagsWithRootPath = conf.location.root.fold(tags)(root => tags :+ s"$ROOT_PATH:$root")

    new ServiceOptions()
      .setName(conf.serviceName)
      .setTags(tagsWithRootPath.asJava)
      .setId(nodeId)
      .setPort(conf.location.port)
      .setAddress(conf.location.host)
      .setCheckOptions(
        checkOpts
      )
  }

  private def register(opts: ServiceOptions): Future[Void] = {
    val f = Future.future[Void]()
    client.registerService(opts, f)
    f
  }
}
