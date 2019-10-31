package com.cloudentity.tools.vertx.server.api.conf

import io.circe._
import io.vertx.core.http.HttpServerOptions

case class RouteConfs(value: List[RouteConf])

object RouteConfs {
  import io.circe.generic.auto._
  import codecs._
  import io.circe.generic.semiauto._

  implicit lazy val StringKeyDecoder = KeyDecoder.decodeKeyString
  implicit lazy val RouteConfDecoder = deriveDecoder[RouteConf]

  implicit lazy val ListRouteConfDecoder: Decoder[List[RouteConf]] = Decoder.decodeList[RouteConf](RouteConfDecoder)
  implicit lazy val MapRouteConfDecoder: Decoder[List[RouteConf]] = Decoder.decodeMap[String, List[RouteConf]](StringKeyDecoder, ListRouteConfDecoder).map(_.toList.sortBy(_._1).flatMap(_._2))

  implicit lazy val RouteConfsDecoder: Decoder[RouteConfs] = ListRouteConfDecoder.or(MapRouteConfDecoder).map(RouteConfs.apply)
}

case class ApiServerConf(
  http: HttpServerOptions,
  routesRegistry: Option[String],
  filtersRegistry: Option[String],
  basePath: Option[String],
  bodySizeLimitKb: Option[Int],
  routes: RouteConfs,
  prependRoutes: Option[List[RouteConf]],
  appendRoutes: Option[List[RouteConf]],
  disabledRoutes: Option[List[RouteId]],
  serverInstances: Option[Int]
)