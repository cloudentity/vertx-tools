package com.cloudentity.tools.vertx.registry

import com.cloudentity.tools.vertx.scala.FutureConversions
import com.cloudentity.tools.vertx.scala.VertxExecutionContext
import com.cloudentity.tools.vertx.verticles.VertxUtil
import io.vertx.core.Vertx

import scala.concurrent.Future
import scala.concurrent.duration._

object TestTools extends FutureConversions {
  /**
    * This method executes provided async `action` up to `attempts` times in equal time intervals.
    * At each `action` execution its result is verified with `verify`. If the verification throws an exception
    * the action is executed again. Otherwise, if verification raised no exception then the `retest` ends successfully.
    * If the `action` is executed max no. of `attempts` then the provided TestContext is failed with last exception from
    * the `verify` method.
    */
  def retest[A](vertx: Vertx, attempts: Int, every: Duration, action: () => Future[A])
                  (verify: A => Unit)(implicit ec: VertxExecutionContext): Future[Unit] =
    retestRec(vertx, attempts, every,  action)(verify)

  def retestRec[A](vertx: Vertx, attempts: Int, every: Duration, action: () => Future[A])
               (verify: A => Unit)(implicit ec: VertxExecutionContext): Future[Unit] = {

      action().map(verify).recoverWith { case ex: Throwable =>
        if (attempts > 0) {
          VertxUtil.executeBlocking(vertx, () => Thread.sleep(Math.max(0, every.toMillis))).toScala
            .flatMap { _ =>
              retestRec(vertx, attempts - 1, every, action)(verify)
            }
        } else {
          Future.failed[Unit](ex)
        }
      }
  }
}
