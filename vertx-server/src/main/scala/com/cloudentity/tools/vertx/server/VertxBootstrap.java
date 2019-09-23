package com.cloudentity.tools.vertx.server;

import com.cloudentity.tools.vertx.bus.VertxBus;
import com.cloudentity.tools.vertx.conf.ConfBuilder;
import com.cloudentity.tools.vertx.conf.ConfPrinter;
import com.cloudentity.tools.vertx.conf.ConfVerticleDeploy;
import com.cloudentity.tools.vertx.conf.ModulesReader;
import com.cloudentity.tools.vertx.json.VertxJson;
import com.cloudentity.tools.vertx.launchers.OrchisCommandLauncher;
import com.cloudentity.tools.vertx.logging.InitLog;
import com.cloudentity.tools.vertx.server.api.ApiServerDeployer;
import com.cloudentity.tools.vertx.shutdown.ShutdownVerticle;
import com.cloudentity.tools.vertx.tracing.TracingVerticle;
import com.cloudentity.tools.vertx.verticles.VertxDeploy;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import java.util.List;

public class VertxBootstrap extends AbstractVerticle {
  public static void main(String[] args) {
    OrchisCommandLauncher.main(args);
  }

  String shutdownVerticleDeploymentId;

  @Override
  public void start(Future startFuture) throws Exception {
    VertxBus.registerPayloadCodec(vertx.eventBus());
    VertxJson.registerJsonObjectDeserializer();

    String runMode = getRunMode();
    switch (runMode) {
      case "standard":
        InitLog.info("Starting application");
        ConfVerticleDeploy.deployVerticleFromConf(vertx, config())
          .compose(x -> VertxDeploy.deploy(vertx, new TracingVerticle()))
          .compose(x -> VertxDeploy.deploy(vertx, new ShutdownVerticle(false)))
          .compose(deploymentId -> {
            this.shutdownVerticleDeploymentId = deploymentId;
            return Future.succeededFuture();
          })
          .compose(x -> beforeServerStart())
          .compose(x -> deployServer())
          .compose(x -> afterServerStart())
          .setHandler(handler(startFuture));

        break;
      case "dry":
        InitLog.info("Running in dry mode");
        InitLog.info("Unresolved meta config: " + config().toString());
        ConfPrinter.logMetaConfigEnvVariables(config());
        logAvailableModules();

        ConfVerticleDeploy.deployVerticleFromConf(vertx, config())
          .setHandler(async -> {
            vertx.close();
            if (async.succeeded()) Future.succeededFuture();
            else Future.failedFuture(async.cause());
          });

        break;
      default:
        String errMsg = "Unsupported run mode: '" + runMode + '"';
        InitLog.error(errMsg);
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
    InitLog.info("Available modules: [" + String.join(", ", availableModules) + "]");
  }

  private Handler<AsyncResult<Object>> handler(Future startFuture) {
    return result -> {
      if (result.succeeded()) {
        InitLog.info("Application started successfully");
        startFuture.complete();
      } else {
        InitLog.error("Application failed to start properly", result.cause());
        if (shutdownVerticleDeploymentId != null) {
          vertx.undeploy(shutdownVerticleDeploymentId, x -> startFuture.fail(result.cause()));
        } else {
          startFuture.fail(result.cause());
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
