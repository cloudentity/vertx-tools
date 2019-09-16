package com.cloudentity.tools.vertx.http.client

import java.util.Optional

import com.cloudentity.tools.vertx.http.SmartHttp._
import com.cloudentity.tools.vertx.http.builder.RequestCtxBuilderImpl._
import com.cloudentity.tools.vertx.http.builder.SmartHttpClientBuilderImpl._
import com.cloudentity.tools.vertx.http.builder._
import com.cloudentity.tools.vertx.http.SmartHttpClient
import com.cloudentity.tools.vertx.sd.{Node, Sd}
import com.cloudentity.tools.vertx.tracing.{LoggingWithTracing, TracingContext}
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.{HttpClient, HttpClientOptions, HttpClientResponse, HttpMethod, RequestOptions}
import io.vertx.core.{AsyncResult, Future, Handler, Vertx}
import scalaz.{-\/, \/, \/-}

case class ClientResponse(body: Option[Buffer], http: HttpClientResponse)

class SmartHttpClientImpl(
  sd: Sd,
  client: HttpClient,
  defaultRetries: Int,
  defaultResponseTimeout: Option[Int],
  defaultResponseStatus: EvaluateResponseCallStatus,
  defaultExceptionRetry: EvaluateExceptionRetry
) extends SmartHttpClient with RequestExecution {

  val log = LoggingWithTracing.getLogger(this.getClass)

  override def request(method: HttpMethod, uri: String): RequestCtxBuilder =
    new RequestCtxBuilderImpl(this, RequestCtxBuilderImpl.RequestCtxValues(RequestValues(method = method, uri = Option(uri)), CallValues()))

  override def execute(ctx: RequestCtx): Future[HttpClientResponse] = execute(TracingContext.dummy(), ctx)

  override def execute(ctx: RequestCtx, bodyHandler: Handler[Buffer]): Future[HttpClientResponse] =
    execute(TracingContext.dummy(), ctx, bodyHandler)

  override def executeWithBody(ctx: RequestCtx): Future[SmartHttpResponse] =
    executeWithBody(TracingContext.dummy(), ctx)

  override def execute(tracingContext: TracingContext, ctx: RequestCtx): Future[HttpClientResponse] =
    executeWithBodyHandling(tracingContext, ctx, IgnoreBody)
      .compose { response => Future.succeededFuture(response.http) }

  override def execute(tracingContext: TracingContext, ctx: RequestCtx, bodyHandler: Handler[Buffer]): Future[HttpClientResponse] =
    executeWithBodyHandling(tracingContext, ctx, UseBodyHandler(bodyHandler))
      .compose { response => Future.succeededFuture(response.http) }

  override def executeWithBody(tracingContext: TracingContext, ctx: RequestCtx): Future[SmartHttpResponse] =
    executeWithBodyHandling(tracingContext, ctx, ReadBody).compose { resp =>
      resp.body match {
        case Some(body) => Future.succeededFuture(new SmartHttpResponseImpl(body, resp.http))
        case None       => Future.failedFuture(new Exception("Could not read response body"))
      }
    }

  private def executeWithBodyHandling(tracingContext: TracingContext, ctx: RequestCtx, bodyHandling: BodyHandling): Future[ClientResponse] = {
    val promise = Future.future[ClientResponse]()

    discoverStep(
      RequestStep(
        tracingContext,
        ctx.callOpts.retries.getOrElse(defaultRetries) + 1,
        ctx.evalResponseFailure.getOrElse(defaultResponseStatus),
        ctx.evalExceptionRetry.getOrElse(defaultExceptionRetry),
        ctx.callOpts.responseTimeout.orElse(defaultResponseTimeout),
        ctx.req, promise, bodyHandling), None
    )

    promise
  }

  sealed trait BodyHandling
    case object IgnoreBody extends BodyHandling
    case class UseBodyHandler(handler: Handler[Buffer]) extends BodyHandling
    case object ReadBody extends BodyHandling


  case class RequestStep(
    tracing: TracingContext,
    attemptsLeft: Int,
    evalResponse: EvaluateResponseCallStatus,
    evalExceptionRetry: EvaluateExceptionRetry,
    responseTimeout: Option[Int],
    req: Request,
    promise: Future[ClientResponse],
    bodyHandling: BodyHandling
  )

  private def discoverStep(rs: RequestStep, lastResult: Option[Throwable \/ ClientResponse]): Unit =
    sd.discover()
      .map(newNode => runRequest(rs, newNode, lastResult))
      .getOrElse {
        log.error(rs.tracing, s"Request failed. Could not discover '${sd.serviceName().value}' node. ${reqSignature(rs.req)}")
        finishWithLastResponse(rs.promise, lastResult, rs.req)
      }

  private def runRequest(rs: RequestStep, node: Node, lastResult: Option[Throwable \/ ClientResponse]): Unit = {
    if(rs.attemptsLeft > 0) {
      log.debug(rs.tracing, s"Calling ${callSignature(rs.req, node)}, ${rs.attemptsLeft - 1} retries left")

      val handler: Handler[HttpClientResponse] =
      { response: HttpClientResponse =>
        rs.bodyHandling match {
          case IgnoreBody =>
            step(rs, node, ClientResponse(None, response))
          case ReadBody =>
            response.bodyHandler { body =>
              step(rs, node, ClientResponse(Some(body), response))
            }.exceptionHandler { ex => retryRun(rs, node, ex) }
          case UseBodyHandler(handler) =>
            response.bodyHandler(handler)
            step(rs, node, ClientResponse(None, response))
        }
      }

      val r =
        client.request(rs.req.method, setupOptions(rs.req, node), handler)
          .exceptionHandler { ex => retryRun(rs, node, ex) }

      rs.responseTimeout.foreach(ts => r.setTimeout(ts))
      rs.req.headers.foreach(header => r.putHeader(header.key, header.value))

      rs.req.body match {
        case Some(body) => r.end(body)
        case None       => r.end()
      }
    } else {
      log.error(rs.tracing, s"Request failed. No more retries left. Returning last result. ${reqSignature(rs.req)}")
      finishWithLastResponse(rs.promise, lastResult, rs.req)
    }
  }

  private def retryRun(rs: RequestStep, node: Node, ex: Throwable) = {
    log.error(rs.tracing, s"Call failed with exception: ${callSignature(rs.req, node)}", ex)
    node.cb.execute[Unit](_.fail(ex))

    if (rs.evalExceptionRetry(ex))
      discoverStep(rs.copy(attemptsLeft = rs.attemptsLeft - 1), Some(-\/(ex)))
    else rs.promise.fail(ex)
  }

  private def step(rs: RequestStep, node: Node, response: ClientResponse): Unit =
    rs.evalResponse(new SmartHttpResponseImpl(response.body.getOrElse(Buffer.buffer()), response.http)) match {
      case CallOk =>
        log.debug(rs.tracing, s"Request succeeded: ${callSignature(rs.req, node)}, response ${respSignature(response.http)}")
        rs.promise.complete(response)
      case CallFailed(retry) =>
        log.error(rs.tracing, s"Call failed with bad response: ${callSignature(rs.req, node)}, response ${respSignature(response.http)}")
        node.cb.execute[Unit](_.fail("response failed"))

        if (retry)
          discoverStep(rs.copy(attemptsLeft = rs.attemptsLeft - 1), Some(\/-(response)))
        else rs.promise.complete(response)
    }

  private def finishWithLastResponse(promise: Future[ClientResponse], lastResult: Option[Throwable \/ ClientResponse], req: Request): Unit =
    lastResult match {
      case Some(\/-(response)) => promise.complete(response)
      case Some(-\/(ex))       => promise.fail(ex)
      case None                => promise.fail(s"Request failed. ${reqSignature(req)}")
    }

  private def setupOptions(req: Request, node: Node): RequestOptions =
    new RequestOptions()
      .setHost(node.location.host)
      .setPort(node.location.port)
      .setSsl(node.location.ssl)
      .setURI(node.location.root.fold(req.uri)(_ + req.uri))

  private def reqSignature(req: Request): String =
    s"<service=${sd.serviceName().value}> ${req.method} ${req.uri}"

  private def respSignature(resp: HttpClientResponse): String =
    s"statusCode=${resp.statusCode()}"

  private def callSignature(req: Request, node: Node): String =
    s"<service=${node.name.value}> ${req.method} ${node.location.host}:${node.location.port}${node.location.root.fold(req.uri)(_ + req.uri)}"

  override def close(): Future[Void] = {
    client.close()
    sd.close().compose(_ => Future.succeededFuture())
  }
}
