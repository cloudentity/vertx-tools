package com.cloudentity.tools.vertx.http

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{MustMatchers, WordSpec}

@RunWith(classOf[JUnitRunner])
class HeadersTest extends WordSpec with MustMatchers {
  "Headers" should {
    "be case-insensitive on get" in {
      val hs =
        Headers("Content-Length" -> List("100"))
      hs.get("content-length") must be(Some("100"))
    }

    "be case-insensitive on getAll" in {
      val hs =
        Headers("Content-Length" -> List("100"))
      hs.getValues("content-length") must be(Some(List("100")))
    }

    "be case-insensitive on add" in {
      val hs =
        Headers("Content-Length" -> List("100"))
          .add("content-length", "200")
      hs.getValues("content-length") must be(Some(List("100", "200")))
    }

    "be case-insensitive on set" in {
      val hs =
        Headers("Content-Length" -> List("100"))
          .set("content-length", "200")
      hs.getValues("content-length") must be(Some(List("200")))
    }

    "be case-insensitive on setAll" in {
      val hs =
        Headers("Content-Length" -> List("100"))
          .setValues("content-length", List("200", "300"))
      hs.getValues("content-length") must be(Some(List("200", "300")))
    }

    "be case-insensitive on addAll" in {
      val hs =
        Headers("Content-Length" -> List("100"))
          .addValues("content-length", List("200", "300"))
      hs.getValues("content-length") must be(Some(List("100", "200", "300")))
    }

    "be case-insensitive on addMap" in {
      val hs =
        Headers("CONTENT-LENGTH" -> List("100"))
          .addHeaders(Map("content-length" -> "200", "Content-Length" -> "300"))
      hs.getValues("content-length") must be(Some(List("100", "200", "300")))
    }

    "be case-insensitive on addMapAll" in {
      val hs =
        Headers("CONTENT-LENGTH" -> List("100"))
          .addMultiHeaders(Map("content-length" -> List("200", "300"), "Content-Length" -> List("400")))
      hs.getValues("content-length") must be(Some(List("100", "200", "300", "400")))
    }

    "be case-insensitive on remove all values" in {
      val hs =
        Headers("Content-Length" -> List("100"))
          .remove("content-length")
      hs.getValues("content-length") must be(None)
    }

    "be case-insensitive on remove single value" in {
      val hs =
        Headers("Content-Length" -> List("100", "200"))
          .remove("content-length", "200")
      hs.getValues("content-length") must be(Some(List("100")))
    }

    "be case-insensitive on contains header" in {
      val hs =
        Headers("Content-Length" -> List("100"))
      hs.contains("content-length") must be(true)
      hs.contains("not-exists") must be(false)
    }

    "be case-insensitive on contains value" in {
      val hs =
        Headers("Content-Length" -> List("100"))
      hs.contains("content-length", "100") must be(true)
      hs.contains("content-length", "200") must be(false)
    }
  }
}