package com.cloudentity.tools.vertx.http

import java.util.function.{Function => JFunction}

import com.cloudentity.tools.vertx.http.builder.{SmartHttpClientBuilder, SmartHttpClientBuilderImpl, SmartHttpResponse}
import com.cloudentity.tools.vertx.sd.circuit.CircuitBreakerFactory
import com.cloudentity.tools.vertx.sd.{ServiceName, SmartSd}
import io.vertx.circuitbreaker.CircuitBreakerOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.{Future, Vertx}

object SmartHttp {
  val defaultRetries = 0

  case class HttpHeader(key: String, value: String)

  /**
    * Creates a builder with fields setup from configuration.
    *
    * Config schema:
    * {
    *   "serviceName": "service-a",
    *   "serviceLocation": {
    *     "host": "service-a-host",
    *     "port": 8080,
    *     "ssl": false,
    *     "root": "/"
    *   },
    *   "serviceTags": ["v1"],
    *   "http": {},
    *   "retries": 5,
    *   "responseTimeout": 3000,
    *   "failureHttpCodes": [500],
    *   "retryFailedResponse": false,
    *   "retryOnException": false,
    *   "circuitBreakerOptions": {}
    * }
    *
    * "serviceLocation" is optional, if set then service discovery is disabled and provided location is used instead
    * "serviceTags" is optional - client filters out service nodes without those tags
    * "http" is converted to io.vertx.core.http.HttpClientOptions
    * "circuitBreakerOptions" is converted to io.vertx.circuitbreaker.CircuitBreakerOptions
    *
    * If "circuitBreakerOptions" is not set or "circuitBreakerOptions" has "off" field set to true then CB is not used by the HTTP client.
    */
  def clientBuilder(vertx: Vertx, config: JsonObject): SmartHttpClientBuilder =
    SmartHttpClientBuilderImpl.fromConfig(vertx, config)

  /**
    * Creates a builder with fields setup from configuration.
    * If `config.serviceName` is not set, then `serviceClientName` is used as service name.
    *
    * See {@link SmartHttp#clientBuilder(Vertx, JsonObject)} for config schema.
    */
  def clientBuilder(vertx: Vertx, serviceClientName: String, config: JsonObject): SmartHttpClientBuilder =
    if (Option(config.getString("serviceName")).isDefined)
      clientBuilder(vertx, config)
    else clientBuilder(vertx, config).serviceName(serviceClientName)

  /**
    * Creates a builder with `serviceName` set.
    *
    * Prefer using `clientBuilder` methods accepting configuration JsonObject.
    */
  def clientBuilder(vertx: Vertx, serviceName: String): SmartHttpClientBuilder = new SmartHttpClientBuilderImpl(vertx).serviceName(serviceName)

  private[http] def withSd(vertx: Vertx, serviceName: String, serviceTags: List[String], circuitBreakerOpts: CircuitBreakerOptions): Future[SmartSd] =
    SmartSd.withRoundRobinDiscovery(vertx, ServiceName(serviceName), serviceTags, CircuitBreakerFactory.fromOpts(vertx, ServiceName(serviceName), circuitBreakerOpts))

  private[http] def sdWithCircuitBreakerConfig(vertx: Vertx, serviceName: String, serviceTags: List[String], circuitBreakerOpts: Option[JsonObject]): Future[SmartSd] =
    SmartSd.withRoundRobinDiscovery(vertx, ServiceName(serviceName), serviceTags, CircuitBreakerFactory.fromConfig(vertx, ServiceName(serviceName), circuitBreakerOpts))

  type EvaluateExceptionRetry = JFunction[Throwable, java.lang.Boolean] // if (returns true) then request is retried
  type EvaluateResponseFailure = JFunction[SmartHttpResponse, java.lang.Boolean] // if (returns true) then circuit-breaker failure counter is increased; if (returns true && 'retryResponse' == true) request is retried
}


