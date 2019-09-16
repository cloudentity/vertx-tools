package com.cloudentity.tools.vertx.sd.circuit

import java.lang
import java.util.function

import io.vertx.circuitbreaker.{CircuitBreaker, CircuitBreakerState}
import io.vertx.core.{Future, Handler, Promise}

class NoopCB(_name: String) extends CircuitBreaker {
  override def executeAndReportWithFallback[T](promise: Promise[T], command: Handler[Promise[T]], fallback: function.Function[Throwable, T]): CircuitBreaker = {
    command.handle(promise)
    this
  }

  override def executeWithFallback[T](command: Handler[Promise[T]], fallback: function.Function[Throwable, T]): Future[T] = {
    val p = Promise.promise[T]()
    executeAndReportWithFallback(p, command, null)
    p.future()
  }

  override def execute[T](command: Handler[Promise[T]]): Future[T] =
    executeWithFallback(command, null)

  override def executeAndReport[T](promise: Promise[T], command: Handler[Promise[T]]): CircuitBreaker =
    executeAndReportWithFallback(promise, command, null)

  override def closeHandler(handler: Handler[Void]): CircuitBreaker = this
  override def openHandler(handler: Handler[Void]): CircuitBreaker = this
  override def halfOpenHandler(handler: Handler[Void]): CircuitBreaker = this
  override def close(): CircuitBreaker = this
  override def state(): CircuitBreakerState = CircuitBreakerState.CLOSED
  override def name(): String = _name
  override def reset(): CircuitBreaker = this
  override def fallback[T](handler: function.Function[Throwable, T]): CircuitBreaker = this
  override def open(): CircuitBreaker = this
  override def failureCount(): Long = 0

  override def retryPolicy(retryPolicy: function.Function[Integer, lang.Long]): CircuitBreaker = this
}