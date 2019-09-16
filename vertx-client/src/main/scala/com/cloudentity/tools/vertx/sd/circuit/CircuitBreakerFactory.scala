package com.cloudentity.tools.vertx.sd.circuit

import com.cloudentity.tools.vertx.sd.{NodeId, ServiceName}
import io.vertx.circuitbreaker.{CircuitBreaker, CircuitBreakerOptions}
import io.vertx.core.json.JsonObject
import io.vertx.core.{Future, Vertx}
import org.slf4j.LoggerFactory

import scala.util.Try

object CircuitBreakerFactory {
  val log = LoggerFactory.getLogger(this.getClass)

  def fromOpts(vertx: Vertx, name: ServiceName, opts: CircuitBreakerOptions) =
    CircuitBreakerFactory(id => CircuitBreaker.create(s"${name.value}:${id.value}", vertx, opts))

  def fromConfig(vertx: Vertx, name: ServiceName, confOpt: Option[JsonObject]): Future[CircuitBreakerFactory] =
    confOpt match {
      case Some(conf) =>
        if (conf.getBoolean("off")) Future.succeededFuture(noop())
        else Try(new CircuitBreakerOptions(conf)) match {
          case scala.util.Success(opts) => Future.succeededFuture(fromOpts(vertx, name, opts))
          case scala.util.Failure(ex)   => Future.failedFuture(ex)
        }
      case None =>
        Future.succeededFuture(noop)
    }

  def noop() = CircuitBreakerFactory(id => new NoopCB(id.value))
}

case class CircuitBreakerFactory(build: NodeId => CircuitBreaker)
