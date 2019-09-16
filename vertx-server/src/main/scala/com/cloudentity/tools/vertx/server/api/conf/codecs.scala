package com.cloudentity.tools.vertx.server.api.conf

import io.circe.Decoder.Result
import io.circe.parser._
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerOptions
import org.slf4j.LoggerFactory

import scala.util.Try

object codecs {
  private val log = LoggerFactory.getLogger(this.getClass)

  def IdEnc[A](f: A => String): Encoder[A] = Encoder.encodeString.contramap(f)
  def IdDec[A](f: String => A): Decoder[A] = Decoder.decodeString.map(f)

  implicit lazy val RouteIdEnc = IdEnc[RouteId](_.value)
  implicit lazy val RouteIdDec = IdDec(RouteId)

  implicit lazy val httpMethodEnc: Encoder[HttpMethod] = Encoder.encodeString.contramap(_.toString)
  implicit lazy val httpMethodDec: Decoder[HttpMethod] = new Decoder[HttpMethod] {
    override def apply(c: HCursor): Result[HttpMethod] =
      c.focus
        .flatMap(_.asString)
        .map(method => Try(HttpMethod.valueOf(method.toUpperCase))) match {
        case Some(util.Success(method)) => Right(method)
        case Some(util.Failure(ex))     =>
          log.error("Could not decode HttpMethod", ex) // TODO make circe propagate DecodingFailure
          Left(DecodingFailure.fromThrowable(ex, c.history))
        case None                  => Left(DecodingFailure("HttpMethod should be String", c.history))
      }
  }

  /**
    * Decodes HttpServerOptions using HttpServerOptions.fromJson
    */
  implicit lazy val httpServerOptionsEnc: Decoder[HttpServerOptions] =
    Decoder.decodeJsonObject
      .map(circeJson => new io.vertx.core.json.JsonObject(circeJson.asJson.toString))
      .emapTry(vertxJson => Try(new HttpServerOptions(vertxJson)))

  /**
    * Encodes HttpServerOptions using HttpServerOptions.toJson
    */
  implicit lazy val httpServerOptionsDec: Encoder[HttpServerOptions] =
    Encoder.encodeJsonObject.contramap { opts =>
      decode[JsonObject](opts.toJson.toString)
        .getOrElse(throw new Exception("Could not convert io.vertx.core.json.JsonObject to io.circe.JsonObject"))
    }

  /**
    * Decodes FilterConf.
    * Tries to decode JSON object to FilterConf, if JSON is string "x" then it's decoded to FilterConf("x", Json.Null)
    */
  implicit lazy val FilterConfDec: Decoder[FilterConf] =
    deriveDecoder[FilterConf].or(Decoder.decodeString.map(name => FilterConf(name, Json.Null)))

  implicit lazy val FilterConfEnc: Encoder[FilterConf] = {
    val enc = deriveEncoder[FilterConf]

    (a: FilterConf) =>
      if (a.conf.isNull) Json.fromString(a.name)
      else               enc.apply(a)
  }
}
