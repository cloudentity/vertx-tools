package com.cloudentity.tools.vertx.scala.bus

import com.cloudentity.tools.vertx.bus.ComponentVerticle
import com.cloudentity.tools.vertx.scala.{FutureConversions, Futures, ScalaSyntax}
import io.vertx.core.json.JsonObject
import com.cloudentity.tools.vertx.scala.VertxExecutionContext

import scala.concurrent.Future

abstract class ScalaComponentVerticle extends ComponentVerticle with FutureConversions with ScalaSyntax {
  protected def getConfigAsyncS(): Future[JsonObject] = Futures.toScala(getConfigAsync)

  // ScalaVerticleTemplate code:
  protected implicit var executionContext: VertxExecutionContext = _

  def setup(): Unit = {
    this.executionContext = VertxExecutionContext(this.vertx.getOrCreateContext())
  }

  override def initComponentAsync(): VxFuture[Void] = Futures.toJava(initComponentAsyncS().asInstanceOf[Future[Void]])
  def initComponentAsyncS(): Future[Unit] = Future.successful(())

  override def start(): Unit = {
    setup()
    super.start()
  }

  override def cleanupAsync(): VxFuture[Void] = Futures.toJava(cleanupAsyncS().asInstanceOf[Future[Void]])
  def cleanupAsyncS(): Future[Unit] = Future.successful(())
}
