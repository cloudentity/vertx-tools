package com.cloudentity.tools.vertx.sd

import com.cloudentity.tools.vertx.bus.VertxBus
import com.cloudentity.tools.vertx.sd.SdTestTools._
import io.vertx.core.{Future, Vertx}
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.{RunTestOnContext, VertxUnitRunner}
import org.junit.{Rule, Test}
import org.junit.runner.RunWith
import org.scalatest.MustMatchers
import java.util.{List => JList}

import io.vertx.circuitbreaker.CircuitBreakerOptions
import io.vertx.servicediscovery.Record

@RunWith(classOf[VertxUnitRunner])
class SmartSdTest extends MustMatchers {
  val _rule = new RunTestOnContext
  @Rule def rule = _rule

  val vertx: Vertx = Vertx.vertx

  val cbOpts = new CircuitBreakerOptions()

  @Test def shouldDiscoverStaticService(ctx: TestContext): Unit = {
    VertxBus.registerPayloadCodec(vertx.eventBus())
    val async = ctx.async()

    // given
    val serviceName = "authz"
    val host = "localhost"
    val port = 8080
    val rootPath = "/root-path"
    val conf =
      new JsonObject()
        .put("name", serviceName)
        .put("location", new JsonObject()
          .put("host", host)
          .put("port", port)
          .put("ssl", false)
          .put("root", rootPath)
        ).put("metadata", new JsonObject().put("ID", "x"))

    // when
    deployStaticDiscovery(vertx, conf)
      .compose(_ => SmartSd.withRoundRobinDiscovery(vertx, ServiceName.apply(serviceName), Nil, cbOpts))
      .compose { sd: SmartSd =>
        // then
        ctx.assertEquals(true, sd.discover().isDefined)
        ctx.assertEquals(host, sd.discover().get.location.host)
        ctx.assertEquals(port, sd.discover().get.location.port)
        ctx.assertEquals(Some(rootPath), sd.discover().get.location.root)

        async.complete()
        Future.succeededFuture[Void]()
      }.setHandler(ctx.asyncAssertSuccess())
  }

  val serviceName = "service-a"
  val location1 = Location("host-1", 80, false, None)
  val location2 = Location("host-2", 80, false, None)

  def waitForNodeRepublish(): Future[Void] = {
    val promise = Future.future[Void]()
    VertxBus.consumePublished(
      vertx.eventBus, s"${SdVerticle.SERVICE_DISCOVERY_REFRESH_ADDR}.${serviceName}",
      classOf[java.util.List[Record]], (_: JList[Record]) =>  promise.complete()
    )
    promise
  }

  @Test def shouldDiscoverNewNode(ctx: TestContext): Unit = {
    VertxBus.registerPayloadCodec(vertx.eventBus)

    val sdProvider = eventBusSdProvider(vertx)
    var sd: SmartSd = null

    deployEventBusDiscovery(vertx)
      .compose(_ => SmartSd.withRoundRobinDiscovery(vertx, ServiceName.apply(serviceName), Nil, cbOpts))
      .compose { smartSd =>
        sd = smartSd
        Future.succeededFuture[Void]()
      }.compose { _ =>
        // registering first node
        sdProvider.register(serviceName, SmartRecord(NodeId("node-1"), location1, Nil))
      }.compose { _ =>
        waitForNodeRepublish
      }.compose { _ =>
        // make sure there is only one node available
        assertAvailableNodes(sd, Set(location1))

        Future.succeededFuture[Void]()
      }.compose { _ =>
        // registering second node
        sdProvider.register(serviceName, SmartRecord(NodeId("node-2"), location2, Nil))
      }.compose { _ =>
        waitForNodeRepublish
      }.compose { _ =>
        // make sure there there are two nodes available
        assertAvailableNodes(sd, Set(location1, location2))

        Future.succeededFuture[Void]()
      }.setHandler(ctx.asyncAssertSuccess())
  }

  @Test def shouldNotDiscoverRemovedNode(ctx: TestContext): Unit = {
    VertxBus.registerPayloadCodec(vertx.eventBus)

    val sdProvider = eventBusSdProvider(vertx)
    var sd: SmartSd = null
    var registrationId: String = null

    deployEventBusDiscovery(vertx)
      .compose(_ => SmartSd.withRoundRobinDiscovery(vertx, ServiceName.apply(serviceName), Nil, cbOpts))
      .compose { smartSd =>
        sd = smartSd
        Future.succeededFuture[Void]()
      }
      .compose { _ =>
        // registering first node
        sdProvider.register(serviceName, SmartRecord(NodeId("node-1"), location1, Nil))
      }.compose { _ =>
        // registering second node
        sdProvider.register(serviceName, SmartRecord(NodeId("node-2"), location2, Nil))
      }.compose { regId: String =>
        // saving registrationId
        registrationId = regId
        Future.succeededFuture[Void]()
      }.compose { _ =>
        waitForNodeRepublish
      }.compose { _ =>
        // make sure there there are two nodes available
        assertAvailableNodes(sd, Set(location1, location2))

        Future.succeededFuture[Void]()
      }.compose { _ =>
        sdProvider.unRegister(registrationId)
      }.compose { _ =>
        waitForNodeRepublish
      }.compose { _ =>
        // make sure there is only one node available
        assertAvailableNodes(sd, Set(location1))

        Future.succeededFuture[Void]()
      }.setHandler(ctx.asyncAssertSuccess())
  }

  @Test def shouldNotDiscoverNodeWithMissingTag(ctx: TestContext): Unit = {
    VertxBus.registerPayloadCodec(vertx.eventBus)

    val sdProvider = eventBusSdProvider(vertx)
    var sd: SmartSd = null
    var registrationId: String = null

    deployEventBusDiscovery(vertx)
      .compose(_ => SmartSd.withRoundRobinDiscovery(vertx, ServiceName.apply(serviceName), List("tag"), cbOpts))
      .compose { smartSd =>
        sd = smartSd
        Future.succeededFuture[Void]()
      }
      .compose { _ =>
        // registering first node
        sdProvider.register(serviceName, SmartRecord(NodeId("node-1"), location1, List("tag")))
      }.compose { _ =>
      // registering second node
      sdProvider.register(serviceName, SmartRecord(NodeId("node-2"), location2, Nil))
    }.compose { regId: String =>
      // saving registrationId
      registrationId = regId
      Future.succeededFuture[Void]()
    }.compose { _ =>
      waitForNodeRepublish
    }.compose { _ =>
      // make sure there there are node with tag available
      assertAvailableNodes(sd, Set(location1))

      Future.succeededFuture[Void]()
    }.setHandler(ctx.asyncAssertSuccess())
  }

  private def assertAvailableNodes(sd: SmartSd, expectedNodes: Set[Location]) = {
    val nodes =
      List(sd.discover(), sd.discover(), sd.discover(), sd.discover())
        .flatten.map(_.location).toSet
    nodes must be(expectedNodes)
  }
}
