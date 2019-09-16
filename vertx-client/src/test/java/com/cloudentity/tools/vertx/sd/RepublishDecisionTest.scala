package com.cloudentity.tools.vertx.sd

import com.cloudentity.tools.vertx.sd.SdVerticle.RepublishDecision
import io.vertx.core.json.JsonObject
import io.vertx.servicediscovery.Record
import org.junit.Test
import org.scalatest.MustMatchers

class RepublishDecisionTest extends MustMatchers {
  val service = "service-a"

  val recordA1 = new Record(new JsonObject().put("location", new JsonObject().put("host", "host-a-1")))
  val recordA2 = new Record(new JsonObject().put("location", new JsonObject().put("host", "host-a-2")))

  @Test def testMakeRepublishDecision_when_nothing_discovered: Unit = {
    SdVerticle.makeRepublishDecision(Map(), Map()) must be (RepublishDecision(Map(), List(), List()))
  }

  @Test def testMakeRepublishDecision_when_new_service_discovered: Unit = {
    SdVerticle.makeRepublishDecision(Map(service -> List(recordA1)), Map()) must be (RepublishDecision(Map(service -> List(recordA1)), List(), List()))
  }

  @Test def testMakeRepublishDecision_service_removed: Unit = {
    SdVerticle.makeRepublishDecision(Map(), Map(service -> List(recordA1))) must be (RepublishDecision(Map(), List(), List(service)))
  }

  @Test def testMakeRepublishDecision_service_not_changed: Unit = {
    SdVerticle.makeRepublishDecision(Map(service -> List(recordA1)), Map(service -> List(recordA1))) must be (RepublishDecision(Map(), List(service), List()))
  }

  @Test def testMakeRepublishDecision_service_changed: Unit = {
    SdVerticle.makeRepublishDecision(Map(service -> List(recordA1, recordA2)), Map(service -> List(recordA1))) must be (RepublishDecision(Map(service -> List(recordA1, recordA2)), List(), List()))
    SdVerticle.makeRepublishDecision(Map(service -> List(recordA2)), Map(service -> List(recordA1))) must be (RepublishDecision(Map(service -> List(recordA2)), List(), List()))
  }
}
