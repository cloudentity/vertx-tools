package com.cloudentity.tools.vertx.test

import com.cloudentity.tools.vertx.bus.ServiceVerticle

abstract class ScalaServiceVerticleUnitTest[B <: ServiceVerticle, A] extends ServiceVerticleUnitTest[B, A] with ScalaVertxTestHelper
