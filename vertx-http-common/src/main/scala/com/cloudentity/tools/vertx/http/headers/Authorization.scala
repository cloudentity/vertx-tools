package com.cloudentity.tools.vertx.http.headers

import com.cloudentity.tools.vertx.http.Headers

trait AuthorizationSyntax {
  import Authorization._
  protected def hs: Headers

  def withAuthorization = AuthorizationBuilder(hs)
  def withAuthorization(a: Authorization) =
    hs.set(AUTHORIZATION, a.value)

  def authorizationRaw: Option[String] =
    hs.get(AUTHORIZATION)

  def authorization: Option[Authorization] =
    hs.get(AUTHORIZATION).map { value =>
      val lower = value.toLowerCase
      if (lower.startsWith("bearer")) {
        BearerAuthorization(lower.drop("bearer".length).trim)
      } else {
        OtherAuthorization(value)
      }
    }
}

sealed trait Authorization { def value: String }

object Authorization {
  val AUTHORIZATION = "Authorization"
  val BEARER = "Bearer"

  case class BearerAuthorization(token: String) extends Authorization { def value = s"$BEARER $token"}
  case class OtherAuthorization(value: String) extends Authorization

  case class AuthorizationBuilder(hs: Headers) {
    def bearer(token: String): Headers = hs.set(AUTHORIZATION, s"$BEARER $token")
    def other(value: String): Headers = hs.set(AUTHORIZATION, value)
  }
}