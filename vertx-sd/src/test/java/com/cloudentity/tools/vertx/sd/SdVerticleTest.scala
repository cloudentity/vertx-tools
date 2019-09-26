package com.cloudentity.tools.vertx.sd

import java.util.{List => JList}

import com.cloudentity.tools.vertx.bus.{ServiceClientFactory, VertxBus}
import com.cloudentity.tools.vertx.sd.test.SdTestTools._
import io.vertx.core.json.{JsonObject}
import io.vertx.core.{Future, Vertx}
import io.vertx.ext.unit.junit.{RunTestOnContext, VertxUnitRunner}
import io.vertx.ext.unit.{Async, TestContext}
import io.vertx.servicediscovery.Record
import org.junit.runner.RunWith
import org.junit.{Rule, Test}
import org.scalatest.MustMatchers

@RunWith(classOf[VertxUnitRunner])
class SdVerticleTest extends MustMatchers {
  val _rule = new RunTestOnContext
  @Rule def rule = _rule

  val vertx: Vertx = Vertx.vertx

  @Test def shouldRegisterStaticService(ctx: TestContext): Unit = {
    VertxBus.registerPayloadCodec(vertx.eventBus)
    val async: Async = ctx.async
    // given
    val serviceName: String = "authz"
    val host: String = "localhost"
    val port: Int = 8080
    val conf: JsonObject = new JsonObject().put("name", serviceName).put("location", new JsonObject().put("host", host).put("port", port))
    // when
    deployStaticDiscovery(vertx, conf).compose((x: String) => {
      val sd: SdService = ServiceClientFactory.make(vertx.eventBus, classOf[SdService])
      sd.discover(serviceName)
    }).compose((x: JList[Record]) => {
      // then
      ctx.assertEquals(1, x.size)
      ctx.assertEquals(serviceName, x.get(0).getName)
      ctx.assertEquals(host, x.get(0).getLocation.getString("host"))
      ctx.assertEquals(port, x.get(0).getLocation.getInteger("port"))
      async.complete()
      Future.succeededFuture[Unit]
    }).setHandler(ctx.asyncAssertSuccess[Unit])
  }

  @Test def shouldDiscoverNewNode(ctx: TestContext): Unit = {
    VertxBus.registerPayloadCodec(vertx.eventBus)
    val async = ctx.async

    val serviceName = "service-a"
    val sdProvider = eventBusSdProvider(vertx)
    val sd = ServiceClientFactory.make(vertx.eventBus(), classOf[SdService])

    deployEventBusDiscovery(vertx)
      .compose { _ =>
        // registering first node
        sdProvider.register(serviceName, SmartRecord(NodeId("node-1"), Location("host-1", 80, false, None), Nil))
      }.compose { _ =>
        sd.discover(serviceName)
      }.compose { records: JList[Record] =>
        // assert first node discoverable
        ctx.assertEquals(1, records.size())
        ctx.assertEquals(serviceName, records.get(0).getName)
        ctx.assertEquals("node-1", records.get(0).getMetadata.getString("ID"))

        // registering second node
        sdProvider.register(serviceName, SmartRecord(NodeId("node-2"), Location("host-2", 80, false, None), Nil))
      }.compose { _ =>
        sd.discover(serviceName)
      }.compose { records: JList[Record] =>
        // assert both nodes discoverable
        ctx.assertEquals(2, records.size())
        ctx.assertEquals(serviceName, records.get(0).getName)
        ctx.assertTrue(Set("node-1", "node-2").contains(records.get(0).getMetadata.getString("ID")))
        ctx.assertTrue(Set("node-1", "node-2").contains(records.get(1).getMetadata.getString("ID")))

        async.complete()
        Future.succeededFuture[Void]()
      }.setHandler(ctx.asyncAssertSuccess())
  }

  @Test def shouldNotDiscoverRemovedNode(ctx: TestContext): Unit = {
    VertxBus.registerPayloadCodec(vertx.eventBus)
    val async = ctx.async

    val serviceName = "service-a"
    val sdProvider = eventBusSdProvider(vertx)
    val sd = ServiceClientFactory.make(vertx.eventBus(), classOf[SdService])

    var registrationId: String = ""
    deployEventBusDiscovery(vertx)
      .compose { _ =>
        // registering first node
        sdProvider.register(serviceName, SmartRecord(NodeId("node-1"), Location("host-1", 80, false, None), Nil))
      }.compose { _ =>
        // registering second node
        sdProvider.register(serviceName, SmartRecord(NodeId("node-2"), Location("host-2", 80, false, None), Nil))
      }.compose { regId: String =>
        // saving registrationId
        registrationId = regId
        sd.discover(serviceName)
      }.compose { records: JList[Record] =>
        // assert both nodes discoverable
        ctx.assertEquals(2, records.size())
        ctx.assertEquals(serviceName, records.get(0).getName)
        ctx.assertTrue(Set("node-1", "node-2").contains(records.get(0).getMetadata.getString("ID")))
        ctx.assertTrue(Set("node-1", "node-2").contains(records.get(1).getMetadata.getString("ID")))

        Future.succeededFuture[Void]()
      }.compose { _ =>
        // un-registering second node
        sdProvider.unRegister(registrationId)
      }.compose { _ =>
        sd.discover(serviceName)
      }.compose { records: JList[Record] =>
        // assert first node discoverable
        ctx.assertEquals(1, records.size())
        ctx.assertEquals(serviceName, records.get(0).getName)
        ctx.assertEquals("node-1", records.get(0).getMetadata.getString("ID"))

        async.complete()
        Future.succeededFuture[Void]()
      }.setHandler(ctx.asyncAssertSuccess())
  }
}
