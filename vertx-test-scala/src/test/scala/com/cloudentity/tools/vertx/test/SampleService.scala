package com.cloudentity.tools.vertx.test

import com.cloudentity.tools.vertx.bus.{ServiceVerticle, VertxEndpoint}
import io.vertx.core.Future
import org.slf4j.LoggerFactory

trait SampleService {
  @VertxEndpoint(address = "cut")
  def cut(param: String): Future[String]
}

class SampleServiceVerticle extends ServiceVerticle with SampleService {
  val log = LoggerFactory.getLogger(this.getClass)

  def cut(param: String): Future[String] = {
    log.debug(s"config: $getConfig")
    val length = getConfig.getInteger("length")
    log.debug(s"Substring: $param, length: $length")

    Future.succeededFuture(param.substring(0, length))
  }
}