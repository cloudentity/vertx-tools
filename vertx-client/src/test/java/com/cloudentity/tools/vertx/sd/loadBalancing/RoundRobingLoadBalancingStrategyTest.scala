package com.cloudentity.tools.vertx.sd.loadBalancing

import java.util.function

import com.cloudentity.tools.vertx.sd.circuit.NoopCB
import com.cloudentity.tools.vertx.sd.{Location, Node, ServiceName}
import io.vertx.circuitbreaker.{CircuitBreaker, CircuitBreakerState}
import io.vertx.core.{Future, Handler}
import org.junit.Test
import org.scalatest.MustMatchers
import org.scalatest.junit.AssertionsForJUnit

class RoundRobingLoadBalancingStrategyTest extends AssertionsForJUnit with MustMatchers {
  val strategy = LoadBalancingStrategy.roundRobin
  val nodeTemplate = Node(ServiceName("service"), null, Location("localhost", 80, false, None))

  @Test def shouldReturnNoneWhenNoNodes(): Unit = {
    // given
    val nodes = Vector()

    // when then
    strategy.run(nodes) must be((nodes, None))
  }

  @Test def shouldReturnNoneWhenAllNodesWithOpenCircuitBreaker(): Unit = {
    // given
    val cb = new NoopCB("") { override def state() = CircuitBreakerState.OPEN }
    val node = nodeTemplate.copy(cb = cb)
    val nodes = Vector(node)

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

  @Test def shouldReturnNodeAndPushItBackToQueueIfNotOpenCircuitBreaker(): Unit = {
    // given
    val cb = new NoopCB("") { override def state() = CircuitBreakerState.CLOSED }
    val node1 = nodeTemplate.copy(cb = cb)
    val node2 = nodeTemplate.copy(cb = cb)
    val node3 = nodeTemplate.copy(cb = cb)

    val nodes = Vector(node1, node2, node3)

    // when then
    strategy.run(nodes) must be((Vector(node2, node3, node1), Some(node1)))
  }
}
