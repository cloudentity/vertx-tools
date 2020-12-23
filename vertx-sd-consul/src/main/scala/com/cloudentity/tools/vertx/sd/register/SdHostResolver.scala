package com.cloudentity.tools.vertx.sd.register

import java.net.NetworkInterface

import scala.collection.JavaConverters._

object SdHostResolver {
  case class Network(name: String, addresses: List[Address])
  case class Address(ip: String, host: String)

  def resolve(networks: List[Network])(preferIp: Boolean, preferredNetworkOpt: Option[String], preferredIp: Option[String], preferredHost: Option[String]): Option[String] = {
    if (preferredNetworkOpt.isEmpty && preferredIp.isEmpty && preferredHost.isEmpty) None
    else {
      val addresses = networks.filter(_.name.matches(preferredNetworkOpt.getOrElse(".*"))).flatMap(_.addresses)

      addresses
        .filter(address => address.host.matches(preferredHost.getOrElse(".*")))
        .filter(address => address.ip.startsWith(preferredIp.getOrElse("")))
        .map { address =>
          if (preferIp) address.ip else address.host
        }.headOption
    }
  }

  def convert(ifs: List[NetworkInterface]): List[Network] =
    ifs.map { i =>
      Network(i.getName, i.getInetAddresses.asScala.toList.map(addr => Address(ip = addr.getHostAddress, host = addr.getHostName)))
    }
}
