package com.cloudentity.tools.vertx.http.headers

import com.cloudentity.tools.vertx.http.Headers

trait HeadersSyntax {
  implicit class ToHeadersSyntax(protected val hs: Headers) extends ContentTypeSyntax with AuthorizationSyntax
}

object HeadersSyntax extends HeadersSyntax