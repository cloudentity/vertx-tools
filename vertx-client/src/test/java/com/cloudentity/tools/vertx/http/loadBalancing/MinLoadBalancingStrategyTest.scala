package com.cloudentity.tools.vertx.http.loadBalancing

import com.cloudentity.tools.vertx.http.circuit._
import com.cloudentity.tools.vertx.sd.{Location, Node, ServiceName}
import io.vertx.circuitbreaker.{CircuitBreaker, CircuitBreakerState}
import io.vertx.core.{Future, Handler, Promise}
import org.junit.Test
import org.scalatest.MustMatchers
import org.scalatest.junit.AssertionsForJUnit

class MinLoadBalancingStrategyTest extends AssertionsForJUnit with MustMatchers {
  val strategy = LoadBalancingStrategy.minLoad
  val nodeTemplate = Node(ServiceName("service"), null, Location("localhost", 80, false, None))

  @Test def shouldReturnNoneWhenNoNodes(): Unit = {
    // given
    val nodes = Vector()

    // when then
    strategy.run(nodes) must be((nodes, None))
  }

  @Test def shouldReturnNoneWhenAllNodesWithOpenCircuitBreaker(): Unit = {
    // given
    val node = nodeTemplate.copy(cb = cbWithFixedLoadEvaluator(100, CircuitBreakerState.OPEN))
    val nodes = Vector(node, node)

    // when then
    strategy.run(nodes) must be((nodes, None))
    strategy.run(nodes :+ node) must be((nodes :+ node, None)) // with 2 nodes with open circuit
  }

  @Test def shouldReturnNodeIfClosedCircuitBreaker(): Unit = {
    // given
    val cb = new NoopCB("") { override def state() = CircuitBreakerState.CLOSED }
    val node = nodeTemplate.copy(cb = cb)
    val nodes = Vector(node)

    // when then
    strategy.run(nodes) must be((nodes, Some(node)))
  }

  @Test def shouldReturnNodeIfHalfOpenCircuitBreaker(): Unit = {
    // given
    val cb = new NoopCB("") { override def state() = CircuitBreakerState.HALF_OPEN }
    val node = nodeTemplate.copy(cb = cb)
    val nodes = Vector(node)

    // when then
    strategy.run(nodes) must be((nodes, Some(node)))
  }

  @Test def shouldReturnNodeWithMinimalLoadIfNotOpenCircuitBreaker(): Unit = {
    // given
    val node1 = nodeTemplate.copy(cb = cbWithFixedLoadEvaluator(200, CircuitBreakerState.CLOSED))
    val node2 = nodeTemplate.copy(cb = cbWithFixedLoadEvaluator(250, CircuitBreakerState.CLOSED))
    val node3 = nodeTemplate.copy(cb = cbWithFixedLoadEvaluator(100, CircuitBreakerState.CLOSED))

    val nodes = Vector(node1, node2, node3)

    // when then
    strategy.run(nodes) must be((Vector(node3, node1, node2), Some(node3)))
  }

  def fixedLoadEvaluator(_loadValue: Double): CircuitBreakerLoadEvaluator =
    new CircuitBreakerLoadEvaluator {
      override def loadValue: Double = _loadValue
      override def executeAndEval[T](cb: CircuitBreaker)(command: Handler[Promise[T]]): Future[T] = cb.execute(command)
    }

  def cbWithFixedLoadEvaluator(_loadValue: Double, _state: CircuitBreakerState): CircuitBreaker =
    new CircuitBreakerWithLoadEvaluator(
      new NoopCB("") { override def state() = _state },
      fixedLoadEvaluator(_loadValue)
    )
}
