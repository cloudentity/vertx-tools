package com.cloudentity.tools.vertx.server.api.routes.utils

import com.cloudentity.tools.vertx.http.Headers
import com.cloudentity.tools.vertx.scala.Operation
import com.cloudentity.tools.vertx.server.api.errors.ApiError
import com.cloudentity.tools.vertx.server.api.routes.{RouteService, ScalaRouteVerticle}
import io.vertx.ext.web.RoutingContext

import scala.concurrent.Future
import scalaz.\/-

class RouteWithOperationsAndApiError extends ScalaRouteVerticle with CirceRouteOperations {
  import io.circe.generic.auto._

  case class User(name: String)
  case class Result(success: Boolean)

  override protected def handle(ctx: RoutingContext): Unit = {
    val program: Future[ApiError \/ Result] = {
      for {
        id      <- getPathParam(ctx, "id")
        user    <- getBody[User](ctx)
        _       <- validateUser(user)
        updated <- updateName(id, user.name).toOperation
      } yield Result(updated)
    }.run

    handleCompleteS(ctx, OK)(program)
  }

  private def validateUser(user: User): Operation[ApiError, Unit] =
    if (user.name.length < 5)
      Operation.error(apiError(400, "CODE_TOO_SHORT", "User code is too short"))
    else Operation.success(())

  def updateName(id: String, name: String): Future[Boolean] = Future.successful(true)
}

class RouteWithOperationsAndHeaders extends ScalaRouteVerticle with CirceRouteOperations {
  import io.circe.generic.auto._

  case class User(name: String)
  case class Result(success: Boolean)

  override protected def handle(ctx: RoutingContext): Unit = {
    val program: Future[ApiError \/ (Result, Headers)] = {
      for {
        id      <- getPathParam(ctx, "id")
        user    <- getBody[User](ctx)
        updated <- updateName(id, user.name).toOperation
      } yield (Result(updated), Headers())
    }.run

    handleCompleteWithHeadersS(ctx, OK)(program)
  }

  def updateName(id: String, name: String): Future[Boolean] = Future.successful(true)
}

class RouteWithOperationsAndApiResponse extends ScalaRouteVerticle with CirceRouteOperations {
  import io.circe.generic.auto._

  case class User(name: String)
  case class Result(success: Boolean)

  override protected def handle(ctx: RoutingContext): Unit = {
    val program: Future[ApiError \/ ApiResponse[Result]] = {
      for {
        id      <- getPathParam(ctx, "id")
        user    <- getBody[User](ctx)
        updated <- updateName(id, user.name).toOperation
      } yield apiResponse(Result(updated), OK)
    }.run

    handleResponseS(ctx)(program)
  }

  def updateName(id: String, name: String): Future[Boolean] = Future.successful(true)
}

sealed trait ProcessingError
case object StorageError extends ProcessingError

object ProcessingError {
  implicit val ProcessingErrorToApiError: ToApiError[ProcessingError] = {
    case StorageError => ApiError.`with`(409, "StorageError", "Storage error")
  }
}

class RouteWithOperationsAndADT extends ScalaRouteVerticle with CirceRouteOperations {
  import io.circe.generic.auto._

  case class User(name: String)
  case class ValidationError(minLength: Int)
  case class Result(success: Boolean)

  override protected def handle(ctx: RoutingContext): Unit = {
    val program: Future[ApiError \/ Result] = {
      for {
        id      <- getPathParam(ctx, "id")
        user    <- getBody[User](ctx)
        _       <- validateUser(user)
        updated <- updateName(id, user.name).toOperation.convertError
      } yield Result(updated)
    }.run

    handleCompleteS(ctx, OK)(program)
  }

  private def validateUser(user: User): Operation[ApiError, Unit] =
    if (user.name.length < 5)
      Operation.error(apiError(400, "CODE_TOO_SHORT", "User code is too short", ValidationError(5)))
    else Operation.success(())

  def updateName(id: String, name: String): Future[ProcessingError \/ Boolean] = Future.successful(\/-(true))
}

class RouteWithContinuations extends ScalaRouteVerticle with CirceRouteContinuations {
  import io.circe.generic.auto._

  case class User(name: String)
  case class Result(success: Boolean)

  override protected def handle(ctx: RoutingContext): Unit =
    withPathParam(ctx, "id") { id =>
      withBody[User](ctx) { user =>
        val program: Future[Result] =
          for {
            updated <- updateName(id, user.name)
          } yield (Result(updated))

        handleCompleteS(ctx, OK)(program.convertError)
      }
    }

  def updateName(id: String, name: String): Future[Boolean] = Future.successful(true)
}

class RouteWithContinuationsWithADT extends ScalaRouteVerticle with CirceRouteContinuations {
  import io.circe.generic.auto._

  case class User(name: String)
  case class Result(success: Boolean)

  override protected def handle(ctx: RoutingContext): Unit =
    withPathParam(ctx, "id") { id =>
      withBody[User](ctx) { user =>
        val program: Future[ProcessingError \/ Result] = {
          for {
            updated <- updateName(id, user.name).toOperation
          } yield (Result(updated))
        }.run

        handleCompleteS(ctx, OK)(program.convertError)
      }
    }

  def updateName(id: String, name: String): Future[ProcessingError \/ Boolean] = Future.successful(\/-(true))
}


