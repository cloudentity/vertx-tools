package com.cloudentity.tools.vertx.server.http

sealed trait HttpStatus { def asInt: Int }

object HttpStatus {
  sealed trait HttpStatusWithBody extends HttpStatus
  sealed trait HttpStatusWithoutBody extends HttpStatus

  case object OK extends HttpStatusWithBody with HttpStatusWithoutBody { def asInt = 200 }
  case object Created extends HttpStatusWithBody with HttpStatusWithoutBody { def asInt = 201 }
  case object Accepted extends HttpStatusWithBody with HttpStatusWithoutBody { def asInt = 202 }
  case object NonAuthoritativeInformation extends HttpStatusWithBody with HttpStatusWithoutBody { def asInt = 203 }
  case object NoContent extends HttpStatusWithoutBody { def asInt = 204 }
  case object ResetContent extends HttpStatusWithoutBody { def asInt = 205 }
  case object PartialContent extends HttpStatusWithBody with HttpStatusWithoutBody { def asInt = 206 }
}