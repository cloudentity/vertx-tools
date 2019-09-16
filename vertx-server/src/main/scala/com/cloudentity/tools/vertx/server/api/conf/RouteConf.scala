package com.cloudentity.tools.vertx.server.api.conf

import io.vertx.core.http.HttpMethod

case class RouteId(value: String) extends AnyVal

case class RouteConf(
  id: RouteId,
  handler: Option[String],
  method: Option[HttpMethod],
  urlPath: String,
  skipBodyHandler: Option[Boolean],
  bodySizeLimitKb: Option[Int],
  regex: Option[Boolean],
  filters: Option[List[FilterConf]]
)
