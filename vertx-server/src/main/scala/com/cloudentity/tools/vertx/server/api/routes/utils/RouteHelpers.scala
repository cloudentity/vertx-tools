package com.cloudentity.tools.vertx.server.api.routes.utils

import com.cloudentity.tools.vertx.scala.bus.ScalaServiceVerticle
import com.cloudentity.tools.vertx.scala.{FutureConversions, Futures, Operation}
import com.cloudentity.tools.vertx.server.api.ResponseHelpers.respondWithError
import com.cloudentity.tools.vertx.server.http._
import HttpStatus._
import com.cloudentity.tools.vertx.http.Headers
import com.cloudentity.tools.vertx.http.headers.HeadersSyntax
import com.cloudentity.tools.vertx.server.api.errors.ApiError
import com.cloudentity.tools.vertx.server.api.tracing.RoutingWithTracing
import com.cloudentity.tools.vertx.tracing.LoggingWithTracing
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.RoutingContext

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.Try
import scalaz.{-\/, \/-}

trait BaseRouteHelpers extends FutureConversions with HeadersSyntax with ServerConversions with ClientConversions { this: ScalaServiceVerticle =>
  val OK                          = HttpStatus.OK
  val Created                     = HttpStatus.Created
  val Accepted                    = HttpStatus.Accepted
  val NonAuthoritativeInformation = HttpStatus.NonAuthoritativeInformation
  val NoContent                   = HttpStatus.NoContent
  val ResetContent                = HttpStatus.ResetContent
  val PartialContent              = HttpStatus.PartialContent
  
  private val log: LoggingWithTracing = LoggingWithTracing.getLogger(this.getClass)

  def unauthenticatedRequest(msg: String) = ApiError.`with`(401, "Unauthenticated", msg)
  def unauthorizedRequest(msg: String) = ApiError.`with`(403, "Unauthorized", msg)
  def invalidRequest(msg: String) = ApiError.`with`(400, "InvalidRequest", msg)
  val internalServerError         = ApiError.`with`(500, "InternalServerError", "Internal Server Error")

  def apiError(statusCode: Int, code: String, msg: String) = ApiError.`with`(statusCode, code, msg)
  def apiError[A](statusCode: Int, code: String, msg: String, details: A)(implicit ec: VertxEncoder[A]) = ApiError.withEncodedDetails(statusCode, code, msg, ec.encode(details))
  def apiResponse[A](body: A, status: HttpStatusWithBody, headers: Headers): ApiResponse[A] = ApiResponseWithBody(status, body, headers)
  def apiResponse[A](body: A, status: HttpStatusWithBody): ApiResponse[A] = ApiResponseWithBody(status, body, Headers().withContentType.applicationJson)
  def apiResponse(status: HttpStatusWithoutBody, headers: Headers): ApiResponse[Unit] = ApiResponseWithoutBody(status, headers)
  def apiResponse(status: HttpStatusWithoutBody): ApiResponse[Unit] = ApiResponseWithoutBody(status, Headers().withContentType.applicationJson)

  sealed trait ApiResponse[+A]
    case class ApiResponseWithBody[A](status: HttpStatusWithBody, body: A, headers: Headers = Headers().withContentType.applicationJson) extends ApiResponse[A]
    case class ApiResponseWithoutBody(status: HttpStatusWithoutBody, headers: Headers = Headers().withContentType.applicationJson) extends ApiResponse[Unit]

  implicit lazy val UnitVertxEncoder: VertxEncoder[Unit] = _ => ""

  protected def end(ctx: RoutingContext, statusCode: Int, headers: Headers, body: String): Unit =
    setHeaders(ctx.response(), headers)
      .setStatusCode(statusCode)
      .end(body)

  protected def end(ctx: RoutingContext, statusCode: Int, headers: Headers): Unit =
    setHeaders(ctx.response(), headers)
      .setStatusCode(statusCode)
      .end()

  private def setHeaders(resp: HttpServerResponse, headers: Headers): HttpServerResponse = {
    headers.toMap.foreach { case (name, values) =>
      import scala.collection.JavaConverters._
      resp.headers().add(name, values.asJava)
    }
    resp
  }

  def handleComplete[A](ctx: RoutingContext, successStatus: HttpStatusWithBody, headers: Headers = Headers().withContentType.applicationJson)(program: VxFuture[ApiError \/ A])
                       (implicit encoder: VertxEncoder[A]): Unit =
    complete(ctx, program)(a => end(ctx, successStatus.asInt, headers, encoder.encode(a)))

  def handleResponse[A](ctx: RoutingContext)(program: VxFuture[ApiError \/ ApiResponse[A]])
                       (implicit encoder: VertxEncoder[A]): Unit =
    complete(ctx, program) {
      case ApiResponseWithBody(status, body, headers) =>
        end(ctx, status.asInt, headers, encoder.encode(body))
      case ApiResponseWithoutBody(status, headers) =>
        end(ctx, status.asInt, headers)
    }

  def handleResponseS[A](ctx: RoutingContext)(program: Future[ApiError \/ ApiResponse[A]])
                                   (implicit encoder: VertxEncoder[A]): Unit =
    handleResponse[A](ctx)(Futures.toJava(program))

  def handleCompleteS[A](ctx: RoutingContext, successStatus: HttpStatusWithBody, headers: Headers = Headers().withContentType.applicationJson)(program: Future[ApiError \/ A])
                        (implicit encoder: VertxEncoder[A]): Unit =
    handleComplete(ctx, successStatus, headers)(Futures.toJava(program))

  def handleCompleteWithHeaders[A](ctx: RoutingContext, successStatus: HttpStatusWithBody)(program: VxFuture[ApiError \/ (A, Headers)])
                       (implicit encoder: VertxEncoder[A]): Unit =
    complete(ctx, program) { case (a, headers) => end(ctx, successStatus.asInt, headers, encoder.encode(a)) }

  def handleCompleteWithHeadersS[A](ctx: RoutingContext, successStatus: HttpStatusWithBody)(program: Future[ApiError \/ (A, Headers)])
                        (implicit encoder: VertxEncoder[A]): Unit =
    complete(ctx, Futures.toJava(program)) { case (a, headers) => end(ctx, successStatus.asInt, headers, encoder.encode(a)) }

  def handleCompleteNoBody[A](ctx: RoutingContext, successStatus: HttpStatusWithoutBody, headers: Headers = Headers())(program: VxFuture[ApiError \/ A]): Unit =
    complete(ctx, program)(_ => end(ctx, successStatus.asInt, headers))

  def handleCompleteNoBodyS[A](ctx: RoutingContext, successStatus: HttpStatusWithoutBody, headers: Headers = Headers())(program: Future[ApiError \/ A]): Unit =
    handleCompleteNoBody(ctx, successStatus, headers)(Futures.toJava(program))

  def handleCompleteNoBodyWithHeaders[A](ctx: RoutingContext, successStatus: HttpStatusWithoutBody)(program: VxFuture[ApiError \/ (A, Headers)]): Unit =
    complete(ctx, program) { case (_, headers ) => end(ctx, successStatus.asInt, headers) }

  def handleCompleteNoBodyWithHeadersS[A](ctx: RoutingContext, successStatus: HttpStatusWithoutBody, headers: Headers = Headers())(program: Future[ApiError \/ (A, Headers)]): Unit =
    complete(ctx, Futures.toJava(program)) { case (_, headers ) => end(ctx, successStatus.asInt, headers) }

  private def complete[A](ctx: RoutingContext, program: VxFuture[ApiError \/ A])(successEnd: A => Unit): Unit =
    program.setHandler { async =>
      if (async.succeeded()) {
        async.result() match {
          case \/-(a) =>
            successEnd(a)

          case -\/(error) =>
            val tracingCtx = RoutingWithTracing.getOrCreate(ctx, getTracing)
            log.warn(tracingCtx, s"Request failed with error: $error")
            tracingCtx.logError(error)
            respondWithError(ctx, error)
        }
      } else {
        val tracingCtx = RoutingWithTracing.getOrCreate(ctx, getTracing)
        log.error(tracingCtx, s"Request failed with exception", async.cause())
        tracingCtx.logError(async.cause())
        respondWithError(ctx, internalServerError)
      }
    }
}

trait RouteContinuations extends BaseRouteHelpers with FutureConversions { this: ScalaServiceVerticle =>
  private val log: LoggingWithTracing = LoggingWithTracing.getLogger(this.getClass)

  def withBody[A](ctx: RoutingContext)(handler: A => Unit)(implicit decoder: VertxDecoder[A]): Unit =
    decoder.decode(ctx.getBodyAsString) match {
      case Right(body) => handler(body)
      case Left(error) =>
        val tracingCtx = RoutingWithTracing.getOrCreate(ctx, getTracing)
        log.debug(tracingCtx, s"Failed to parse body: $error")
        tracingCtx.logError(error)
        respondWithError(ctx, invalidRequest(error.msg))
    }

  def withPathParam(ctx: RoutingContext, paramName: String)(handler: String => Unit): Unit =
    Option(ctx.pathParam(paramName)) match {
      case Some(param) => handler(param)
      case None =>
        val error = s"Missing path param: $paramName"
        val tracingCtx = RoutingWithTracing.getOrCreate(ctx, getTracing)
        log.debug(tracingCtx, error)
        tracingCtx.logError(error)
        respondWithError(ctx, invalidRequest(error))
    }

  def withHeader(ctx: RoutingContext, headerName: String, apiError: => ApiError)(handler: String => Unit): Unit =
    Option(ctx.request.getHeader(headerName)) match {
      case Some(header) => handler(header)
      case None =>
        val error = s"Missing header: $headerName"
        val tracingCtx = RoutingWithTracing.getOrCreate(ctx, getTracing)
        log.debug(tracingCtx, error)
        tracingCtx.logError(error)
        respondWithError(ctx, apiError)
    }

  def withHeader(ctx: RoutingContext, headerName: String)(handler: String => Unit): Unit =
    withHeader(ctx, headerName, invalidRequest(s"Missing header: $headerName"))(handler)
}

trait RouteOperations extends BaseRouteHelpers { this: ScalaServiceVerticle =>
  private val log: LoggingWithTracing = LoggingWithTracing.getLogger(this.getClass)

  def getBody[A](ctx: RoutingContext)(implicit decoder: VertxDecoder[A]): Operation[ApiError, A] =
    decoder.decode(ctx.getBodyAsString) match {
      case Right(body) => Operation.success(body)
      case Left(error) =>
        val tracingCtx = RoutingWithTracing.getOrCreate(ctx, getTracing)
        log.debug(tracingCtx, s"Failed to parse body: $error")
        tracingCtx.logError(error)
        Operation.error(invalidRequest(error.msg))
    }

  def getPathParam(ctx: RoutingContext, paramName: String): Operation[ApiError, String] =
    Option(ctx.pathParam(paramName)) match {
      case Some(param) => Operation.success(param)
      case None =>
        val error = s"Missing path param: $paramName"
        val tracingCtx = RoutingWithTracing.getOrCreate(ctx, getTracing)
        log.debug(tracingCtx, error)
        tracingCtx.logError(error)
        Operation.error(invalidRequest(error))
    }

  def getHeader(ctx: RoutingContext, headerName: String, apiError: => ApiError): Operation[ApiError, String] =
    Option(ctx.request.getHeader(headerName)) match {
      case Some(header) => Operation.success(header)
      case None =>
        val error = s"Missing header: $headerName"
        val tracingCtx = RoutingWithTracing.getOrCreate(ctx, getTracing)
        log.debug(tracingCtx, error)
        tracingCtx.logError(error)
        Operation.error(apiError)
    }

  def getHeader(ctx: RoutingContext, headerName: String): Operation[ApiError, String] =
    getHeader(ctx, headerName, invalidRequest(s"Missing header: $headerName"))

  def getCtxVariable[A](ctx: RoutingContext, variableName: String, apiError: ApiError = internalServerError): Operation[ApiError, A] = {
    val variable = ctx.get[Any](variableName)
    Try(Option(variable.asInstanceOf[A])).toOption.flatten match {
      case Some(a) =>
        Operation.success(a)
      case None =>
        val error = s"Could not retrieve '$variableName' variable from RoutingContext. Got '$variable'"
        val tracingCtx = RoutingWithTracing.getOrCreate(ctx, getTracing)
        log.error(tracingCtx, error)
        tracingCtx.logError(error)
        Operation.error(apiError)
    }
  }
}