package com.cloudentity.tools.vertx.scala

import com.cloudentity.tools.vertx.scala.syntax._
import io.vertx.core.Vertx

sealed trait ClientError
final case class FatalClientError(status: Int) extends ClientError

sealed trait AppError
final case class FatalExternalError(error: ClientError) extends AppError

class OperationSpec {
  implicit val ex: VertxExecutionContext = VertxExecutionContext(Vertx.vertx().getOrCreateContext())

  Operation.success[ClientError, String]("a").leftMap[AppError](FatalExternalError.apply)
}
