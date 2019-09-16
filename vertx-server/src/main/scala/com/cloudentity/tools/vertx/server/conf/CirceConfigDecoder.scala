package com.cloudentity.tools.vertx.server.conf

import io.circe.{CursorOp, Decoder, DecodingFailure, ParsingFailure}
import com.cloudentity.tools.vertx.bus.ComponentVerticle
import com.cloudentity.tools.vertx.server.conf.CirceConfigDecoder._

import scala.util.{Failure, Success, Try}

trait CirceConfigDecoder { self: ComponentVerticle =>
  /**
    * Decodes A from ComponentVerticle.getConfig. Throws exception on error.
    */
  def decodeConfigUnsafe[A](implicit d: Decoder[A]): A =
    decodeConfigUnsafe(None)

  /**
    * Decodes A from ComponentVerticle.getConfig().getValue(key). Throws exception on error.
    */
  def decodeConfigUnsafe[A](key: String)(implicit d: Decoder[A]): A =
    decodeConfigUnsafe(Some(key))

  private def decodeConfigUnsafe[A](keyOpt: Option[String])(implicit d: Decoder[A]): A =
    decodeConfig[A](keyOpt) match {
      case Right(a) => a
      case Left(error) =>
        error match {
          case ConfigMissing() =>
            throw new Exception(s"Config is missing ${configSignature(keyOpt)}")
          case ConfigDecodingException(ex) =>
            throw new Exception(s"Could not get verticle config ${configSignature(keyOpt)}", ex)
          case ConfigCirceError(ParsingFailure(msg, ex)) =>
            // should never happen since we have JsonObject as input
            throw new Exception(s"Could not parse verticle config: $msg ${configSignature(keyOpt)}", ex)
          case ConfigCirceError(DecodingFailure(msg, ops)) =>
            throw new Exception(s"Could not decode verticle config attribute at '${self.configPath() + keyOpt.map("." + _).getOrElse("")}${CursorOp.opsToPath(ops)}' ${configSignature(keyOpt)}")
        }
    }

  private def decodeConfig[A](keyOpt: Option[String])(implicit d: Decoder[A]): Either[DecodingError, A] =
    extractConfig(keyOpt) match {
      case Success(confString)              => io.circe.parser.decode[A](confString).left.map(ConfigCirceError)
      case Failure(_: NullPointerException) => Left(ConfigMissing())
      case Failure(ex)                      => Left(ConfigDecodingException(ex))
    }

  private def extractConfig(keyOpt: Option[String]): Try[String] = {
    val conf = self.getConfig()
    Try(keyOpt.map(conf.getValue).getOrElse(conf).toString)
  }

  private def configSignature(keyOpt: Option[String]) =
    s"[verticleId=${self.verticleId()}, configPath=${self.configPath() + keyOpt.map("." + _).getOrElse("")}]"
}

object CirceConfigDecoder {
  sealed trait DecodingError
  case class ConfigMissing() extends DecodingError
  case class ConfigDecodingException(ex: Throwable) extends DecodingError
  case class ConfigCirceError(e: io.circe.Error) extends DecodingError
}