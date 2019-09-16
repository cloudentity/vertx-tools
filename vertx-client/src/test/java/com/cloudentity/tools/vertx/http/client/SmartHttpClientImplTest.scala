package com.cloudentity.tools.vertx.http.client

import com.cloudentity.tools.vertx.http.{SmartHttp, SmartHttpClient}
import com.cloudentity.tools.vertx.http.builder.SmartHttpClientBuilderImpl._
import com.cloudentity.tools.vertx.http.builder.SmartHttpResponse
import com.cloudentity.tools.vertx.sd._
import com.cloudentity.tools.vertx.test.VertxUnitTest
import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.{HttpClientOptions, HttpClientResponse}
import io.vertx.core.json.JsonObject
import io.vertx.core.{AsyncResult, Future, Handler, Vertx}
import io.vertx.ext.unit.{Async, TestContext}
import okhttp3.mockwebserver.{MockResponse, MockWebServer}
import org.junit.{After, Before, Test}
import org.scalatest.MustMatchers
import org.scalatest.junit.AssertionsForJUnit

class SmartHttpClientImplTest extends VertxUnitTest with AssertionsForJUnit with MustMatchers {

  def defaultClient(sd: Sd, vertx: Vertx) =
    new SmartHttpClientImpl(sd, vertx.createHttpClient(),
      defaultRetries = 10,
      defaultResponseTimeout = None,
      defaultResponseStatus = r => if (r.getHttp.statusCode() == 500) CallFailed(true) else CallOk,
      defaultExceptionRetry = _ => true
    )

  @Test def shouldHandleFailedSmartHttpRequest(context: TestContext): Unit = {
    // given
    val smartSd = staticSd(None)
    val client = defaultClient(smartSd, vertx)

    // when
    val r = client.get(defaultUri).end()

    // then
    r.setHandler(context.asyncAssertFailure());
  }

  @Test def shouldAllowHandlingResponsePerRequest(context: TestContext): Unit = {
    // given
    setupServerResponses(response569)
    val hostUrl = server.url("/")
    val location = Location(hostUrl.host(), hostUrl.port(), ssl = false, None)
    val smartSd = staticSd(Some(Node(sn, cb, location)))

    val client = defaultClient(smartSd, vertx)

    // when
    val r = client.get(defaultUri).responseFailure(_.getHttp.statusCode() == 569).end()

    // then
    r.setHandler(context.asyncAssertSuccess((response: HttpClientResponse) => {
      response.statusCode() mustBe 569
    }))
  }

  @Test def shouldReadBody(context: TestContext): Unit = {
    // given
    val expectedBody = "BODY"
    setupServerResponses(response200.setBody(expectedBody))
    val hostUrl = server.url("/")
    val location = Location(hostUrl.host(), hostUrl.port(), ssl = false, None)
    val smartSd = staticSd(Some(Node(sn, cb, location)))

    val client = defaultClient(smartSd, vertx)

    // when
    val r = client.get(defaultUri).endWithBody()

    // then
    r.setHandler(context.asyncAssertSuccess((response: SmartHttpResponse) => {
      response.getHttp.statusCode() mustBe 200
      response.getBody.toString() mustBe expectedBody
    }))
  }

  @Test def shouldUseBodyHandler(context: TestContext): Unit = {
    // given
    val expectedBody = "BODY"
    setupServerResponses(response200.setBody(expectedBody))
    val hostUrl = server.url("/")
    val location = Location(hostUrl.host(), hostUrl.port(), ssl = false, None)
    val smartSd = staticSd(Some(Node(sn, cb, location)))

    val client = defaultClient(smartSd, vertx)

    // when
    val promise = Future.future[Buffer]()
    val r = client.get(defaultUri).end { body => promise.complete(body) }

    r.compose { response =>
      promise.compose(body => Future.succeededFuture((response, body)))
    }.setHandler(context.asyncAssertSuccess {
      case (response: HttpClientResponse, body: Buffer) =>
        response.statusCode() mustBe 200
        body.toString() mustBe expectedBody
    })
  }

  @Test def shouldHandleSmartSuccessfulHttpRequest(context: TestContext): Unit = {
    // given
    val expectedBody = "test response"
    setupServerResponses(response200.setBody(expectedBody))
    val hostUrl = server.url("/")
    val location = Location(hostUrl.host(), hostUrl.port(), ssl = false, None)
    val smartSd = staticSd(Some(Node(sn, cb, location)))
    val client = defaultClient(smartSd, vertx)

    // when
    val r = client.get(defaultUri)
      .putHeader("Test-Header", "test value")
      .putHeader("Other-Header", "other value")
      .end()

    // then
    r.setHandler(context.asyncAssertSuccess((response: HttpClientResponse) => {
      response.statusCode() mustBe 200
      val recordedRequest = server.takeRequest()
      recordedRequest.getPath mustBe "/"
      recordedRequest.getHeader("Test-Header") mustBe "test value"
      response.bodyHandler(bh => {
        body(bh) mustBe expectedBody
      })
    }))
  }

  @Test def shouldHandleSmartFailedHttpRequest(context: TestContext): Unit = {
    // given
    setupServerResponses(response500)
    val hostUrl = server.url("/")
    val smartSd = staticSd(Some(Node(sn, cb, Location(hostUrl.host(), hostUrl.port(), ssl = false, None))))
    val client = defaultClient(smartSd, vertx)

    // when
    val r = client.get(defaultUri).end()

    // then
    r.setHandler(context.asyncAssertSuccess((response: HttpClientResponse) => {
      response.statusCode() mustBe 500
      val recordedRequest = server.takeRequest()
      recordedRequest.getPath mustBe "/"
    }))
  }

  @Test def shouldHandleMultipleAttemptsWithInvalidResponse(context: TestContext): Unit = {
    // given
    val async = context.async()
    setupServerResponses(response500, response500, response200.setBody("ok"))
    val hostUrl = server.url("/")
    val node = Some(Node(sn, cb, Location(hostUrl.host(), hostUrl.port(), ssl = false, None)))
    val smartSd = staticSd(node, node, node, node, node)
    val client = defaultClient(smartSd, vertx)

    // when
    val r = client.get(defaultUri).end()

    // then
    r.setHandler(assertSuccessWithBody(context, async, "ok"))
  }

  @Test def shouldHandleMultipleAttemptsWithUnavailableNodes(context: TestContext): Unit = {
    // given
    val async = context.async()
    server.enqueue(response200.setBody("ok"))
    val hostUrl = server.url("/")
    val validNode = Some(Node(sn, cb, Location(hostUrl.host(), hostUrl.port(), ssl = false, None)))
    val invalidNode = Some(Node(sn, cb, Location(hostUrl.host(), 23000, ssl = false, None)))
    val smartSd = staticSd(invalidNode, invalidNode, invalidNode, invalidNode, validNode)
    val client = defaultClient(smartSd, vertx)

    // when
    val r = client.get(defaultUri).end()

    // then
    r.setHandler(assertSuccessWithBody(context, async, "ok"))
  }

  @Test def shouldMakeCallToStaticServiceLocation(context: TestContext): Unit = {
    // given
    val async = context.async()
    server.enqueue(response200.setBody("ok"))
    val hostUrl = server.url("/")

    SmartHttp.clientBuilder(vertx, new JsonObject().put("serviceLocation", new JsonObject().put("host", hostUrl.host()).put("port", hostUrl.port()).put("ssl", false))).build()
      // when
      .compose(_.get(defaultUri).end())
      // then
      .setHandler(assertSuccessWithBody(context, async, "ok"))
  }

  private def body(bh: Buffer): String = {
    bh.getString(0, bh.length())
  }

  private def response200 = new MockResponse().setResponseCode(200)

  private def response569 = new MockResponse().setResponseCode(569)

  private def response500 = new MockResponse().setResponseCode(500)

  private def setupServerResponses(mockResponse: MockResponse*) = {
    mockResponse.foreach(server.enqueue(_))
  }

  private def assertSuccessWithBody(context: TestContext, async: Async, expectedBody: String): Handler[AsyncResult[HttpClientResponse]] = {
    context.asyncAssertSuccess((response: HttpClientResponse) => {
      response.statusCode() mustBe 200
      server.takeRequest().getPath mustBe "/"
      response.bodyHandler(bh => {
        body(bh) mustBe expectedBody
        async.complete()
      })
    })
  }

  var cb: CircuitBreaker = null
  var server: MockWebServer = null
  val port = 23001
  val sn = ServiceName("test")

  val defaultUri = "/"

  def staticSd(v: Option[Node]*) = new Sd {
    var stack = List(v:_*)
    override def discover(): Option[Node] = {
      stack match {
        case h :: tail =>
          stack = tail
          h
        case _ => None
      }
    }

    override def serviceName(): ServiceName = ServiceName("test")

    override def close(): Future[Unit] = Future.succeededFuture(())
  }

  @Before
  def beforeTest() = {
    server = new MockWebServer()
    server.start(port)
    cb = CircuitBreaker.create("test", vertx)
  }

  @After
  def afterTest() = {
    server.shutdown()
  }
}
