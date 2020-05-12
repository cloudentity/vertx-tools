package com.cloudentity.tools.vertx.registry

import java.io.File
import java.util.Optional

import com.google.common.io.Files
import com.cloudentity.tools.vertx.bus.VertxEndpointClient
import com.cloudentity.tools.vertx.conf.ConfVerticleDeploy
import com.cloudentity.tools.vertx.registry.RegistryVerticle.RegistryType
import com.cloudentity.tools.vertx.scala.{FutureConversions, Futures}
import com.cloudentity.tools.vertx.test.VertxUnitTest
import com.cloudentity.tools.vertx.verticles.VertxDeploy
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.core.{AbstractVerticle, Future}
import io.vertx.ext.unit.TestContext
import com.cloudentity.tools.vertx.scala.VertxExecutionContext
import org.junit.{Assert, Test}

import scala.concurrent.duration._

class RegistryVerticleTest() extends VertxUnitTest with FutureConversions {
  @Test
  def shouldSucceedStartupWhenStartingHealthyVerticle(context: TestContext): Unit = {
    // given
    val configFile = copyDescriptorsToTempFile("src/test/resources/registry/one-healthy.json")
    val typ = RegistryType("test")

    val client = VertxEndpointClient.make(vertx, classOf[RegistryService], Optional.of(typ.value))
    // when
    ConfVerticleDeploy.deployFileConfVerticle(vertx, configFile.getAbsolutePath)
      .compose { _ => VertxDeploy.deploy(vertx, new RegistryVerticle(typ)) }
      .compose { _ => client.getVerticleIds }
      .compose { (ids: java.util.List[String]) =>
        // then
        context.assertEquals(1, ids.size)
        context.assertTrue(ids.contains("healthy1-verticle"))

        Future.succeededFuture(())
      }.setHandler(context.asyncAssertSuccess())
  }

  @Test
  def shouldNotDeployDisabledTrueVerticle(context: TestContext): Unit = {
    // given
    val configFile = copyDescriptorsToTempFile("src/test/resources/registry/disabled-healthy.json")
    val typ = RegistryType("test")

    val client = VertxEndpointClient.make(vertx, classOf[RegistryService], Optional.of(typ.value))
    // when
    ConfVerticleDeploy.deployFileConfVerticle(vertx, configFile.getAbsolutePath)
      .compose { _ => VertxDeploy.deploy(vertx, new RegistryVerticle(typ)) }
      .compose { _ => client.getVerticleIds }
      .compose { (ids: java.util.List[String]) =>
        // then
        context.assertEquals(1, ids.size)
        context.assertTrue(ids.contains("healthy1-verticle"))

        Future.succeededFuture(())
      }.setHandler(context.asyncAssertSuccess())
  }

  @Test
  def shouldNotDeployEnabledFalseVerticle(context: TestContext): Unit = {
    // given
    val configFile = copyDescriptorsToTempFile("src/test/resources/registry/enabled-false-healthy.json")
    val typ = RegistryType("test")

    val client = VertxEndpointClient.make(vertx, classOf[RegistryService], Optional.of(typ.value))
    // when
    ConfVerticleDeploy.deployFileConfVerticle(vertx, configFile.getAbsolutePath)
      .compose { _ => VertxDeploy.deploy(vertx, new RegistryVerticle(typ)) }
      .compose { _ => client.getVerticleIds }
      .compose { (ids: java.util.List[String]) =>
        // then
        context.assertEquals(1, ids.size)
        context.assertTrue(ids.contains("healthy1-verticle"))

        Future.succeededFuture(())
      }.setHandler(context.asyncAssertSuccess())
  }

  @Test
  def shouldSucceedStartupWhenStartingRegistryWithHealthyVerticleFromAnotherRegistry(context: TestContext): Unit = {
    // given
    val configFile = copyDescriptorsToTempFile("src/test/resources/registry/registry-in-registry.json")
    val typ = RegistryType("test")

    val client = VertxEndpointClient.make(vertx, classOf[RegistryService], Optional.of(typ.value))
    // when
    ConfVerticleDeploy.deployFileConfVerticle(vertx, configFile.getAbsolutePath)
      .compose { _ => VertxDeploy.deploy(vertx, new RegistryVerticle(RegistryType("super"))) }
      .compose { _ => client.getVerticleIds }
      .compose { (ids: java.util.List[String]) =>
        // then
        context.assertEquals(1, ids.size)
        context.assertTrue(ids.contains("healthy1-verticle"))

        Future.succeededFuture(())
      }.setHandler(context.asyncAssertSuccess())
  }

  @Test
  def shouldFailOnStartupWhenStartingFailingVerticle(context: TestContext): Unit = {
    // given
    val configFile = copyDescriptorsToTempFile("src/test/resources/registry/one-failing.json")

    // when
    ConfVerticleDeploy.deployFileConfVerticle(vertx, configFile.getAbsolutePath)
      .compose { _ => VertxDeploy.deploy(vertx, new RegistryVerticle(RegistryType("test"))) }
      .setHandler(context.asyncAssertFailure())
  }

  @Test
  def shouldSucceedWhenAddingExtraHealthyVerticle(context: TestContext): Unit = {
    implicit val ec = VertxExecutionContext(vertx.getOrCreateContext())

    // given
    val configFile = copyDescriptorsToTempFile("src/test/resources/registry/one-healthy.json")
    val typ = RegistryType("test")

    val client = VertxEndpointClient.make(vertx, classOf[RegistryService], Optional.of(typ.value))
    // when
    ConfVerticleDeploy.deployFileConfVerticle(vertx, configFile.getAbsolutePath, new ConfigRetrieverOptions().setScanPeriod(100))
      .compose { _ => VertxDeploy.deploy(vertx, new RegistryVerticle(typ)) }
      .compose { _ =>
        Files.copy(new File("src/test/resources/registry/two-healthy.json"), configFile)
        Future.succeededFuture(())
      }
      .compose { _ =>
        // then
        TestTools.retest(vertx, 10, 300 millis, () => Futures.toScala(client.getVerticleIds)) { ids =>
          Assert.assertTrue(ids.size() == 2)
          Assert.assertTrue(ids.contains("healthy1-verticle"))
          Assert.assertTrue(ids.contains("healthy2-verticle"))
        }.toJava
      }.setHandler(context.asyncAssertSuccess())
  }

  @Test
  def shouldSucceedWhenRemovingVerticle(context: TestContext): Unit = {
    implicit val ec = VertxExecutionContext(vertx.getOrCreateContext())

    // given
    val configFile = copyDescriptorsToTempFile("src/test/resources/registry/two-healthy.json")
    val typ = RegistryType("test")

    val client = VertxEndpointClient.make(vertx, classOf[RegistryService], Optional.of(typ.value))

    ConfVerticleDeploy.deployFileConfVerticle(vertx, configFile.getAbsolutePath, new ConfigRetrieverOptions().setScanPeriod(100))
      .compose { _ => VertxDeploy.deploy(vertx, new RegistryVerticle(typ)) }
      .compose { _ => client.getVerticleIds }
      .compose { (ids: java.util.List[String]) =>
        // then
        context.assertEquals(2, ids.size)
        context.assertTrue(ids.contains("healthy1-verticle"))
        context.assertTrue(ids.contains("healthy2-verticle"))

        Future.succeededFuture(())
      }.compose { _ =>
        Files.copy(new File("src/test/resources/registry/one-healthy.json"), configFile)
        Future.succeededFuture(())
      }.compose { _ =>
      // then

      TestTools.retest(vertx, 10, 300 millis, () => Futures.toScala(client.getVerticleIds)) { ids =>
        Assert.assertTrue(ids.size() == 1)
        Assert.assertTrue(ids.contains("healthy1-verticle"))
      }.toJava
    }.setHandler(context.asyncAssertSuccess())
  }

  @Test
  def shouldSucceedStartupOfVerticleWithConfig(context: TestContext): Unit = {
    // given
    val configFile = copyDescriptorsToTempFile("src/test/resources/registry/verticle-config.json")
    val typ = RegistryType("test")

    val client = VertxEndpointClient.make(vertx, classOf[RegistryService], Optional.of(typ.value))
    // when
    ConfVerticleDeploy.deployFileConfVerticle(vertx, configFile.getAbsolutePath)
      .compose { _ => VertxDeploy.deploy(vertx, new RegistryVerticle(typ)) }
      .compose { _ => client.getVerticleIds }
      .compose { (ids: java.util.List[String]) =>
        // then
        context.assertEquals(1, ids.size)
        context.assertTrue(ids.contains("verticle-with-config"))

        Future.succeededFuture(())
      }.setHandler(context.asyncAssertSuccess())
  }

  @Test
  def shouldSucceedStartupOfVerticleWithAddressPrefix(context: TestContext): Unit = {
    // given
    val configFile = copyDescriptorsToTempFile("src/test/resources/registry/verticle-prefix.json")
    val typ = RegistryType("test")

    val client = VertxEndpointClient.make(vertx, classOf[RegistryService], Optional.of(typ.value))
    // when
    ConfVerticleDeploy.deployFileConfVerticle(vertx, configFile.getAbsolutePath)
      .compose { _ => VertxDeploy.deploy(vertx, new RegistryVerticle(typ)) }
      .compose { _ => client.getVerticleIds }
      .compose { (ids: java.util.List[String]) =>
        // then
        context.assertEquals(1, ids.size)
        context.assertTrue(ids.contains("verticle-with-prefix"))

        Future.succeededFuture(())
      }.setHandler(context.asyncAssertSuccess())
  }

  @Test
  def shouldSucceedStartupVerticleWithCpuDeploymentStrategy(context: TestContext): Unit = {
    StatefulVerticle.counter = 0

    // given
    val configFile = copyDescriptorsToTempFile("src/test/resources/registry/cpu-deployment-strategy.json")
    val typ = RegistryType("test")

    val verticle = new RegistryVerticle(typ) {
      override def getAvailableCpus(): Int = 2
    }

    // when
    ConfVerticleDeploy.deployFileConfVerticle(vertx, configFile.getAbsolutePath)
      .compose { _ => VertxDeploy.deploy(vertx, verticle) }
      .compose { _ =>
        context.assertEquals(2, StatefulVerticle.counter)

        Future.succeededFuture(())
      }.setHandler(context.asyncAssertSuccess())
  }

  @Test
  def shouldSucceedStartupVerticleWithDefaultCpuDeploymentStrategy(context: TestContext): Unit = {
    StatefulVerticle.counter = 0

    // given
    val configFile = copyDescriptorsToTempFile("src/test/resources/registry/cpu-default-deployment-strategy.json")
    val typ = RegistryType("test")

    val verticle = new RegistryVerticle(typ) {
      override def getAvailableCpus(): Int = 2
    }

    // when
    ConfVerticleDeploy.deployFileConfVerticle(vertx, configFile.getAbsolutePath)
      .compose { _ => VertxDeploy.deploy(vertx, verticle) }
      .compose { _ =>
        context.assertEquals(2, StatefulVerticle.counter)

        Future.succeededFuture(())
      }.setHandler(context.asyncAssertSuccess())
  }

  def copyDescriptorsToTempFile(sourceConfigPath: String): File = {
    val tempDir = Files.createTempDir()
    val from = new File(sourceConfigPath)

    val to = new File(tempDir, "conf.json")
    Files.copy(from, to)
    to
  }
}

class HealthyTestVerticle extends AbstractVerticle {

}

class FailingTestVerticle extends AbstractVerticle {
  override def start() = {
    throw new Exception("failure")
  }
}

class TestVerticleWithConfig extends AbstractVerticle {
  override def start() = {
    Assert.assertEquals("registry:test.verticle-with-config.verticleConfig", config().getString("configPath"))
  }
}

class TestVerticleWithPrefix extends AbstractVerticle {
  override def start() = {
    Assert.assertEquals("address-prefix", config().getString("prefix"))
  }
}

object StatefulVerticle {
  var counter = 0
}

class StatefulVerticle extends AbstractVerticle {
  override def start() = {
    synchronized {
      StatefulVerticle.counter += 1
    }
  }
}