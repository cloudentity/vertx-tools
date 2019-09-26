package com.cloudentity.tools.vertx.sd

import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.json.JsonArray
import io.vertx.servicediscovery.Record
import io.vertx.servicediscovery.types.HttpLocation

import scalaz.Scalaz._
import scala.util.Try
import scala.collection.JavaConverters._

case class Location(host: String, port: Int, ssl: Boolean, root: Option[String])
case class Node(name: ServiceName, cb: CircuitBreaker, location: Location)
case class ServiceName(value: String)
case class NodeId(value: String)

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