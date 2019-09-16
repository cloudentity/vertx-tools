package com.cloudentity.tools.vertx.http.headers

import com.cloudentity.tools.vertx.http.Headers

sealed trait ContentType { def value: String }

object ContentType {
  val CONTENT_TYPE     = "Content-Type"
  val APPLICATION_JSON = "application/json"
  val APPLICATION_YAML = "application/x-yaml"

  case object ApplicationJson extends ContentType { val value = APPLICATION_JSON }
  case object ApplicationYaml extends ContentType { val value = APPLICATION_YAML }
  case class OtherContentType(value: String) extends ContentType

  case class ContentTypeBuilder(hs: Headers) {
    def applicationJson: Headers = hs.set(CONTENT_TYPE, APPLICATION_JSON)
    def applicationYaml: Headers = hs.set(CONTENT_TYPE, APPLICATION_YAML)
    def other(value: String): Headers = hs.set(CONTENT_TYPE, value)
  }
}

trait ContentTypeSyntax {
  import ContentType._
  protected def hs: Headers

  def withContentType = ContentTypeBuilder(hs)
  def withContentType(ct: ContentType) =
    hs.set(CONTENT_TYPE, ct.value)

  def contentTypeRaw: Option[String] =
    hs.get(CONTENT_TYPE)

  def contentType: Option[ContentType] =
    hs.get(CONTENT_TYPE).map {
      case APPLICATION_JSON => ApplicationJson
      case APPLICATION_YAML => ApplicationYaml
      case x                => OtherContentType(x)
    }
}