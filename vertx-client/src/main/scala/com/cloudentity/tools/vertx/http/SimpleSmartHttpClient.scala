package com.cloudentity.tools.vertx.http

import com.cloudentity.tools.vertx.http.builder.SmartHttpClientBuilderImpl
import com.cloudentity.tools.vertx.http.builder.SmartHttpClientBuilderImpl.CallOk
import com.cloudentity.tools.vertx.http.client.SmartHttpClientImpl
import com.cloudentity.tools.vertx.sd.{Location, Node, Sd, ServiceName}
import com.cloudentity.tools.vertx.sd.circuit.NoopCB
import io.vertx.core.{Future, Vertx}
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.json.JsonObject

object SimpleSmartHttpClient {

  /**
    * DEPRACATED: "Use SmartHttp.clientBuilder().build() - it supports the same configuration schema"
    *
    * Creates SmartHttpClient without circuit-breaker and service-discovery.
    *
    * Configuration schema:
    * {
    *   "serviceLocation": {
    *     "host": "localhost",
    *     "port": 8080,
    *     "ssl": false,
    *     "root": "/root-path"
    *   },
    *   "http": {
    *     # configuration of io.vertx.core.http.HttpClientOptions
    *   }
    * }
    *
    * "serviceLocation.root" and "http" are optional.
    */
  @Deprecated
  def create(vertx: Vertx, config: JsonObject): Either[Throwable, SmartHttpClient] =
    SmartHttpClientBuilderImpl.buildLocation(config.getJsonObject("serviceLocation"))
      .map { location =>
        val httpOpts = Option(config.getJsonObject("http")).map(new HttpClientOptions(_)).getOrElse(new HttpClientOptions())
        create(vertx, location, httpOpts)
      }

  /**
    * DEPRACATED: "Use SmartHttp.clientBuilder().build() - it supports the same configuration schema"
    */
  @Deprecated
  def create(vertx: Vertx, serviceHost: String, servicePort: Integer, serviceSsl: Boolean, serviceRootPath: String): SmartHttpClient =
    create(vertx, Location(serviceHost, servicePort, serviceSsl, Some(serviceRootPath)), new HttpClientOptions())

  /**
    * DEPRACATED: "Use SmartHttp.clientBuilder().build() - it supports the same configuration schema"
    */
  @Deprecated
  def create(vertx: Vertx, serviceHost: String, servicePort: Integer, serviceSsl: Boolean, serviceRootPath: String, opts: HttpClientOptions): SmartHttpClient =
    create(vertx, Location(serviceHost, servicePort, serviceSsl, Some(serviceRootPath)), opts)

  /**
    * DEPRACATED: "Use SmartHttp.clientBuilder().build() - it supports the same configuration schema"
    */
  @Deprecated
  def create(vertx: Vertx, location: Location, httpOpts: HttpClientOptions): SmartHttpClient = {
    val sd = new Sd {
      val _serviceName = ServiceName(s"http${if (location.ssl) "s" else ""}//${location.host}:${location.port}")

      override def discover(): Option[Node] = Some(Node(_serviceName, new NoopCB(_serviceName.value), location))
      override def serviceName(): ServiceName = _serviceName
      override def close(): Future[Unit] = Future.succeededFuture()
    }

    val client = vertx.createHttpClient(httpOpts)
    new SmartHttpClientImpl(
      sd, client,
      defaultResponseStatus = _ => CallOk,
      defaultResponseTimeout = None,
      defaultExceptionRetry = _ => false,
      defaultRetries = 0
    )
  }
}