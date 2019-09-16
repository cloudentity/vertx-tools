package com.cloudentity.tools.vertx.http.builder

import com.cloudentity.tools.vertx.http.SmartHttp._
import com.cloudentity.tools.vertx.http._
import io.vertx.core.{Future, Handler}
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.{HttpClientResponse, HttpMethod}
import RequestCtxBuilderImpl.{RequestCtx, _}
import com.cloudentity.tools.vertx.http.builder.SmartHttpClientBuilderImpl.EvaluateResponseCallStatus
import com.cloudentity.tools.vertx.tracing.TracingContext

object RequestCtxBuilderImpl {
  case class RequestCtxValues(req: RequestValues, call: CallValues)

  case class RequestValues(
    method: HttpMethod,
    uri: Option[String] = None,
    body: Option[Buffer] = None,
    headers: List[SmartHttp.HttpHeader] = Nil
  )

  case class Request(
    method: HttpMethod,
    uri: String,
    body: Option[Buffer],
    headers: List[SmartHttp.HttpHeader]
  )

  case class RequestCtx(
    req: Request,
    evalResponseFailure: Option[EvaluateResponseCallStatus],
    evalExceptionRetry: Option[EvaluateExceptionRetry],
    callOpts: CallValues
  )
}

trait RequestExecution {
  def execute(ctx: RequestCtx): Future[HttpClientResponse]
  def execute(ctx: RequestCtx, bodyHandler: Handler[Buffer]): Future[HttpClientResponse]
  def executeWithBody(ctx: RequestCtx): Future[SmartHttpResponse]

  def execute(tracingContext: TracingContext, ctx: RequestCtx): Future[HttpClientResponse]
  def execute(tracingContext: TracingContext, ctx: RequestCtx, bodyHandler: Handler[Buffer]): Future[HttpClientResponse]
  def executeWithBody(tracingContext: TracingContext, ctx: RequestCtx): Future[SmartHttpResponse]
}

class RequestCtxBuilderImpl(executor: RequestExecution, ctx: RequestCtxValues) extends RequestCtxBuilder with CallValuesBuilderImpl[RequestCtxBuilder] {
  override def copy(rs: CallValues): RequestCtxBuilderImpl = copy(ctx.copy(call = rs))
  override def callValues: CallValues = ctx.call

  private def copy(req: RequestCtxValues): RequestCtxBuilderImpl = new RequestCtxBuilderImpl(executor, req)

  def method(m: HttpMethod): RequestCtxBuilderImpl =
    copy(ctx.copy(req = ctx.req.copy(method = m)))

  private def body(m: String): RequestCtxBuilderImpl =
    copy(ctx.copy(req = ctx.req.copy(body = Some(Buffer.buffer(m)))))

  private def body(m: Buffer): RequestCtxBuilderImpl =
    copy(ctx.copy(req = ctx.req.copy(body = Some(m))))

  def putHeader(key: String, value: String): RequestCtxBuilderImpl =
    copy(ctx.copy(req = ctx.req.copy(headers = HttpHeader(key, value) :: ctx.req.headers)))

  private def build(): RequestCtx =
    RequestCtx(
      req = Request(
        method = ctx.req.method,
        uri = ctx.req.uri.getOrElse(""),
        body = ctx.req.body,
        headers = ctx.req.headers
      ),
      evalResponseFailure = buildResponseFailureEvaluatorOpt(ctx.call),
      evalExceptionRetry = buildExceptionFailureEvaluatorOpt(ctx.call),
      callOpts = ctx.call
    )

  /**
    * Returns Some[EvaluateResponseCallStatus] if `responseFailure` or `retryFailedResponse` has been set
    */
  private def buildResponseFailureEvaluatorOpt(values: CallValues): Option[EvaluateResponseCallStatus] =
    if (List(values.responseFailure, values.retryFailedResponse).flatten.nonEmpty)
      Some(SmartHttpClientBuilderImpl.buildResponseFailureEvaluator(values))
    else None

  /**
    * Returns Some[EvaluateExceptionRetry] if `retryOnException` has been set
    */
  private def buildExceptionFailureEvaluatorOpt(values: CallValues): Option[EvaluateExceptionRetry] =
    if (values.retryOnException.isDefined)
      Some(SmartHttpClientBuilderImpl.buildExceptionFailureEvaluator(values))
    else None

  def end(): Future[HttpClientResponse] =
    executor.execute(build())

  def end(m: Buffer): Future[HttpClientResponse] =
    executor.execute(body(m).build())

  def end(m: String): Future[HttpClientResponse] =
    executor.execute(body(m).build())

  def end(bodyHandler: Handler[Buffer]): Future[HttpClientResponse] =
    executor.execute(build(), bodyHandler)

  def end(m: Buffer, bodyHandler: Handler[Buffer]): Future[HttpClientResponse] =
    executor.execute(body(m).build(), bodyHandler)

  def end(m: String, bodyHandler: Handler[Buffer]): Future[HttpClientResponse] =
    executor.execute(body(m).build(), bodyHandler)

  override def endWithBody(): Future[SmartHttpResponse] =
    executor.executeWithBody(build())

  override def endWithBody(b: Buffer): Future[SmartHttpResponse] =
    executor.executeWithBody(body(b).build())

  override def endWithBody(b: String): Future[SmartHttpResponse] =
    executor.executeWithBody(body(b).build())


  def end(tracingContext: TracingContext): Future[HttpClientResponse] =
    executor.execute(tracingContext, build())

  def end(tracingContext: TracingContext, m: Buffer): Future[HttpClientResponse] =
    executor.execute(tracingContext, body(m).build())

  def end(tracingContext: TracingContext, m: String): Future[HttpClientResponse] =
    executor.execute(tracingContext, body(m).build())

  def end(tracingContext: TracingContext, bodyHandler: Handler[Buffer]): Future[HttpClientResponse] =
    executor.execute(tracingContext, build(), bodyHandler)

  def end(tracingContext: TracingContext, m: Buffer, bodyHandler: Handler[Buffer]): Future[HttpClientResponse] =
    executor.execute(tracingContext, body(m).build(), bodyHandler)

  def end(tracingContext: TracingContext, m: String, bodyHandler: Handler[Buffer]): Future[HttpClientResponse] =
    executor.execute(tracingContext, body(m).build(), bodyHandler)

  override def endWithBody(tracingContext: TracingContext): Future[SmartHttpResponse] =
    executor.executeWithBody(tracingContext, build())

  override def endWithBody(tracingContext: TracingContext, b: Buffer): Future[SmartHttpResponse] =
    executor.executeWithBody(tracingContext, body(b).build())

  override def endWithBody(tracingContext: TracingContext, b: String): Future[SmartHttpResponse] =
    executor.executeWithBody(tracingContext, body(b).build())
}

object RequestCtxValues {

}