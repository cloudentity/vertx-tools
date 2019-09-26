package com.cloudentity.tools.vertx.sd.test

import com.cloudentity.tools.vertx.bus.{ServiceVerticle, VertxEndpoint}
import com.cloudentity.tools.vertx.sd.{SdService, SmartRecord}
import io.vertx.core.json.JsonObject
import io.vertx.core.{Future, Promise, Vertx}
import io.vertx.servicediscovery.spi.{ServiceImporter, ServicePublisher}
import io.vertx.servicediscovery.{Record, Status}

trait EventBusSdProviderService {
  @VertxEndpoint(address = "event-bus-sd-provider.register")
  def register(serviceName: String, record: SmartRecord): Future[String]

  @VertxEndpoint(address = "event-bus-sd-provider.un-register")
  def unRegister(registrationId: String): Future[Void]
}

/**
  * Registers/un-registers SmartRecords in SdVerticle. For testing purposes.
  */
class EventBusSdProvider extends ServiceVerticle with EventBusSdProviderService {

  class EventBusServiceImporter() extends ServiceImporter with EventBusSdProviderService {
    var publisher: ServicePublisher = null

    override def start(vertx: Vertx, publisher: ServicePublisher, configuration: JsonObject, promise: Promise[Void]): Unit = {
      this.publisher = publisher
      promise.complete()
    }

    override def register(serviceName: String, smartRecord: SmartRecord): Future[String] = {
      val promise = Future.future[Record]()
      val record =
        SmartRecord.toRecord(smartRecord)
          .setName(serviceName)
          .setType("http-endpoint").setStatus(Status.UP)

      publisher.publish(record, promise)
      promise.compose(r => Future.succeededFuture(r.getRegistration))
    }

    override def unRegister(registrationId: String): Future[Void] = {
      val promise = Future.future[Void]()
      publisher.unpublish(registrationId, promise)
      promise
    }
  }

  val importer = new EventBusServiceImporter()

  override def initServiceAsync(): Future[Void] =
    createClient(classOf[SdService])
      .registerServiceImporter(importer, new JsonObject())

  override def register(serviceName: String, record: SmartRecord): Future[String] = importer.register(serviceName, record)
  override def unRegister(registrationId: String): Future[Void] = importer.unRegister(registrationId)
}
