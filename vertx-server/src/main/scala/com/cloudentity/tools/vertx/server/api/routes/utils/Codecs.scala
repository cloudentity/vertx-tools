package com.cloudentity.tools.vertx.server.api.routes.utils

import io.circe.{CursorOp, DecodingFailure, ParsingFailure, Printer}

import scala.reflect.ClassTag
import scala.util.Try

sealed trait DecoderError { def msg: String }
  case class ParsingError(msg: String) extends DecoderError
  case class MappingError(msg: String) extends DecoderError

trait VertxEncoder[A] {
  def encode(a: A): String
}

trait VertxDecoder[A] {
  def decode(json: String): Either[DecoderError, A]
}

trait CirceVertxCodecs {
  val printer = Printer.noSpaces.copy(dropNullValues = true)

  implicit def decoder[A](implicit d: io.circe.Decoder[A]) =
    new VertxDecoder[A] {
      override def decode(json: String): Either[DecoderError, A] =
        io.circe.parser.decode[A](json).left.map {
          case x: DecodingFailure => MappingError(s"JSON mapping error at ${CursorOp.opsToPath(x.history)}")
          case x: ParsingFailure  => ParsingError(s"JSON parsing error - ${x.message}")
        }
    }

  implicit def encoder[A](implicit e: io.circe.Encoder[A]) =
    new VertxEncoder[A] {
      override def encode(a: A): String = e(a).printWith(printer)
    }
}

trait JacksonVertxCodecs {
  implicit def decoder[A](implicit ct: ClassTag[A]) =
    new VertxDecoder[A] {
      override def decode(json: String): Either[DecoderError, A] =
        Try(io.vertx.core.json.Json.decodeValue(json, ct.runtimeClass.asInstanceOf[Class[A]])).toEither
          .left.map {
          case ex => MappingError(ex.getMessage)
        }
    }

  implicit def encoder[A] =
    new VertxEncoder[A] {
      override def encode(a: A): String = io.vertx.core.json.Json.encode(a)
    }
}
