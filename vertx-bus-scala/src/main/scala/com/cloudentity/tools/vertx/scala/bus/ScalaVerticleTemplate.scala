package com.cloudentity.tools.vertx.scala.bus

import io.vertx.core.{Future => VxFuture}


/**
  * This trait contains code that needs to be copied over to make Java verticle (extending AbstractVerticle) extensible in Scala code
  * with access to io.vertx.scala.core.ScalaVerticle-like utils.
  *
  * When extending Java verticle do not overwrite start/stop methods, use init/exit instead to avoid cumbersome super calls.
  *
  * {@see io.cloudentity.tools.scala.ScalaComponentVerticle}
  */
trait ScalaVerticleTemplate {
  /*
  protected implicit var executionContext: ExecutionContext = _
  protected var vertxS: Vertx = _
  protected var ctxS: Context = _

  def setup(): Unit = {
    this.vertxS = new Vertx(vertx)
    this.ctxS = new Context(context)
    this.executionContext = VertxExecutionContext(this.vertxS.getOrCreateContext())
  }

  def init(): Unit = ()
  def initFuture(): Future[Unit] = Future.successful(())

  def exit(): Unit = ()
  def exitFuture(): Future[Unit] = Future.successful(())

  override def start(): Unit = {
    super.start()
    setup()
    init()
  }

  override def stop(): Unit = {
    exit()
    super.stop()
  }

  override def start(complete: VxFuture[Void]): Unit = {
    val promise = VxFuture.future[Void]()
    super.start(promise)
    promise.compose { _ => Futures.toJava(initFuture()).asInstanceOf[VxFuture[Void]] }
      .setHandler(complete)
  }

  override def stop(complete: VxFuture[Void]): Unit = {
    val promise = VxFuture.future[Void]()
    super.stop(promise)
    promise.compose { _ => Futures.toJava(initFuture()).asInstanceOf[VxFuture[Void]] }
      .setHandler(complete)
  }
  */
}
