package com.cloudentity.tools.vertx.http.builder

import com.cloudentity.tools.vertx.http.SmartHttp._
import com.cloudentity.tools.vertx.http._
import com.cloudentity.tools.vertx.http.client.SmartHttpClientImpl
import com.cloudentity.tools.vertx.sd.{Location, Sd}
import io.vertx.core.http.{HttpClientOptions}
import io.vertx.core.json.{JsonArray, JsonObject}
import io.vertx.core.{Future, Vertx}

import scalaz.Scalaz._
import scala.util.Try

class SmartHttpClientBuilderImpl(vertx: Vertx, val values: SmartHttpClientBuilderImpl.SmartHttpClientValues) extends SmartHttpClientBuilder with CallValuesBuilderImpl[SmartHttpClientBuilder] {
  def this(vertx: Vertx) = this(vertx, SmartHttpClientBuilderImpl.SmartHttpClientValues())

  override def serviceName(n: String) =
    copy(values.copy(serviceName = Option(n)))

  override def serviceTags(ts: String*): SmartHttpClientBuilder =
    copy(values.copy(serviceTags = Some(ts.toList)))

  override def httpClientOptions(o: HttpClientOptions): SmartHttpClientBuilder =
    copy(values.copy(httpClientOptions = Option(o)))

  override def circuitBreakerConfig(c: JsonObject) =
    copy(values.copy(circuitBreakerConfig = Option(c)))

  override def build(): Future[SmartHttpClient] = {
    val sdFutOpt: Option[Future[Sd]] =
    (values.serviceName, values.serviceLocation) match {
        case (_, Some(serviceLocation)) =>
          Option(Future.succeededFuture(Sd.withStaticLocation(serviceLocation)))
        case (Some(serviceName), _) =>
          Option(sdWithCircuitBreakerConfig(vertx, serviceName, values.serviceTags.getOrElse(Nil), values.circuitBreakerConfig).asInstanceOf[Future[Sd]])
        case (None, None) =>
          None
      }

    sdFutOpt match {
      case Some(sdFut) =>
        sdFut.compose { sd =>
          val vertxClient = vertx.createHttpClient(values.httpClientOptions.getOrElse(new HttpClientOptions()))
          val client =
            new SmartHttpClientImpl(sd, vertxClient,
              defaultResponseStatus = SmartHttpClientBuilderImpl.buildResponseFailureEvaluator(values.callValues),
              defaultResponseTimeout = values.callValues.responseTimeout,
              defaultExceptionRetry = SmartHttpClientBuilderImpl.buildExceptionFailureEvaluator(values.callValues),
              defaultRetries = values.callValues.retries.getOrElse(defaultRetries)
            )

          Future.succeededFuture(client)
        }
      case None =>
        // SmartHttpClient has to have 'serviceName' or 'serviceLocation' configured - no reasonable default value
        Future.failedFuture[SmartHttpClient](new Exception("'serviceName' nor 'serviceLocation' is set in SmartHttpClient builder"))
    }
  }


  private def copy(vs: SmartHttpClientBuilderImpl.SmartHttpClientValues) = new SmartHttpClientBuilderImpl(vertx, vs)

  protected override def copy(rs: CallValues): SmartHttpClientBuilder = new SmartHttpClientBuilderImpl(vertx, values.copy(callValues = rs))

  protected override def callValues: CallValues = values.callValues
}

object SmartHttpClientBuilderImpl {
  type EvaluateResponseCallStatus = Function[SmartHttpResponse, CallStatus]

  sealed trait CallStatus
    case class CallFailed(retry: Boolean) extends CallStatus
    case object CallOk extends CallStatus

  case class SmartHttpClientValues(
    serviceName: Option[String] = None,
    serviceLocation: Option[Location] = None,
    serviceTags: Option[List[String]] = None,
    httpClientOptions: Option[HttpClientOptions] = None,
    callValues: CallValues = CallValues(),
    circuitBreakerConfig: Option[JsonObject] = None
  )

  def fromConfig(vertx: Vertx, config: JsonObject): SmartHttpClientBuilder = {
    import scala.collection.JavaConverters._
    def retryHttpCodesToResponseFailure(array: JsonArray): Option[EvaluateResponseFailure] =
      Try(array.getList.asScala.toList.map(_.asInstanceOf[Int])).toOption
        .map(responseFailureFromRetryHttpCodes)

    def tags(array: JsonArray): Option[List[String]] =
      Try(array.getList.asScala.toList.map(_.asInstanceOf[String])).toOption

    def responseFailureFromRetryHttpCodes(retryHttpCodes: List[Int]) =
      new EvaluateResponseFailure{
        override def apply(resp: SmartHttpResponse): java.lang.Boolean =
          if (retryHttpCodes.exists(resp.getHttp.statusCode() == _)) true
          else false
      }

    // we need to wrap in Try, becayse JsonObject.getXxx can throw ClassCastException
    Try {
      new SmartHttpClientBuilderImpl(vertx,
        SmartHttpClientValues(
          serviceName              = Option(config.getString("serviceName")),
          serviceLocation          = Option(config.getJsonObject("serviceLocation")).map(decodeServiceLocation(_).get), // it may throw, but it's
          serviceTags              = Option(config.getJsonArray("serviceTags")).flatMap(tags),
          httpClientOptions        = Option(config.getJsonObject("httpClientOptions", config.getJsonObject("http"))).map(o => new HttpClientOptions(o)),

          // For integer and boolean values we can't use Option(config.getBoolean("field")) because there is weird interoperability issue between Scala and Java Boolean/Int.
          // If we had CallValues(retries = Option(config.getInteger("retries"))) and config.retries was null we would get CallValues(retries = Some(0)); similarly with Booleans.
          // Moreover, Option(null.asInstanceOf[Boolean]) evaluates to Some(false) in Scala 2.12 (???)
          callValues = CallValues(
            retries                  = Option(config.getValue("retries")).map(_.asInstanceOf[Int]),
            responseFailure          = Option(config.getJsonArray("failureHttpCodes")).flatMap(retryHttpCodesToResponseFailure),
            retryFailedResponse      = Option(config.getValue("retryFailedResponse")).map(_.asInstanceOf[Boolean]),
            retryOnException         = Option(config.getValue("retryOnException")).map(_.asInstanceOf[Boolean]),
            responseTimeout          = Option(config.getValue("responseTimeout")).map(_.asInstanceOf[Int])
          ),

          circuitBreakerConfig     = Option(config.getJsonObject("circuitBreakerOptions", config.getJsonObject("circuitBreaker")))
        )
      )
    } match {
      case scala.util.Success(builder) => builder
      case scala.util.Failure(ex) =>
        new SmartHttpClientBuilderImpl(vertx, SmartHttpClientValues()) {
          override def build(): Future[SmartHttpClient] = Future.failedFuture(ex)
        }
    }
  }

  private def decodeServiceLocation(obj: JsonObject): Try[Location] = {
    for {
      host <- Option(obj.getString("host"))
      port <- Option(obj.getInteger("port"))
      ssl  <- Option(obj.getBoolean("ssl"))
      root  = Option(obj.getString("root"))
    } yield (Location(host, port, ssl, root))
  }.toRight(new Exception("Could not decode 'serviceLocation'")).toTry

  def buildResponseFailureEvaluator(values: CallValues): EvaluateResponseCallStatus = {
    val retryFailedResponse = values.retryFailedResponse.getOrElse(true)
    (resp: SmartHttpResponse) =>
      values.responseFailure
        .map(func => if (func.apply(resp)) CallFailed(retryFailedResponse) else CallOk)
        .getOrElse(CallOk)
  }

  def buildExceptionFailureEvaluator(values: CallValues): EvaluateExceptionRetry =
    _ => values.retryOnException.getOrElse(true).asInstanceOf[java.lang.Boolean]

  def buildLocation(obj: JsonObject): Either[Throwable, Location] =
    for {
      host    <- Option(obj.getString("host")).toRight(new Exception("'host' not set"))
      port    <- Option(obj.getValue("port")).map(_.asInstanceOf[Int]).toRight(new Exception("'port' not set"))
      ssl     <- Option(obj.getValue("ssl")).map(_.asInstanceOf[Boolean]).toRight(new Exception("'ssl' not set"))
      rootPath = Option(obj.getString("root"))
    } yield Location(host, port, ssl, rootPath)
}