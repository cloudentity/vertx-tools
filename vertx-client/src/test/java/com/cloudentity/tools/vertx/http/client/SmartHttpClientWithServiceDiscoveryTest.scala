package com.cloudentity.tools.vertx.http.client

import com.cloudentity.tools.vertx.http.builder.SmartHttpClientBuilderImpl._
import com.cloudentity.tools.vertx.sd.SdTestTools.{deployEventBusDiscovery, eventBusSdProvider}
import com.cloudentity.tools.vertx.sd._
import com.cloudentity.tools.vertx.test.ServiceVerticleIntegrationTest
import io.vertx.circuitbreaker.CircuitBreakerOptions
import io.vertx.core.Vertx
import io.vertx.core.http.{HttpClientOptions, HttpClientResponse}
import io.vertx.ext.unit.TestContext
import okhttp3.mockwebserver.{MockResponse, MockWebServer, RecordedRequest}
import org.junit.{After, Before, Test}
import org.scalatest.MustMatchers
import org.scalatest.junit.AssertionsForJUnit

class SmartHttpClientWithServiceDiscoveryTest extends ServiceVerticleIntegrationTest with AssertionsForJUnit with MustMatchers {

  def defaultClient(sd: Sd, vertx: Vertx) =
    new SmartHttpClientImpl(sd, vertx.createHttpClient(),
      defaultRetries = 10,
      defaultResponseTimeout = None,
      defaultResponseStatus = r => if (r.getHttp.statusCode() == 500) CallFailed(true) else CallOk,
      defaultExceptionRetry = _ => true
    )

  val serviceName = ServiceName("service")
  val port = 23001
  val portTwo = 23002
  val locationOne = Location("localhost", port, false, None)
  val locationTwo = Location("localhost", portTwo, false, None)
  val cbOpts = new CircuitBreakerOptions

  val resetTimeout = 1000
  val configKey = "circuit-breaker"

  @Test def shouldDiscoverNode(ctx: TestContext): Unit = {
    serverOne.enqueue(new MockResponse().setResponseCode(569))
    serverTwo.enqueue(new MockResponse().setResponseCode(200))

    val sdProvider = eventBusSdProvider(vertx)
    val async = ctx.async()
    val hostUrl = serverOne.url("/")
    deployEventBusDiscovery(vertx)
      .compose(_ =>
        // registering first node
        sdProvider.register(serviceName.value, SmartRecord(NodeId("node-1"), locationOne, Nil))
      ).compose(_ =>
        // registering second node
        sdProvider.register(serviceName.value, SmartRecord(NodeId("node-2"), locationTwo, Nil))
      )
      .compose(_ => SmartSd.withRoundRobinDiscovery(vertx, serviceName, Nil, cbOpts))
      .compose { sd: SmartSd =>
        defaultClient(sd, vertx).get(defaultUri).responseFailure(_.getHttp.statusCode() != 200).end()
      }.setHandler(ctx.asyncAssertSuccess((response: HttpClientResponse) => {
        response.statusCode() mustBe 200
        async.complete()
      }))
  }

  @Test def shouldDiscoverNodeWithRootPath(ctx: TestContext): Unit = {
    val rootPath = "/root-path"
    serverOne.setDispatcher { (recordedRequest: RecordedRequest) =>
      if (recordedRequest.getPath.startsWith(rootPath)) new MockResponse().setResponseCode(200)
      else new MockResponse().setResponseCode(500)
    }

    val sdProvider = eventBusSdProvider(vertx)
    val async = ctx.async()
    deployEventBusDiscovery(vertx)
      .compose(_ =>
        sdProvider.register(serviceName.value, SmartRecord(NodeId("node-1"), locationOne.copy(root = Some(rootPath)), Nil))
      )
      .compose(_ => SmartSd.withRoundRobinDiscovery(vertx, serviceName, Nil, cbOpts))
      .compose { sd: SmartSd =>
        defaultClient(sd, vertx).get(defaultUri).responseFailure(_.getHttp.statusCode() != 200).end()
      }.setHandler(ctx.asyncAssertSuccess((response: HttpClientResponse) => {
      response.statusCode() mustBe 200
      async.complete()
    }))
  }

  val defaultUri = "/"

  var serverOne: MockWebServer = null
  var serverTwo: MockWebServer = null

  @Before
  def beforeTest() = {
    serverOne = new MockWebServer()
    serverOne.start(port)
    serverTwo = new MockWebServer()
    serverTwo.start(portTwo)
  }

  @After
  def afterTest() = {
    serverOne.shutdown()
    serverTwo.shutdown()
  }

}
