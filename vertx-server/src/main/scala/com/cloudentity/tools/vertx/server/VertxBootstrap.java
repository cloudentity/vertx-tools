package com.cloudentity.tools.vertx.server;

import com.cloudentity.tools.vertx.bus.VertxBus;
import com.cloudentity.tools.vertx.conf.ConfPrinter;
import com.cloudentity.tools.vertx.conf.ConfVerticleDeploy;
import com.cloudentity.tools.vertx.conf.modules.ModulesReader;
import com.cloudentity.tools.vertx.json.VertxJson;
import com.cloudentity.tools.vertx.launchers.OrchisCommandLauncher;
import com.cloudentity.tools.vertx.logging.InitLog;
import com.cloudentity.tools.vertx.registry.RegistryVerticle;
import com.cloudentity.tools.vertx.server.api.ApiServerDeployer;
import com.cloudentity.tools.vertx.shutdown.ShutdownVerticle;
import com.cloudentity.tools.vertx.tracing.TracingVerticle;
import com.cloudentity.tools.vertx.verticles.VertxDeploy;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class VertxBootstrap extends AbstractVerticle {
  public static void main(String[] args) {
    OrchisCommandLauncher.main(args);
  }

  String shutdownVerticleDeploymentId;

  private Logger log = LoggerFactory.getLogger(this.getClass());
  private InitLog initLog = InitLog.of(log);

  @Override
  public void start(Future startFuture) throws Exception {
    VertxBus.registerPayloadCodec(vertx.eventBus());
    VertxJson.registerJsonObjectDeserializer();
    VertxJson.configureJsonMapper();

    String runMode = getRunMode();
    switch (runMode) {
      case "standard":
        initLog.info("Starting application");
        ConfVerticleDeploy.deployVerticleFromMetaConfig(vertx, config())
          .compose(x -> VertxDeploy.deploy(vertx, new TracingVerticle()))
          .compose(x -> VertxDeploy.deploy(vertx, new ShutdownVerticle(false)))
          .compose(deploymentId -> {
            this.shutdownVerticleDeploymentId = deploymentId;
            return Future.succeededFuture();
          })
          .compose(x -> RegistryVerticle.deploy(vertx, "system-init", false))
          .compose(x -> beforeServerStart())
          .compose(x -> deployServer())
          .compose(x -> RegistryVerticle.deploy(vertx, "system-ready", false))
          .compose(x -> afterServerStart())
          .setHandler(handler(startFuture));

        break;
      case "dry":
        initLog.info("Running in dry mode");
        initLog.info("Unresolved meta config: " + config().toString());
        ConfPrinter.logMetaConfigEnvVariables(config(), log);
        logAvailableModules();

        ConfVerticleDeploy.deployVerticleFromMetaConfig(vertx, config())
          .setHandler(async -> {
            vertx.close();
            if (async.succeeded()) Future.succeededFuture();
            else Future.failedFuture(async.cause());
          });

        break;
      default:
        String errMsg = "Unsupported run mode: '" + runMode + '"';
        initLog.error(errMsg);
        startFuture.fail(errMsg);
    }
  }

  protected Future<String> deployServer() {
    return ApiServerDeployer.deployServer(vertx);
  }

  private String getRunMode() {
    String mode = config().getString("mode");
    return mode == null ? "standard" : mode;
  }

  private void logAvailableModules() {
    List<String> availableModules = ModulesReader.readAvailableModuleNames();
    availableModules.sort(String::compareTo);
    initLog.info("Available modules: [" + String.join(", ", availableModules) + "]");
  }

  private Handler<AsyncResult<Object>> handler(Future startFuture) {
    return result -> {
      if (result.succeeded()) {
        initLog.info("Application started successfully");
        startFuture.complete();
      } else {
        initLog.error("Application failed to start properly", result.cause());
        if (shutdownVerticleDeploymentId != null) {
          vertx.undeploy(shutdownVerticleDeploymentId, x -> startFuture.fail("Exiting."));
        } else {
          startFuture.fail("Exiting.");
        }
      }
    };
  }


  protected Future beforeServerStart() {
    return Future.succeededFuture();
  }

  protected Future afterServerStart() {
    return Future.succeededFuture();
  }
}
