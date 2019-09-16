package com.cloudentity.tools.vertx.sd.loadBalancing

import com.cloudentity.tools.vertx.sd.Node
import com.cloudentity.tools.vertx.sd.circuit.CircuitBreakerWithLoadEvaluator
import io.vertx.circuitbreaker.CircuitBreakerState

object LoadBalancingStrategy {
  val roundRobin = LoadBalancingStrategy { nodes =>
    val (open, notOpen) = nodes.span(_.cb.state() == CircuitBreakerState.OPEN)

    notOpen match {
      case firstNotOpen +: tail =>
        ((tail ++ open.reverse) :+ firstNotOpen, Some(firstNotOpen))
      case Vector() =>
        (nodes, None)
    }
  }

  val minLoad = LoadBalancingStrategy { ns =>
    val sorted = ns.sortBy { n =>
      val cb = n.cb.asInstanceOf[CircuitBreakerWithLoadEvaluator]
      cb.loadEval.loadValue
    }

    (sorted, sorted.filter(_.cb.state() != CircuitBreakerState.OPEN).headOption)
  }
}

/**
  * Run returns updated Nodes vector and optional Node that was picked by the strategy.
  */
case class LoadBalancingStrategy(run: Vector[Node] => (Vector[Node], Option[Node]))
