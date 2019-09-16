package com.cloudentity.tools.vertx.sd

import java.util.{List => JList}

import com.cloudentity.tools.vertx.bus.{ServiceClientFactory, VertxBus}
import com.cloudentity.tools.vertx.sd.circuit.{CircuitBreakerFactory, CircuitBreakerLoadEvaluator, CircuitBreakerWithLoadEvaluator, NoopCB}
import com.cloudentity.tools.vertx.sd.loadBalancing.LoadBalancingStrategy
import io.vertx.circuitbreaker.{CircuitBreaker, CircuitBreakerOptions}
import io.vertx.core.json.JsonArray
import io.vertx.core.{Future, Promise, Vertx}
import io.vertx.servicediscovery.Record
import io.vertx.servicediscovery.types.HttpLocation
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scalaz.Scalaz._

import scala.util.Try

case class Location(host: String, port: Int, ssl: Boolean, root: Option[String])
case class Node(name: ServiceName, cb: CircuitBreaker, location: Location)
case class SmartRecord(id: NodeId, location: Location, tags: List[String])

object SmartRecord {
  def apply(r: Record): Either[Exception, SmartRecord] = {
    for {
      id   <- Option(r.getMetadata.getString("ID")).toRight(s"metadata.ID")
      host <- Option(r.getLocation.getString("host")).toRight(s"location.host")
      port <- Option(r.getLocation.getInteger("port")).toRight(s"location.port")
      ssl  <- Option(r.getLocation.getBoolean("ssl", false)).toRight(s"location.ssl")
      rootPath = Option(r.getLocation.getString("root"))
      tags = readTags(r).getOrElse(Nil)
    } yield SmartRecord(NodeId(id), Location(host, port, ssl, rootPath), tags)
  }.leftMap(msg => new Exception(msg + s" missing in Record  ${r.toJson}"))

  def toRecord(sr: SmartRecord): Record = {
    val r = new Record()
    r.getMetadata.put("ID", sr.id.value).put("tags", sr.tags.asJava)
    r.setLocation(buildLocationJson(sr.location))
  }

  private def buildLocationJson(location: Location): io.vertx.core.json.JsonObject = {
    val httpLocation =
      new HttpLocation()
        .setHost(location.host)
        .setPort(location.port)
        .setSsl(location.ssl)
    location.root.foreach(httpLocation.setRoot)

    val locationJson = httpLocation.toJson
    if (location.root.isEmpty) locationJson.remove("root") // HttpLocation by default sets 'root' to empty string. If Location.rootPath is None then we don't want it in json-object
    locationJson
  }


  private def readTags(r: Record): Try[List[String]] = Try {
    r.getMetadata.getJsonArray("tags", new JsonArray())
      .getList.asScala.toList
      .map(_.asInstanceOf[String])
  }
}

case class ServiceName(value: String)
case class NodeId(value: String)

object SmartSd {
  val log = LoggerFactory.getLogger(this.getClass)

  def apply(vertx: Vertx, serviceName: ServiceName, serviceTags: List[String], lb: LoadBalancingStrategy, cbFactory: NodeId => CircuitBreaker): Future[SmartSd] = {
    val ssd = new SmartSd(vertx, serviceName, serviceTags, cbFactory, lb)

    ssd.sdClient.discover(serviceName.value)
      .compose { records =>
        val eithers: List[Either[Exception, SmartRecord]] = records.asScala.toList.map(SmartRecord.apply)
        val either: Either[Exception, List[SmartRecord]] = eithers.sequenceU

        either match {
          case Right(records) =>
            ssd.update(records)
            log.debug(s"SmartSd for service '${serviceName.value}' started")
            Future.succeededFuture(ssd)
          case Left(ex) =>
            log.error(s"Failed to start SmartSd for service '${serviceName.value}': ${ex.getMessage}")
            Future.failedFuture(ex)
        }
      }
  }

  def withRoundRobinDiscovery(vertx: Vertx, serviceName: ServiceName, tags: List[String], cbFactoryFut: Future[CircuitBreakerFactory]): Future[SmartSd] =
    cbFactoryFut.compose { cbFactory => withRoundRobinDiscovery(vertx, serviceName, tags, cbFactory) }

  def withRoundRobinDiscovery(vertx: Vertx, serviceName: ServiceName, tags: List[String], cbFactory: CircuitBreakerFactory): Future[SmartSd] =
    apply(vertx, serviceName, tags, LoadBalancingStrategy.roundRobin, cbFactory.build)

  /**
    * Creates SmartSd that builds circuit breakers with default CircuitBreakerOptions.
    */
  def withRoundRobinDiscovery(vertx: Vertx, serviceName: ServiceName, tags: List[String], opts: CircuitBreakerOptions): Future[SmartSd] =
    withRoundRobinDiscovery(vertx, serviceName, tags, CircuitBreakerFactory.fromOpts(vertx, serviceName, opts))

  def withMinLoadDiscovery(vertx: Vertx, serviceName: ServiceName, tags: List[String], eval: CircuitBreakerLoadEvaluator): Future[SmartSd] =
    apply(vertx, serviceName, tags, LoadBalancingStrategy.minLoad, id => new CircuitBreakerWithLoadEvaluator(CircuitBreaker.create(s"${serviceName.value}:${id.value}", vertx), eval))
}

trait Sd {
  def discover(): Option[Node]
  def serviceName(): ServiceName
  def close(): Future[Unit]
}

object Sd {
  def withStaticLocation(location: Location): Sd =
    new Sd {
      val _serviceName = ServiceName(s"http${if (location.ssl) "s" else ""}//${location.host}:${location.port}")

      override def discover(): Option[Node] = Some(Node(_serviceName, new NoopCB(_serviceName.value), location))
      override def serviceName(): ServiceName = _serviceName
      override def close(): Future[Unit] = Future.succeededFuture()
    }
}

case class SmartSd(vertx: Vertx, serviceName: ServiceName, serviceTags: List[String], cbFactory: NodeId => CircuitBreaker, lb: LoadBalancingStrategy) extends Sd {
  val log = LoggerFactory.getLogger(this.getClass.getName + ": " + serviceName.value)

  val sdClient = ServiceClientFactory.make(vertx.eventBus, classOf[SdService])
  var nodes = Vector[Node]()

  val nodesConsumer =
    VertxBus.consumePublished(vertx.eventBus, s"${SdVerticle.SERVICE_DISCOVERY_REFRESH_ADDR}.${serviceName.value}", classOf[java.util.List[Record]],
      (rs: JList[Record]) => {
        val smartRecords = rs.asScala.toList.map(SmartRecord.apply).collect { case Right(record) => record } // SmartSd.apply succeeded on SmartSd startup, so we can cast Either to value
        update(smartRecords)
      }
    )

  def discover(): Option[Node] = {
    val (newNodes, discoveredNodeOpt) = lb.run(nodes)
    nodes = newNodes

    discoveredNodeOpt match {
      case Some(node) => log.trace(s"Discovered node: ${node.location}")
      case None       => log.trace("No nodes available")
    }

    discoveredNodeOpt
  }

  private def update(records: List[SmartRecord]): Unit = {
    val rs = records.filter(r => serviceTags.diff(r.tags).isEmpty)
    val toKeep = nodes.filter(n => rs.exists(r => matches(n, r)))
    val toAdd = rs.filter(r => !nodes.exists(n => matches(n, r)))

    log.debug(s"Updating nodes of `${serviceName.value}`. Keeping: ${logLocations(toKeep.map(_.location))}, adding: ${logLocations(toAdd.map(_.location))}")

    nodes = toKeep ++ build(toAdd)
  }

  private def logLocations(ls: Seq[Location]): String =
    ls.map(l => s"{host=${l.host},port=${l.port},ssl=${l.ssl}}").mkString("[", ",", "]")

  private def matches(n: Node, r: SmartRecord): Boolean =
    n.location == r.location

  private def build(rs: List[SmartRecord]): Vector[Node] =
    rs.map { r => Node(serviceName, cbFactory(r.id), r.location) }.toVector

  override def close(): Future[Unit] = {
    val promise = Promise.promise[Void]()
    nodesConsumer.unregister(promise)
    promise.future.map(())
  }
}
