package com.cloudentity.tools.vertx.scala.bus

import java.util.Optional

import com.cloudentity.tools.vertx.bus.ServiceVerticle
import com.cloudentity.tools.vertx.scala.{FutureConversions, Futures, ScalaSyntax, VertxExecutionContext}

import scala.concurrent.Future

abstract class ScalaServiceVerticle extends ServiceVerticle with FutureConversions with ScalaSyntax {
  protected def vertxServiceAddressPrefixS: Option[String] = {
    val prefix = super.vertxServiceAddressPrefix
    if (prefix.isPresent) Some(prefix.get) else None
  }

  override protected def vertxServiceAddressPrefix: Optional[String] =
    vertxServiceAddressPrefixS match {
      case Some(address) => Optional.ofNullable(address)
      case None          => Optional.empty()
    }

  // ScalaVerticleTemplate code:
  protected implicit var executionContext: VertxExecutionContext = _

  def setup(): Unit = {
    this.executionContext = VertxExecutionContext(this.vertx.getOrCreateContext())
  }

  override def initServiceAsync(): VxFuture[Void] = Futures.toJava(initServiceAsyncS().asInstanceOf[Future[Void]])
  def initServiceAsyncS(): Future[Unit] = Future.successful(())

  override def start(): Unit = {
    setup()
    super.start()
  }

  override def cleanupAsync(): VxFuture[Void] = Futures.toJava(cleanupAsyncS().asInstanceOf[Future[Void]])
  def cleanupAsyncS(): Future[Unit] = Future.successful(())
}
