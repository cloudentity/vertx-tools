package com.cloudentity.tools.vertx.sd

import java.util.{List => JList}

import com.cloudentity.tools.vertx.bus.{ServiceVerticle, VertxBus}
import com.cloudentity.tools.vertx.sd.SdVerticle._
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.json.JsonObject
import io.vertx.core.{AsyncResult, Future}
import io.vertx.servicediscovery.{Record, ServiceDiscovery, Status}
import io.vertx.servicediscovery.spi.ServiceImporter
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

object SdVerticle {
  val log = LoggerFactory.getLogger(this.getClass)
  val SERVICE_DISCOVERY_REFRESH_ADDR = "service.discovery.refresh"

  case class RepublishDecision(changedServices: Map[String, List[Record]], unchangedServices: List[String], removedServices: List[String])

  def makeRepublishDecision(discovered: Map[String, List[Record]], prevDiscovered: Map[String, List[Record]]): RepublishDecision = {
    val removed = prevDiscovered.keys.toSet -- discovered.keys.toSet
    val (changed, unchanged) = discovered.partition { case (serviceName, records) =>
      prevDiscovered.get(serviceName) match {
        case Some(prevRecords) =>
          records.map(_.getLocation).toSet != prevRecords.map(_.getLocation).toSet
        case None => true
      }
    }

    RepublishDecision(changed, unchanged.keys.toList, removed.toList)
  }
}

/**
  * Class holds reference to vertx' ServiceDiscovery and every 3s publishes discovered nodes per service (unless they didn't change).
  * If at least one node of a given service changed then all nodes are republished. The consumers need to figure out what nodes actually changed.
  *
  * It's agnostic about the service discovery mechanism, i.e. one should register service importer {@see ConsulSdProvider, FixedSdProvider}.
  *
  * Configuration is optional, can be stored in ConfVerticle with key = {@link SdVerticle#verticleId()} and following structure:
  *
  * {
  *   "scan-period": 3000
  * }
  *
  * "scan-period" is optional attribute, defaults to 3000 ms
  */
class SdVerticle extends ServiceVerticle with SdService {
  val log = LoggerFactory.getLogger(this.getClass)

  private[sd] var prevDiscovered = Map[String, List[Record]]()
  private[sd] var discovery: ServiceDiscovery = null

  override def start(startFuture: Future[Void]): Unit = {
    discovery = ServiceDiscovery.create(vertx)

    val superStart = Future.future[Void]()
    super.start(superStart)

    superStart
      .compose { _ => discover() }
      .compose { discoveredRecords =>
        makeDecisionAndPublish(discoveredRecords)

        Future.succeededFuture[Void]()
      }
      .compose { _ =>
        vertx.setPeriodic(getScanPeriod(), _ => {
          discover.setHandler((async: AsyncResult[JList[Record]]) => {
            if (async.succeeded) {
              makeDecisionAndPublish(async.result())
            }
            else log.error("Discovering services failed", async.cause)
          })
        })

        log.info("Service discovery initialised successfully")
        Future.succeededFuture[Void]()
      }.setHandler(startFuture)
  }

  private def getScanPeriod(): Long =
    Option(getConfig).getOrElse(new JsonObject()).getLong("scan-period", 3000l)

  private def makeDecisionAndPublish(discoveredRecords: JList[Record]): Unit = {
    val discovered: Map[String, List[Record]] = discoveredRecords.asScala.toList.groupBy(_.getName)
    val decision = SdVerticle.makeRepublishDecision(discovered, prevDiscovered)

    republish(decision)
    prevDiscovered = discovered
  }

  private def republish(decision: RepublishDecision): Unit = {
    decision.changedServices.foreach { case (serviceName, records) =>
      publishRecords(serviceName, records)
    }

    decision.removedServices.foreach { serviceName =>
      publishRecords(serviceName, List())
    }

    if (decision.unchangedServices.nonEmpty)
      log.trace(s"Services [${decision.unchangedServices.mkString(", ")}] didn't change. Skipping republish")
  }

  private def publishRecords(serviceName: String, records: List[Record]): Unit = {
    log.info(s"Publishing ${records.size} node(s) of service '$serviceName': ${records.map((r: Record) => r.toJson).mkString("[", ", ", "]")}")
    VertxBus.publish(vertx.eventBus, SdVerticle.SERVICE_DISCOVERY_REFRESH_ADDR + "." + serviceName, records.asJava)
  }

  override def verticleId() = "service-discovery"

  override def discover: Future[JList[Record]] = {
    val promise = Future.future[JList[Record]]
    discovery.getRecords(r => Status.UP.equals(r.getStatus), promise)
    promise
  }

  override def discover(serviceName: String): Future[JList[Record]] = {
    val promise = Future.future[JList[Record]]
    discovery.getRecords(r => Status.UP.equals(r.getStatus) && serviceName == r.getName, promise)
    promise
  }

  override def registerServiceImporter(importer: ServiceImporter, conf: JsonObject): Future[Void] = {
    val promise = Future.future[Void]
    discovery.registerServiceImporter(importer, conf, promise)
    promise
  }
}
