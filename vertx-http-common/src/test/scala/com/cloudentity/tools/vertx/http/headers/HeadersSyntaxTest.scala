package com.cloudentity.tools.vertx.http.headers

import com.cloudentity.tools.vertx.http.Headers
import com.cloudentity.tools.vertx.http.headers.Authorization._
import com.cloudentity.tools.vertx.http.headers.ContentType._
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{MustMatchers, WordSpec}

@RunWith(classOf[JUnitRunner])
class HeadersSyntaxTest extends WordSpec with MustMatchers with HeadersSyntax {
  "Headers.withAuthorization" should {
    "create Bearer" in {
      Headers().withAuthorization.bearer("xxx") must be(Headers("Authorization" -> List("Bearer xxx")))
      Headers().withAuthorization(BearerAuthorization("xxx")) must be(Headers("Authorization" -> List("Bearer xxx")))
    }

    "create other" in {
      Headers().withAuthorization.other("Basic xxx") must be(Headers("Authorization" -> List("Basic xxx")))
      Headers().withAuthorization(OtherAuthorization("Basic xxx")) must be(Headers("Authorization" -> List("Basic xxx")))
    }
  }

  "Headers.withContentType" should {
    "create application/json" in {
      Headers().withContentType.applicationJson must be(Headers("Content-Type" -> List("application/json")))
      Headers().withContentType(ApplicationJson) must be(Headers("Content-Type" -> List("application/json")))
    }

    "create application/x-yaml" in {
      Headers().withContentType.applicationYaml must be(Headers("Content-Type" -> List("application/x-yaml")))
      Headers().withContentType(ApplicationYaml) must be(Headers("Content-Type" -> List("application/x-yaml")))
    }

    "create other" in {
      Headers().withContentType.other("application/xml") must be(Headers("Content-Type" -> List("application/xml")))
      Headers().withContentType(OtherContentType("application/xml")) must be(Headers("Content-Type" -> List("application/xml")))
    }
  }
}
