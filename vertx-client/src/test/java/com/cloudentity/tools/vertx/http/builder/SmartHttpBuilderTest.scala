package com.cloudentity.tools.vertx.http.builder

import java.util
import java.util.Optional

import com.cloudentity.tools.vertx.http.SmartHttp
import com.cloudentity.tools.vertx.http.builder.SmartHttpClientBuilderImpl.SmartHttpClientValues
import com.cloudentity.tools.vertx.sd.Location
import io.vertx.core.buffer.Buffer
import io.vertx.core.http._
import io.vertx.core.json.{JsonArray, JsonObject}
import io.vertx.core.net.NetSocket
import io.vertx.core.{Handler, MultiMap}
import org.junit.Test
import org.scalatest.MustMatchers
import org.scalatest.junit.AssertionsForJUnit

class SmartHttpBuilderTest extends AssertionsForJUnit with MustMatchers {
  @Test def shouldCreateBuilderFromJsonObject(): Unit = {
    // given
    val config =
      json()
        .put("httpClientOptions", json().put("defaultPort", 8000))
        .put("retries", 3)
        .put("circuitBreakerOptions", json().put("off", true))
        .put("failureHttpCodes", new JsonArray().add(500).add(404))
        .put("retryFailedResponse", true)
        .put("retryOnException", true)
        .put("serviceName", "service-a")
        .put("serviceLocation", new JsonObject().put("host", "localhost").put("port", 80).put("ssl", true).put("root", "/root"))
        .put("circuitBreakerConfigPath", "cb-path")
        .put("responseTimeout", 3000)

    // when
    val builder = SmartHttp.clientBuilder(null, config).asInstanceOf[SmartHttpClientBuilderImpl]

    // then
    builder.values.serviceName mustBe(Some("service-a"))
    builder.values.serviceLocation mustBe(Some(Location("localhost", 80, true, Some("/root"))))
    builder.values.httpClientOptions.map(_.toJson) mustBe(Some(new HttpClientOptions().setDefaultPort(8000).toJson))
    builder.values.circuitBreakerConfig mustBe(Some(json().put("off", true)))
    builder.values.callValues.responseFailure.map(_.apply(new SmartDummyResponse(500))) mustBe(Some(true))
    builder.values.callValues.retryFailedResponse mustBe(Some(true))
    builder.values.callValues.retryOnException mustBe(Some(true))
    builder.values.callValues.responseTimeout mustBe(Some(3000))
  }

  @Test def shouldCreateBuilderFromEmptyJsonObject(): Unit = {
    // given
    val config = new JsonObject()

    // when
    val builder = SmartHttp.clientBuilder(null, config).asInstanceOf[SmartHttpClientBuilderImpl]
    
    // then
    builder.values.serviceName mustBe(None)
    builder.values.httpClientOptions.map(_.toJson) mustBe(None)
    builder.values.circuitBreakerConfig mustBe(None)
    builder.values.callValues.responseFailure mustBe(None)
    builder.values.callValues.retryFailedResponse mustBe(None)
    builder.values.callValues.retryOnException mustBe(None)
    builder.values.callValues.responseTimeout mustBe(None)
  }

  def json() = new JsonObject()
}

class DummyResponse(code: Int) extends HttpClientResponse {
  override def statusCode(): Int = code

  override def request(): HttpClientRequest = ???
  override def handler(handler: Handler[Buffer]): HttpClientResponse = ???
  override def getTrailer(trailerName: String): String = ???
  override def cookies(): util.List[String] = ???
  override def netSocket(): NetSocket = ???
  override def getHeader(headerName: String): String = ???
  override def getHeader(headerName: CharSequence): String = ???
  override def bodyHandler(bodyHandler: Handler[Buffer]): HttpClientResponse = ???
  override def customFrameHandler(handler: Handler[HttpFrame]): HttpClientResponse = ???
  override def resume(): HttpClientResponse = ???
  override def headers(): MultiMap = ???
  override def version(): HttpVersion = ???
  override def pause(): HttpClientResponse = ???
  override def statusMessage(): String = ???
  override def trailers(): MultiMap = ???
  override def endHandler(endHandler: Handler[Void]): HttpClientResponse = ???
  override def exceptionHandler(handler: Handler[Throwable]): HttpClientResponse = ???
  override def fetch(amount: Long): HttpClientResponse = ???
  override def streamPriorityHandler(handler: Handler[StreamPriority]): HttpClientResponse = ???
}

class SmartDummyResponse(code: Int) extends SmartHttpResponse {
  override def getBody: Buffer = null
  override def getHttp: HttpClientResponse = new DummyResponse(code)
}