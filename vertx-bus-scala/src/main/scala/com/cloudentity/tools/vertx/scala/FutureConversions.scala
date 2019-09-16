package com.cloudentity.tools.vertx.scala

import io.vertx.core.{AsyncResult, Future, Handler}

trait FutureConversions {
  implicit class ToScalaFuture[A](f: io.vertx.core.Future[A])(implicit ec: VertxExecutionContext) {
    def toScala() = Futures.toScala(f)
  }

  implicit class ToVertxFuture[A](f: scala.concurrent.Future[A])(implicit ec: VertxExecutionContext) {
    def toJava() = Futures.toJava(f)
  }

  def asFuture[A](f: Handler[AsyncResult[A]] => Unit): Future[A] = {
    val promise = Future.future[A]()

    f { async =>
      if (async.succeeded()) promise.complete(async.result())
      else promise.fail(async.cause())
    }

    promise
  }
}
