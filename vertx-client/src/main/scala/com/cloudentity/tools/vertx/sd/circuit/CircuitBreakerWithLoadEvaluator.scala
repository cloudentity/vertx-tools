package com.cloudentity.tools.vertx.sd.circuit

import java.lang
import java.util.function

import io.vertx.circuitbreaker.{CircuitBreaker, CircuitBreakerState}
import io.vertx.core.{Future, Handler, Promise}

/**
  * Decorates `execute` method of provided CircuitBreaker with load evaluator that is used for load balancing.
  */
class CircuitBreakerWithLoadEvaluator(cb: CircuitBreaker, val loadEval: CircuitBreakerLoadEvaluator) extends CircuitBreaker {
  override def execute[T](command: Handler[Promise[T]]): Future[T] = loadEval.executeAndEval(cb)(command)

  override def executeAndReport[T](promise: Promise[T], handler: Handler[Promise[T]]): CircuitBreaker = cb.executeAndReport(promise, handler)
  override def executeAndReportWithFallback[T](promise: Promise[T], command: Handler[Promise[T]], fallback: function.Function[Throwable, T]): CircuitBreaker = cb.executeAndReportWithFallback(promise, command, fallback)
  override def executeWithFallback[T](command: Handler[Promise[T]], fallback: function.Function[Throwable, T]): Future[T] = cb.executeWithFallback(command, fallback)
  override def halfOpenHandler(handler: Handler[Void]): CircuitBreaker = cb.halfOpenHandler(handler)
  override def state(): CircuitBreakerState = cb.state()
  override def close(): CircuitBreaker = cb.close()
  override def closeHandler(handler: Handler[Void]): CircuitBreaker = cb.closeHandler(handler)
  override def openHandler(handler: Handler[Void]): CircuitBreaker = cb.openHandler(handler)
  override def name(): String = cb.name()
  override def reset(): CircuitBreaker = cb.reset()
  override def fallback[T](handler: function.Function[Throwable, T]): CircuitBreaker = cb.fallback(handler)
  override def open(): CircuitBreaker = cb.open()
  override def failureCount(): Long = cb.failureCount()

  override def retryPolicy(retryPolicy: function.Function[Integer, lang.Long]): CircuitBreaker = this
}

abstract class CircuitBreakerLoadEvaluator {
  def loadValue: Double
  def executeAndEval[T](cb: CircuitBreaker)(command: Handler[Promise[T]]): Future[T]
}

/**
  * Calculates how many execution circuit-breaker is handling at given point in time.
  */
class CountingCircuitBreakerLoadEvaluator extends CircuitBreakerLoadEvaluator {
  var activeExecutions = 0

  override def loadValue: Double = activeExecutions

  override def executeAndEval[T](cb: CircuitBreaker)(command: Handler[Promise[T]]): Future[T] = {
    val f = Future.future[T]
    activeExecutions += 1
    cb.execute(command).setHandler { res =>
      activeExecutions -= 1
      f.handle(res)
    }
    f
  }
}

/**
  * Calculates rolling average of circuit-breaker execution times.
  */
class RollingTimeAvgCircuitBreakerLoadEvaluator[T](windowSize: Int) extends CircuitBreakerLoadEvaluator {
  var avgTime = 0.0

  override def loadValue: Double = avgTime

  import RollingAvg._
  override def executeAndEval[T](cb: CircuitBreaker)(command: Handler[Promise[T]]): Future[T] = {
    val f = Future.future[T]
    val t0 = System.currentTimeMillis()
    cb.execute(command).setHandler { res =>
      val t = System.currentTimeMillis() - t0
      avgTime = rollingAvg(avgTime, t, windowSize)

      f.handle(res)
    }
    f
  }
}

/**
  * Calculates rolling average of circuit-breaker executions times and counts number of active executions.
  *
  * It evaluates circuit-breaker load taking both factors into account.
  */
class RollingTimeAvgWithCountingCircuitBreakerLoadEvaluator[T](windowSize: Int, activeCallsCoeff: Int) extends CircuitBreakerLoadEvaluator {
  var avgTime = 0.0
  var activeExecutions = 0

  override def loadValue: Double = avgTime * Math.min(1, activeExecutions / activeCallsCoeff)

  import RollingAvg._
  override def executeAndEval[T](cb: CircuitBreaker)(command: Handler[Promise[T]]): Future[T] = {
    val f = Future.future[T]
    val t0 = System.currentTimeMillis()
    activeExecutions += 1
    cb.execute(command).setHandler { res =>
      val t = System.currentTimeMillis() - t0
      avgTime = rollingAvg(avgTime, t, windowSize)
      activeExecutions -= 1

      f.handle(res)
    }
    f
  }
}

object RollingAvg {
  /**
    * to verify that rollingAvg actually works execute `rollingAvgTest`. it prints avg from last `window` samples from `xs`
    *
    * def rollingAvgTest(xs: List[Int], window: Int) = {
    *   xs.foldLeft(0) { case (avg, x) =>
    *     val newAvg = movingAvg(avg, x, 3)
    *     println(x, newAvg)
    *     newAvg
    *   }
    * }
    */
  def rollingAvg(avg: Double, x: Double, window: Int) = avg - avg / window + x / window
}