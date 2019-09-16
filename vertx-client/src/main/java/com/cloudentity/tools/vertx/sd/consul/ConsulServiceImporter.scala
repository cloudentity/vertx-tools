package com.cloudentity.tools.vertx.sd.consul

import java.util

import com.cloudentity.tools.vertx.sd.register.ConsulSdRegistrar
import io.vertx.core.json.{JsonArray => VxJsonArray, JsonObject => VxJsonObject}
import io.vertx.core.{CompositeFuture, Future, Handler, Promise, Vertx}
import io.vertx.ext.consul._
import io.vertx.servicediscovery.impl.ServiceTypes
import io.vertx.servicediscovery.spi.{ServiceImporter, ServicePublisher, ServiceType}
import io.vertx.servicediscovery.types.HttpLocation
import io.vertx.servicediscovery.{Record, Status}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
  * Consul service-discovery bridge.
  */
class ConsulServiceImporter extends ServiceImporter {
  val log = LoggerFactory.getLogger(this.getClass)

  case class RegistrationId(value: String)
  case class Registration(id: RegistrationId, entry: ServiceEntry)

  case class PublishDecision(keep: List[Registration], remove: List[Registration], publish: List[ServiceEntry])

  var client: ConsulClientWrapper = null
  var registrations: List[Registration] = List()
  var publisher: ServicePublisher = null

  override def start(vertx: Vertx, publisher: ServicePublisher, configuration: VxJsonObject, promise: Promise[Void]): Unit = {
    this.publisher = publisher
    client = ConsulClientWrapper(ConsulClient.create(vertx, buildConsulClientOptions(configuration)))

    retrievePassingNodesAndRepublish().compose { _ =>
      vertx.setPeriodic(configuration.getLong("scan-period", 2000l), { _ =>
        retrievePassingNodesAndRepublish()
          .setHandler { async =>
            if (async.failed()) log.error("Failure on retrieving and publishing nodes from Consul", async.cause())
          }
        }
      )

      log.info("Consul service importer initialised successfully")
      Future.succeededFuture[Void]()
    }.setHandler(promise)
  }

  override def close(closeHandler: Handler[Void]): Unit =
    FutureUtil.sequence(registrations.map(unPublish)).setHandler(_ => closeHandler.handle(null))

  private def buildConsulClientOptions(configuration: VxJsonObject) = {
    val opts = new ConsulClientOptions(configuration)
    val host = configuration.getString("host", "localhost")
    val port = configuration.getInteger("port", 8500)

    opts.setHost(host)
    opts.setPort(port)
    opts
  }

  private def retrievePassingNodesAndRepublish(): Future[Void] = {
    retrievePassingServices().compose { entries =>
      log.trace(s"Healthy services discovered in Consul: ${audit(entries)}")
      val decision = decide(entries, registrations)
      log.trace(s"Republish decision: publish=${audit(decision.publish)}, remove=${audit(decision.remove.map(_.entry))}, keep=${audit(decision.keep.map(_.entry))}")

      decision.remove.foreach(unPublish)
      FutureUtil.sequence(decision.publish.map(publish)).compose { added =>
        registrations = decision.keep ++ added
        Future.succeededFuture[Void]()
      }
    }
  }

  private def publish(entry: ServiceEntry): Future[Registration] = {
    val f = Future.future[Record]()
    val record = RecordBuilder.build(entry, Status.UP) // we are retrieving from Consul only nodes passing health-checks, so we set Statut.UP
    publisher.publish(record, f)
    f.compose { r =>
      Future.succeededFuture(Registration(RegistrationId(r.getRegistration), entry))
    }
  }

  private def unPublish(registration: Registration): Future[Void] = {
    val f = Future.future[Void]()
    publisher.unpublish(registration.id.value, async => {
      if (async.failed()) log.error(s"Could not un-register node: registrationId=${registration.id}, serviceEntry=${registration.entry.toJson}")
      f.handle(async)
    })
    f
  }

  private def decide(discovered: List[ServiceEntry], registered: List[Registration]): PublishDecision = {
    val (toKeep, toRemove) = registered.partition(node => discovered.map(_.getService.getId).contains(node.entry.getService.getId))
    val toPublish = discovered.filter(e => !registered.map(_.entry.getService.getId).contains(e.getService.getId))

    PublishDecision(toKeep, toRemove, toPublish)
  }

  private def retrievePassingServices(): Future[List[ServiceEntry]] = {
    client.catalogServices().compose { services =>
      FutureUtil.sequence(services.map(s => client.catalogPassingServiceNodes(s.getName)))
        .compose(ls => Future.succeededFuture(ls.flatten))
    }
  }

  private def audit(xs: List[ServiceEntry]): String =
    xs.map(_.toJson).mkString("[",",","]")
}

case class ConsulClientWrapper(client: ConsulClient) {
  def catalogServices(): Future[List[Service]] = {
    val f = Future.future[ServiceList]()
    client.catalogServices(f)
    f.compose(sl => Future.succeededFuture(sl.getList.asScala.toList))
  }

  def catalogPassingServiceNodes(serviceName: String): Future[List[ServiceEntry]] = {
    val f = Future.future[ServiceEntryList]()
    client.healthServiceNodes(serviceName, true, f)
    f.compose(sl => Future.succeededFuture(sl.getList.asScala.toList))
  }
}

object FutureUtil {
  def sequence[A](fs: List[Future[A]]): Future[List[A]] =
    CompositeFuture.all(fs.asJava.asInstanceOf[util.List[Future[_]]])
      .compose { fut =>
        Future.succeededFuture(fut.list[A]().asScala.toList)
      }
}

object RecordBuilder {
  val log = LoggerFactory.getLogger(this.getClass)

  def build(entry: ServiceEntry, status: Status): Record = {
    val recordType = getType(entry)

    new Record()
      .setName(entry.getService.getName)
      .setStatus(status)
      .setType(recordType)
      .setLocation(getLocation(entry, recordType))
      .setMetadata(getMetadata(entry))
  }

  def getType(entry: ServiceEntry): String =
    ServiceTypes.all.asScala
      .find(t => entry.getService.getTags.contains(t.name()))
      .map(_.name())
      .getOrElse(ServiceType.UNKNOWN)

  def getLocation(entry: ServiceEntry, recordType: String): VxJsonObject = {
    val rootPathPrefix = s"${ConsulSdRegistrar.ROOT_PATH}:"
    val rootOpt: Option[String] = entry.getService.getTags.asScala.find(_.startsWith(rootPathPrefix)).map(_.drop(rootPathPrefix.length))

    val loc = new VxJsonObject()
      .put("host", entry.getService.getAddress)
      .put("port", entry.getService.getPort)

    rootOpt.foreach(loc.put("root", _))

    if (recordType == "http-endpoint") {
      loc.put("ssl", entry.getService.getTags.contains("ssl"))
      new HttpLocation(loc).toJson()
    } else loc
  }

  def getMetadata(entry: ServiceEntry): VxJsonObject = {
    val tags = new VxJsonArray()
    entry.getService.getTags.forEach(t => tags.add(t))
    new VxJsonObject()
      .put("ID", entry.getService.getId)
      .put("tags", tags)
  }
}
