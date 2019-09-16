package com.cloudentity.tools.vertx.sd.consul

import io.vertx.core.json.{JsonObject => VxJsonObject}
import io.vertx.ext.consul.ConsulClientOptions

import scala.util.Try

object ConsulConf {
  val CONSUL_CONF_KEY = "consul"

  def fromVxJsonObject(consulConf: VxJsonObject): Either[String, ConsulClientOptions] =
    Try(new ConsulClientOptions(consulConf)).toEither.left.map("Could not decode 'consul' configuration: " + _.getMessage)
}