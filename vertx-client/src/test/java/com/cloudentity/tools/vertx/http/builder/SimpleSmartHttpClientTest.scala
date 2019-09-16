package com.cloudentity.tools.vertx.http.builder

import com.cloudentity.tools.vertx.http.{SimpleSmartHttpClient, SmartHttpClient}
import com.cloudentity.tools.vertx.test.VertxUnitTest
import io.vertx.core.json.JsonObject
import org.junit.Test
import org.scalatest.MustMatchers
import org.scalatest.junit.AssertionsForJUnit

class SimpleSmartHttpClientTest extends VertxUnitTest with AssertionsForJUnit with MustMatchers {
  @Test
  def shouldCreateClientWithValidLocation() {
    // given
    val serviceLocation = new JsonObject().put("host", "localhost").put("port", 8080).put("ssl", false)

    val config = new JsonObject().put("serviceLocation", serviceLocation)

    // when
    val clientE: Either[Throwable, SmartHttpClient] = SimpleSmartHttpClient.create(vertx(), config)

    // then
    clientE.isRight must be(true)
  }

  @Test
  def shouldCreateClientWithValidLocationAndHttpClientOptions() {
    // given
    val serviceLocation = new JsonObject().put("host", "localhost").put("port", 8080).put("ssl", false)
    val httpConfig = new JsonObject().put("keepAlive", false)

    val config = new JsonObject().put("serviceLocation", serviceLocation).put("http", httpConfig)

    // when
    val clientE: Either[Throwable, SmartHttpClient] = SimpleSmartHttpClient.create(vertx(), config)

    // then
    clientE.isRight must be(true)
  }

  @Test
  def shouldReturnFailureWhenMissingPort() {
    // given
    val serviceLocation = new JsonObject().put("host", "localhost").put("ssl", false)

    val config = new JsonObject().put("serviceLocation", serviceLocation)

    // when
    val clientE: Either[Throwable, SmartHttpClient] = SimpleSmartHttpClient.create(vertx(), config)

    // then
    clientE.isRight must be(false)
  }
}
