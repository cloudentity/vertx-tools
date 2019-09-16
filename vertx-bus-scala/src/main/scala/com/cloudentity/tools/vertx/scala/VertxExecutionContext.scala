package com.cloudentity.tools.vertx.scala

import io.vertx.core.Context
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

/**
  * A scala [[scala.concurrent.ExecutionContext]] binds [[scala.concurrent.Promise]]/[[scala.concurrent.Future]] to a thread.
  * In the case of Vert.x we have to make sure that they execute on the right context. A context could be that
  * of a worker or a standard verticle. This execution context implementation runs all Runnables on the associated
  * [[io.vertx.core.Context]].
  *
  * Taken from the original Vert.x-impl
  * https://github.com/vert-x/mod-lang-scala/blob/master/src/main/scala/org/vertx/scala/core/VertxExecutionContext.scala
  *
  */
class VertxExecutionContext(val ctx:Context) extends ExecutionContext{
  private val Log = LoggerFactory.getLogger(classOf[VertxExecutionContext].getName)

  override def execute(runnable: Runnable): Unit = {
    ctx.runOnContext((_:Void) => runnable.run())
  }

  override def reportFailure(cause: Throwable): Unit = {
    Log.error("Failed executing on contet", cause)
  }
}

object VertxExecutionContext {
  def apply(ctx: Context): VertxExecutionContext = new VertxExecutionContext(ctx)
}
