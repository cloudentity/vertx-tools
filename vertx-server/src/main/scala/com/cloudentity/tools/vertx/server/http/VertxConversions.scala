package com.cloudentity.tools.vertx.server.http

import java.util.function.BinaryOperator

import com.cloudentity.tools.vertx.http.Headers
import io.vertx.core.{MultiMap => JMultiMap}
import io.vertx.core.http.{HttpServerRequest => JHttpServerRequest}
import io.vertx.core.http.{HttpServerResponse => JHttpServerResponse}
import io.vertx.core.http.{HttpClientResponse => JHttpClientResponse}
import io.vertx.core.http.{HttpClientRequest => JHttpClientRequest}

trait ServerConversions {
  implicit class JHttpServerRequestConversions(r: JHttpServerRequest) {
    def scalaHeaders(): Headers = HeadersConversions.convert(r.headers())
  }

  implicit class JHttpServerResponseConversions(r: JHttpServerResponse) {
    def scalaHeaders(): Headers = HeadersConversions.convert(r.headers())
  }
}

object ServerConversions extends ServerConversions

trait ClientConversions {
  implicit class JHttpClientResponseConversions(r: JHttpClientResponse) {
    def scalaHeaders(): Headers = HeadersConversions.convert(r.headers())
  }

  implicit class JHttpClientRequestConversions(r: JHttpClientRequest) {
    def scalaHeaders(): Headers = HeadersConversions.convert(r.headers())
  }
}

object ClientConversions extends ClientConversions

object HeadersConversions {
  import scala.collection.JavaConverters._

  def convert(m: JMultiMap): Headers =
    m.names().stream()
      .reduce(
        Headers(),
        (hs: Headers, name: String) => hs.addValues(name, m.getAll(name).asScala.toList),
        new BinaryOperator[Headers] { override def apply(a: Headers, b: Headers): Headers = a.addMultiHeaders(b.toMap) }
      )
}