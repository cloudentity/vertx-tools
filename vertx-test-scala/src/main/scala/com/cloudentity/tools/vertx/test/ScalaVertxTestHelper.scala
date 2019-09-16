package com.cloudentity.tools.vertx.test

import com.cloudentity.tools.vertx.scala.FutureConversions
import io.vertx.core.Vertx
import com.cloudentity.tools.vertx.scala.VertxExecutionContext


trait ScalaVertxTestHelper extends FutureConversions {
  def vertx: Vertx
  implicit lazy val ec = VertxExecutionContext(vertx.getOrCreateContext())
}
