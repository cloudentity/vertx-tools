package com.cloudentity.tools.vertx.registry

import com.cloudentity.tools.vertx.scala.{FutureConversions, Futures, VertxExecutionContext}
import io.vertx.config.spi.{ConfigStore, ConfigStoreFactory}
import io.vertx.core
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.core.{AsyncResult, Handler}

import scala.collection.JavaConverters._
import scala.concurrent.Future

class DescriptorsDirectoryConfigStoreFactory extends ConfigStoreFactory with FutureConversions {
  override def name(): String = "descriptors-dir"

  type FileName = String

  override def create(vertx: core.Vertx, configuration: JsonObject): ConfigStore = {
    implicit val ec = VertxExecutionContext(vertx.getOrCreateContext())
    val path = configuration.getString("path")

    (completionHandler: Handler[AsyncResult[Buffer]]) => {
      val op: Future[List[Future[(FileName, Buffer)]]] =
        asFuture[java.util.List[String]](x => vertx.fileSystem().readDir(path, x)).toScala.map { (files: java.util.List[String]) =>
          files.asScala.toList.map { file =>
            println(file)
            asFuture[Buffer](x => vertx.fileSystem().readFile(file, x)).toScala.map((file, _))
          }
        }

      val buffersFut: Future[List[(FileName, Buffer)]] = op.flatMap { readOps =>
        Future.sequence(readOps)
      }

      val bufferFut: Future[Buffer] =
        buffersFut.map { buffers =>
          val buff = Buffer.buffer("{")
          buffers.foreach { case (fileName, buffer) =>
            buff.appendString("\"" + fileName.reverse.dropWhile(_ != '.').drop(1).reverse + "\":" )
            buff.appendBuffer(buffer)
            buff.appendString(",")
          }
          buff.slice(0, buff.length() - 1)
          buff.appendString("}")
        }
      completionHandler.handle(Futures.toJava(bufferFut))
    }
  }
}