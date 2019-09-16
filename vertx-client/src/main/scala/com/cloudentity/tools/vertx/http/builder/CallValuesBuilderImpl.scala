package com.cloudentity.tools.vertx.http.builder

import com.cloudentity.tools.vertx.http.SmartHttp.EvaluateResponseFailure

case class CallValues(
  retries: Option[Int] = None,
  responseFailure: Option[EvaluateResponseFailure] = None,
  retryFailedResponse: Option[Boolean] = None,
  retryOnException: Option[Boolean] = None,
  responseTimeout: Option[Int] = None
)

trait CallValuesBuilderImpl[Builder] extends CallValuesBuilder[Builder] {
  protected def copy(callOpts: CallValues): Builder
  protected def callValues: CallValues

  override def responseFailure(rf: EvaluateResponseFailure): Builder =
    copy(callValues.copy(responseFailure = Option(rf)))

  override def retryFailedResponse(retry: Boolean): Builder =
    copy(callValues.copy(retryFailedResponse = Option(retry)))

  override def retries(r: Int): Builder =
    copy(callValues.copy(retries = Option(r)))

  override def retryOnException(r: Boolean): Builder =
    copy(callValues.copy(retryOnException = Option(r)))

  override def responseTimeout(t: Int): Builder =
    copy(callValues.copy(responseTimeout = Option(t)))
}