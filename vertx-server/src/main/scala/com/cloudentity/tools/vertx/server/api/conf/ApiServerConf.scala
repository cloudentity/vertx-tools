package com.cloudentity.tools.vertx.server.api.conf

import io.vertx.core.http.HttpServerOptions

case class ApiServerConf(
  http: HttpServerOptions,
  routesRegistry: Option[String],
  filtersRegistry: Option[String],
  basePath: Option[String],
  bodySizeLimitKb: Option[Int],
  routes: List[RouteConf],
  prependRoutes: Option[List[RouteConf]],
  appendRoutes: Option[List[RouteConf]],
  disabledRoutes: Option[List[RouteId]],
  serverInstances: Option[Int]
)