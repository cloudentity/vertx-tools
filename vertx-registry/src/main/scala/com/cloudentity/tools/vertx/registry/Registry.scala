package com.cloudentity.tools.vertx.registry

import java.util

import RegistryVerticle._
import com.cloudentity.tools.vertx.scala.bus.ScalaServiceVerticle
import com.cloudentity.tools.vertx.verticles.VertxDeploy
import io.vertx.core.impl.NoStackTraceThrowable
import io.vertx.core.json.JsonObject
import io.vertx.core.{AsyncResult, DeploymentOptions, Future => VxFuture}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scalaz._
import Scalaz._
import com.cloudentity.tools.vertx.logging.InitLog

object RegistryVerticle {
  case class DeploymentId(value: String)
  case class VerticleId(value: String)
  case class Deployment(id: DeploymentId, verticleId: VerticleId, descriptor: VerticleDescriptor)

  case class VerticleDescriptor(
    main: String,
    enabled: Boolean,
    options: JsonObject,
    configPath: Option[String],
    verticleConfig: Option[JsonObject],
    prefix: Option[AddressPrefix],
    deploymentStrategy: DeploymentStrategy,
    dependsOn: List[VerticleId]
  )

  case class RegistryType(value: String)

  sealed trait AddressPrefix
    case object VerticleIdAddressPrefix extends AddressPrefix
    case class CustomAddressPrefix(value: String) extends AddressPrefix

  sealed trait DeploymentStrategy
    case object SimpleDeploymentStrategy extends DeploymentStrategy // deploys 'options.instances' verticles
    case object CpuDeploymentStrategy extends DeploymentStrategy // deploys 'number of cpus' verticles
    case object Cpux2DeploymentStrategy extends DeploymentStrategy // deploys 'number of cpus' x 2 verticles

  def deploy(vertx: io.vertx.core.Vertx, registryType: String): io.vertx.core.Future[String] =
    VertxDeploy.deploy(vertx, new RegistryVerticle(RegistryType(registryType)))

  def deploy(vertx: io.vertx.core.Vertx, registryType: String, isConfRequired: Boolean): io.vertx.core.Future[String] =
    VertxDeploy.deploy(vertx, new RegistryVerticle(RegistryType(registryType), isConfRequired))
}

/**
  * Deploys verticles defined in configuration at 'registry:{`registryType.value`}' path.
  *
  * If `isConfRequired` is true and configuration object is missing then Registry's start-up fails
  *
  * Configuration schema:
  *
  * {
  *   "verticleId-1": {
  *     "main": "com.cloudentity.verticles.SomeVerticle",
  *     "options": {
  *       "instances": 2
  *     }
  *   },
  *   "verticleId-2": {
  *     "main": "com.cloudentity.verticles.SomeOtherVerticle"
  *   }
  * }
  *
  * The JSON object at the specific verticle-id attribute is used to populate io.vertx.core.DeploymentOptions
  */
class RegistryVerticle(_registryType: RegistryType, isConfRequired: Boolean) extends ScalaServiceVerticle with RegistryService {
  def this(typ: RegistryType) = this(typ, true)

  /**
    * This constructor is used by io.vertx.core.Vertx.deployVerticle method deploying using another RegistryVerticle.
    * RegistryType is taken from 'verticleId' configuration attribute in io.vertx.core.DeploymentOptions.
    */
  def this() = this(null, true)

  lazy val log = LoggerFactory.getLogger(this.getClass + Option(_registryType).map(":" + _.value).getOrElse(""))
  val initLog = InitLog.of(log)

  val internalConfigKey = "config"

  def registryType: RegistryType = {
    Option(_registryType)
      .getOrElse(RegistryType(context.config().getString("verticleId")))
  }

  import scala.collection.JavaConverters._
  var registry = Map[VerticleId, Deployment]()

  override def initServiceAsyncS(): Future[Unit] = {
    if (registryType.value == null) Future.failed(new Exception("Registry type not configured properly"))
    else if (getConfig() == null) abortInit()
    else {
      val activeDescriptors = getActiveDescriptors().get
      val deployment: Future[List[Throwable \/ Deployment]] = deployVerticles(activeDescriptors, registry.keys)

      handleDeploymentResult(deployment)

      deployment.flatMap { results =>
        val oks: List[Deployment]  = results.collect { case \/-(ok) => ok }
        val kos: List[Throwable]  = results.collect { case -\/(ko) => ko }

        if (kos.isEmpty) {
          val message = s"'${registryType.value}' registry started successfully"
          initLog.info(message)
          setupChangeListener()

          Future.successful(())
        } else {
          oks.foreach { d =>
            log.debug(s"Undeploying '${d.verticleId.value}' verticle")
            vertx.undeploy(d.id.value, { (x: AsyncResult[_]) =>
              if (x.succeeded()) log.info(s"Undeployed '${d.verticleId.value}' verticle successfully (cleaning up failed registry)")
              else log.error(s"Undeploying '${d.verticleId.value}' verticle failed (cleaning up failed registry)", x.cause())
            })
          }
          val message = s"Some verticles deployment failed: ${kos.map(x => s"'${x.getMessage}'").mkString(", ")}"
          Future.failed(new NoStackTraceThrowable(message))
        }
      }
    }
  }

  private def readDefaultDeploymentStrategy(): Either[String, DeploymentStrategy] =
    Try {
      val obj = getConfig().getJsonObject(internalConfigKey, new JsonObject())

      if (obj.getString("main") != null)
        throw new NoStackTraceThrowable("'config' key is reserved. Cannot be used as verticle id") // for backward-compatibility issue - it will fail at app startup if a registry has verticle with 'config' id
      else Option(obj.getString("defaultDeploymentStrategy"))
    }.toEither.left.map(_ => "Could not read 'defaultDeploymentStrategy'").flatMap(decodeDeploymentStrategy)

  private def readAllDescriptors(): Either[List[String], Map[VerticleId, VerticleDescriptor]] = {
    val readResults: List[Either[String, (VerticleId, VerticleDescriptor)]] =
      getConfig().fieldNames().asScala.toList.flatMap { name =>
        if (name != internalConfigKey) Some(readDescriptor(name, getConfig().getJsonObject(name)))
        else None
      }

    val oks = readResults.collect { case Right((id, descriptor)) => id -> descriptor }.toMap
    val kos = readResults.collect { case Left(error) => error }

    if (kos.isEmpty) Right(oks)
    else Left(kos)
  }

  private def readDescriptor(name: String, obj: JsonObject): Either[String, (VerticleId, VerticleDescriptor)] = {
    def readDeploymentStrategy(): Either[String, DeploymentStrategy] =
      for {
        defaultDeploymentStrategy <- readDefaultDeploymentStrategy()
        rawDeployStrategyOpt      <- Try(Option(obj.getString("deploymentStrategy"))).toEither.left.map(_ => "Could not read 'deploymentStrategy' attribute")
        deployStrategy            <- rawDeployStrategyOpt match {
                                       case Some(raw) => decodeDeploymentStrategy(Some(raw))
                                       case None     => Right(defaultDeploymentStrategy)
                                     }
      } yield (deployStrategy)

    def readAddressPrefix(): Either[String, Option[AddressPrefix]] =
      Try[Option[AddressPrefix]] {
        Option(obj.getBoolean("prefix")).flatMap { isVerticleIdAddress =>
          if (isVerticleIdAddress) Some(VerticleIdAddressPrefix) else None
        }
      }.recoverWith { case _ =>
        Try {
          Option(obj.getString("prefix")).map { address =>
            CustomAddressPrefix(address)
          }
        }
      }.toEither.left.map(_ => "Could not read 'prefix' attribute")

    for {
      main              <- Try(Option(obj.getString("main"))).toOption.flatten.toRight("Could not read 'main' attribute")
      disabledOpt       <- Try(Option(obj.getValue("disabled")).map(_.asInstanceOf[Boolean])).toEither.left.map(_ => "Could not read 'disabled' attribute")
      enabledOpt        <- Try(Option(obj.getValue("enabled")).map(_.asInstanceOf[Boolean])).toEither.left.map(_ => "Could not read 'enabled' attribute")
      enabled           <- if (enabledOpt.isDefined && disabledOpt.isDefined && enabledOpt == disabledOpt)
                             Left("Both 'enabled' and 'disabled' cannot be set")
                           else Right(enabledOpt.orElse(disabledOpt.map(!_)).getOrElse(true))
      optionsOpt        <- Try(Option(obj.getJsonObject("options"))).toEither.left.map(_ => "Could not read 'options' attribute")
      configPathOpt     <- Try(Option(obj.getString("configPath"))).toEither.left.map(_ => "Could not read 'configPath' attribute")
      verticleConfigOpt <- Try(Option(obj.getJsonObject("verticleConfig"))).toEither.left.map(_ => "Could not read 'verticleConfig' attribute")
      prefixOpt         <- readAddressPrefix()
      deployStrategy    <- readDeploymentStrategy()
      dependsOn         <- Try(Option(obj.getJsonArray("dependsOn")).map(_.getList.asScala.toList.map(id => VerticleId(id.toString))).getOrElse(Nil)).toEither.left.map(_ => "Could not read 'verticleConfig' attribute")
    } yield VerticleDescriptor(main, enabled, optionsOpt.getOrElse(new JsonObject()), configPathOpt, verticleConfigOpt, prefixOpt, deployStrategy, dependsOn)
  }.map(VerticleId(name) -> _).left.map(error => s"Error decoding descriptor of verticle '$name': $error")

  private def decodeDeploymentStrategy(deployStrategyOpt: Option[String]): Either[String, DeploymentStrategy] =
    deployStrategyOpt match {
      case Some("simple") => Right(SimpleDeploymentStrategy)
      case Some("cpu")    => Right(CpuDeploymentStrategy)
      case Some("cpux2")  => Right(Cpux2DeploymentStrategy)
      case Some(x)        => Left(s"Unsupported deployment strategy '$x'")
      case None           => Right(SimpleDeploymentStrategy)
    }

  private def abortInit() =
    if (isConfRequired)
      Future.failed(new NoStackTraceThrowable(s"Could not start '${registryType.value}' verticle Registry. Missing 'registry:${registryType.value}' configuration."))
    else {
      val message = s"Skipping verticle Registry '${registryType.value}' start. Configuration 'registry:${registryType.value}' missing"
      initLog.debug(message)
      Future.successful(())
    }

  private def getActiveDescriptors(): Try[Map[VerticleId, VerticleDescriptor]] = Try {
    val descriptors: Map[VerticleId, VerticleDescriptor] =
      readAllDescriptors() match {
        case Right(ds) => ds
        case Left(errors) => throw new NoStackTraceThrowable(s"Could not read verticle descriptors: ${errors.mkString("; ")}")
      }

    val message = s"Found descriptors for following verticles: [${descriptors.map(_._1.value).mkString(", ")}]"
    initLog.debug(message)

    val (disabledDescriptors, activeDescriptors) = descriptors.partition((disabledVerticle _).tupled)

    if (disabledDescriptors.nonEmpty) {
      val disabledMessage = s"Disabled verticles: [${disabledDescriptors.map(_._1.value).mkString(", ")}]"
      initLog.info(disabledMessage)
    }

    activeDescriptors
  }

  private def disabledVerticle(id: VerticleId, descriptor: VerticleDescriptor): Boolean =
    !descriptor.enabled

  private def setupChangeListener(): Unit =
    registerConfChangeConsumer { change =>
      val prev = change.getPreviousConfiguration.getJsonObject(verticleId, new JsonObject())
      val next = change.getNewConfiguration.getJsonObject(verticleId, new JsonObject())
      log.debug(s"Verticle descriptors changed, prev=$prev, next=$next")

      val toRemoveNames = prev.fieldNames().asScala.toSet -- next.fieldNames().asScala.toSet
      val toAddNames = next.fieldNames().asScala.toSet -- prev.fieldNames().asScala.toSet

      val toRemove = toRemoveNames.map(name => VerticleId(name)).toList

      toAddNames.map(name => readDescriptor(name, next.getJsonObject(name))).toList.sequenceU match {
        case Right(toAdd) =>
          handleDeploymentResult(deployVerticles(toAdd.toMap, registry.keys))
          handleUndeploymentResult(undeployVerticles(toRemove))
        case Left(error) =>
          log.error(s"Could not refresh '${registryType.value}' verticle registry. Malformed verticle descriptor: $error")
      }
    }

  def deployVerticles(verticles: Map[VerticleId, VerticleDescriptor], alreadyDeployed: Iterable[VerticleId]): Future[List[Throwable \/ Deployment]] = {
    def relaxDependsOn(desc: VerticleDescriptor, deployed: Iterable[VerticleId]): VerticleDescriptor =
      desc.copy(dependsOn = (desc.dependsOn.toSet -- deployed).toList)

    def rec(toDeploy: Map[VerticleId, VerticleDescriptor], agg: Future[List[Throwable \/ Deployment]]): Future[List[Throwable \/ Deployment]] =
      if (toDeploy.isEmpty) {
        agg
      } else {
        val free = toDeploy.filter { case (_, desc) => desc.dependsOn.isEmpty }
        if (free.isEmpty) {
          Future.failed(new Exception(s"Could not deploy verticles in ${registryType} registry due unfulfilled dependencies: ${toDeploy.mapValues(_.dependsOn)}"))
        } else {
          val next =
            agg.flatMap { results =>
              if (results.find(_.isLeft).isDefined) {
                Future.successful(results) // skipping deployment of next verticles, because some previous deployments failed
              } else {
                Future.sequence {
                  free.map { case (verticleId, descriptor) => deployVerticle(verticleId, descriptor) }.toList
                }.map(results ::: _)
              }
            }

          rec((toDeploy -- free.keys).mapValues(desc => relaxDependsOn(desc, free.keys)), next)
        }

      }

    rec(verticles.mapValues(relaxDependsOn(_, alreadyDeployed)), Future.successful(Nil))
  }

  def deployVerticle(verticleId: VerticleId, descriptor: VerticleDescriptor): Future[Throwable \/ Deployment] = {
    val opts = descriptor.options.put("config", buildConfig(verticleId, descriptor))
    log.debug(s"Deploying verticle ${buildDeploymentLogObj(verticleId.value, descriptor)}")

    val deploymentOptions = new DeploymentOptions(opts)

    descriptor.deploymentStrategy match {
      case SimpleDeploymentStrategy => // do nothing
      case CpuDeploymentStrategy    => deploymentOptions.setInstances(getAvailableCpus())
      case Cpux2DeploymentStrategy  => deploymentOptions.setInstances(getAvailableCpus() * 2)
    }

    VertxDeploy.deploy(vertx, descriptor.main, deploymentOptions).toScala
      .map[Throwable \/ Deployment] { did =>
        \/-(Deployment(DeploymentId(did), verticleId, descriptor))
      }
      .recover { case ex: Throwable => {
        val wrappedEx = new Exception(buildDeploymentLogObj(verticleId.value, descriptor).toString, ex)
        wrappedEx.setStackTrace(Array())
        -\/(wrappedEx)
      }
    }
  }

  def getAvailableCpus(): Int =
    Runtime.getRuntime.availableProcessors

  private def buildConfig(verticleId: VerticleId, descriptor: VerticleDescriptor) = {
    val opts = descriptor.options

    val config = Option(opts.getJsonObject("config")).getOrElse(new JsonObject())

    config.put("verticleId", verticleId.value)
    descriptor.configPath
      .orElse {
        descriptor.verticleConfig.map(_ => s"${this.verticleId}.${verticleId.value}.verticleConfig")
      }
      .foreach { configPath => config.put("configPath", configPath) }

    descriptor.prefix.foreach {
      case VerticleIdAddressPrefix    => config.put("prefix", true)
      case CustomAddressPrefix(value) => config.put("prefix", value)
    }

    config
  }

  def undeployVerticles(verticles: List[VerticleId]): Future[List[Throwable \/ VerticleId]] =
    Future.sequence(verticles.map(undeployVerticle))

  private def undeployVerticle(id: VerticleId): Future[Throwable \/ VerticleId] =
    registry.get(id) match {
      case Some(depl) =>
        log.info(s"Undeploying ${buildDeploymentLogObj(depl.verticleId.value, depl.descriptor)}")
        VertxDeploy.undeploy(vertx, depl.id.value).toScala
          .map { _ => \/-(id) }.recover { case ex: Throwable =>
            -\/(new Exception(s"Could not undeploy '${depl.verticleId.value}'", ex))
          }
      case None =>
        Future.successful(-\/(new Exception(s"Could not undeploy verticle '${id.value}'. Missing in registry")))
    }

  private def handleDeploymentResult(deployment: Future[List[Throwable \/ Deployment]]): Unit =
    deployment.onComplete {
      case Success(results) =>
        val oks: List[Deployment] = results.collect { case \/-(ok) => ok }
        val kos: List[Throwable]  = results.collect { case -\/(ko) => ko }

        if (!kos.isEmpty) {
          // TODO notify health-check something is wrong
          kos.foreach(ex => {
            initLog.error("Could not deploy verticle", ex)
          })
        } else {
          // TODO notify health-check verticles deployment is OK
        }
        oks.foreach { depl =>
          val logObj = buildDeploymentLogObj(depl.verticleId.value, depl.descriptor)
          initLog.debug(s"Successfully deployed verticle $logObj")
          initLog.info(s"Successfully deployed verticle ${depl.verticleId.value}")
          registry += (depl.verticleId -> depl)
        }
      case Failure(ex) => {
        initLog.error("Unexpected error on verticles deployment", ex)
      }
    }

  private def buildDeploymentLogObj(verticleId: String, desc: VerticleDescriptor): JsonObject =
    new JsonObject()
      .put("verticleId", verticleId)
      .put("main", desc.main)
      .put("prefix", desc.prefix.map(_.toString).getOrElse(null))
      .put("configPath", desc.configPath.getOrElse(null))
      .put("options", desc.options)
      .put("deploymentStrategy", desc.deploymentStrategy.toString)

  private def handleUndeploymentResult(deployment: Future[List[Throwable \/ VerticleId]]): Unit =
    deployment.onComplete {
      case Success(results) =>
        val oks: List[VerticleId] = results.collect { case \/-(ok) => ok }
        val kos: List[Throwable]  = results.collect { case -\/(ko) => ko }

        if (!kos.isEmpty) {
          // TODO notify health-check
          kos.foreach(ex => {
            initLog.error("Could not undeploy verticle", ex)
          })
        }
        oks.foreach { id =>
          val message = s"Successfully undeployed verticle '${id.value}'"
          initLog.info(message)
          registry -= id
        }
      case Failure(ex) => {
        initLog.error("Unexpected error on verticles undeployment", ex)
      }
    }

  override protected def vertxServiceAddressPrefixS(): Option[String] = Some(registryType.value)
  override protected def vertxService(): Class[_] = classOf[RegistryService]

  override def getVerticleIds: VxFuture[util.List[String]] = {
    val verticles = registry.keys.map(_.value)
    VxFuture.succeededFuture(verticles.toList.asJava)
  }

  override def verticleId: String = s"registry:${registryType.value}"
}

