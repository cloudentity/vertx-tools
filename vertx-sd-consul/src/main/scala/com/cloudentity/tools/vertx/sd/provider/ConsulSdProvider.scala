package com.cloudentity.tools.vertx.sd.provider

import com.cloudentity.tools.vertx.bus.{ComponentVerticle, ServiceClientFactory}
import com.cloudentity.tools.vertx.sd.SdService
import com.cloudentity.tools.vertx.sd.consul.ConsulConf._
import com.cloudentity.tools.vertx.sd.consul.{ConsulConf, ConsulServiceImporter}
import com.cloudentity.tools.vertx.sd.provider.ConsulSdProvider._
import io.vertx.core.Future
import io.vertx.core.json.{JsonObject => VxJsonObject}

import scalaz._, Scalaz._

object ConsulSdProvider {
  val VERTICLE_ID = "consul-sd-provider"
  val DISCOVERY_CONF_KEY = "discovery"
}

/**
  * Provides discovered node records from Consul. Creates {@link ConsulServiceImporter} and registers it in SdVerticle as ServiceImporter.
  *
  * Consul configuration should be stored in ConfVerticle with key = {@link ConsulSdProvider#VERTICLE_ID} and following structure:
  *
  * {
  *   "consul": {
  *     "host": "localhost",
  *     "port": 8500,
  *     "ssl": false
  *   },
  *   "discovery": {
  *     "scan-period": 2000
  *   }
  * }
  *
  * If 'consul' attribute is missing there is an attempt to read it from ConfVerticle under 'consul' key.
  */
class ConsulSdProvider extends ComponentVerticle {
  override def start(promise: Future[Void]): Unit = {
    val superStart = Future.future[Void]()
    super.start(superStart)

    superStart.compose { _ =>
      getConfService().getConf(ConsulConf.CONSUL_CONF_KEY).compose { defaultConsulConf =>
        val sd = ServiceClientFactory.make(vertx.eventBus, classOf[SdService])

        val discoveryOpt = Option(getConfig()).flatMap(c => Option(c.getJsonObject(DISCOVERY_CONF_KEY)))
        val consulConfOpt = Option(getConfig()).flatMap(c => Option(c.getJsonObject(CONSUL_CONF_KEY)))

        buildConfiguration(discoveryOpt, consulConfOpt, Option(defaultConsulConf)) match {
          case Right(importerConf) =>
            sd.registerServiceImporter(new ConsulServiceImporter, importerConf)
          case Left(errMsg) =>
            Future.failedFuture[Void](errMsg)
        }
      }
    }.setHandler(promise)
  }

  def buildConfiguration(discoveryOpt: Option[VxJsonObject], consulConf: Option[VxJsonObject], defaultConsulConf: Option[VxJsonObject]): Either[String, VxJsonObject] = {
    val consulConfEither = consulConf.map(ConsulConf.fromVxJsonObject).sequenceU
    val defaultConsulConfOpt = defaultConsulConf.flatMap(ConsulConf.fromVxJsonObject(_).toOption)

    for {
      consulConfOpt <- consulConfEither
      consulConf    <- consulConfOpt.orElse(defaultConsulConfOpt).toRight(missingAttr(s"$CONSUL_CONF_KEY"))
      importerConf   = discoveryOpt.getOrElse(new VxJsonObject)
    } yield {
      importerConf.mergeIn(consulConf.toJson)
    }
  }

  private def missingAttr(name: String) = s"Missing '${verticleId()}.$name' configuration"

  override protected def verticleId = VERTICLE_ID
}
