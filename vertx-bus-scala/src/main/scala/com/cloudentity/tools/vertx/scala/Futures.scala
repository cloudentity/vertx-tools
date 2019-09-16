package com.cloudentity.tools.vertx.scala

import io.vertx.core.{Future => VxFuture}

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

object Futures {
  def toScala[A](java: VxFuture[A])(implicit ec: VertxExecutionContext): Future[A] = {
    val promise = Promise[A]()

    java.setHandler { event =>
      if (event.failed())
        promise.failure(event.cause())
      else {
        try {
          promise.success(event.result())
        }
        catch {
          case npe: NullPointerException => promise.failure(npe)
        }
      }
    }

    promise.future
  }

  def toJava[A](scala: Future[A])(implicit ec: VertxExecutionContext): VxFuture[A] = {
    val vx = VxFuture.future[A]()
    scala.onComplete {
      case Success(a)  =>
        ec.ctx.runOnContext(_ => vx.complete(a))
      case Failure(ex) =>
        ec.ctx.runOnContext(_ => vx.fail(ex))
    }
    vx
  }
}

object FutureOps {
  implicit class ToJavaFuture[A](scala: Future[A])(implicit ec: VertxExecutionContext) {
    def asJava: VxFuture[A] = Futures.toJava(scala)
  }

  implicit class ToScalaFuture[A](java: VxFuture[A])(implicit ec: VertxExecutionContext) {
    def asScala: Future[A] = Futures.toScala(java)
  }
}
