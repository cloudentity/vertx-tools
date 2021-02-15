package com.cloudentity.tools.vertx.http

import com.cloudentity.tools.vertx.bus.VertxEndpoint
import io.vertx.core.Future

trait HttpService {
  @VertxEndpoint
  def getActualPort(): Future[Int]
}
