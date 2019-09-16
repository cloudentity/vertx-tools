package com.cloudentity.tools.vertx.verticleScanner;

import com.google.common.collect.Lists;
import io.vertx.core.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * Verticle handling local deployment of verticles based on a directory with configuration files.
 */
public class DirectoryScanningVerticleDeployer extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(DirectoryScanningVerticleDeployer.class);

  private Map<String, DeployedService> deployedServices = new HashMap<>();

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    redeployVerticlesFromConfig(firstRunEvent -> {
      if (firstRunEvent.succeeded()) {
        scheduledRefresh();
        startFuture.complete();
      } else {
        startFuture.fail(firstRunEvent.cause());
      }
    });
  }

  private void scheduledRefresh() {
    log.debug("Refreshing verticles configuration");
    redeployVerticlesFromConfig(e -> vertx.setTimer(config().getInteger("scanInterval"), handler -> scheduledRefresh()));
  }


  @Override
  public void stop(Future<Void> stopFuture) throws Exception {
    super.stop(stopFuture);
  }

  private void redeployVerticlesFromConfig(Handler<AsyncResult<String>> handler) {
    getVerticleServiceFiles(filesHandler -> {
      if (filesHandler.succeeded()) {
        List<String> files = filesHandler.result();
        log.debug("Found following verticle config files {}",
          files.stream().map(FilenameUtils::getName).collect(toList())
        );
        List<Future> deploymentFutures = refreshVerticlesFromConfigFiles(files);
        CompositeFuture.join(deploymentFutures).setHandler(event -> {
          if (event.succeeded()) {
            handler.handle(Future.succeededFuture());
          } else {
            handler.handle(Future.failedFuture(event.cause()));
          }
        });
      } else {
        handler.handle(Future.failedFuture("Could not read config directory"));
      }
    });
  }

  private List<Future> refreshVerticlesFromConfigFiles(List<String> files) {

    List<String> servicesNamesFromFiles = files.stream()
      .map(this::getServiceNameFromFile)
      .collect(toList());

    List<String> toBeUndeployedServices = Lists.newArrayList(deployedServices.keySet());
    toBeUndeployedServices.removeAll(servicesNamesFromFiles);

    List<Future> undeployingVerticlesFutures = toBeUndeployedServices.stream()
      .map(serviceId -> {
        Future<Void> future = Future.future();
        String deploymentId = deployedServices.get(serviceId).getDeploymentId();
        vertx.undeploy(deploymentId, undeployEvent -> {
          log.info("Undeploy event for {} with deployment id {} : status: {}",
            serviceId, deploymentId, undeployEvent.succeeded());
          if (undeployEvent.succeeded()) {
            deployedServices.remove(serviceId);
          }
          future.complete();
        });
        return future;
      }).collect(toList());

    List<Future> deployingVerticlesFutures = files.stream()
      .map(file -> {
        Future<String> serviceDeploymentFuture = Future.future();
        redeployVerticleFromConfigFile(file, serviceDeploymentFuture);
        return serviceDeploymentFuture;
      })
      .collect(toList());

    undeployingVerticlesFutures.addAll(deployingVerticlesFutures);

    return undeployingVerticlesFutures;
  }

  private void redeployVerticleFromConfigFile(String file, Handler<AsyncResult<String>> handler) {
    getFileHash(file, fileHashEvent -> {
      if (fileHashEvent.succeeded()) {
        deployVerticleWithConfigHash(file, fileHashEvent.result(), handler);
      } else {
        handler.handle(Future.failedFuture("Could not calculate config file hash for " + file));
      }
    });
  }

  private void deployVerticleWithConfigHash(String file, String fileHash, Handler<AsyncResult<String>> handler) {
    String serviceName = getServiceNameFromFile(file);
    if (deployedServices.containsKey(serviceName)) {
      if (!deployedServices.get(serviceName).getConfigHash().equals(fileHash)) {
        redeployVerticle(serviceName, fileHash, handler);
      } else {
        log.debug("Skipping verticle {}. Not modified", serviceName);
        handler.handle(Future.succeededFuture());
      }
    } else {
      deployVerticle(serviceName, fileHash, handler);
    }
  }

  private void redeployVerticle(String serviceName, String fileHash, Handler<AsyncResult<String>> handler) {
    log.info("Redeploying verticle {}", serviceName);
    vertx.undeploy(
      deployedServices.get(serviceName).getDeploymentId(),
      undeployHandler -> {
        if (undeployHandler.succeeded()) {
          deployVerticle(serviceName, fileHash, handler);
          deployedServices.remove(serviceName);
        } else {
          handler.handle(Future.failedFuture("Could not undeploy verticle" + serviceName));
        }
      }
    );
  }

  private void deployVerticle(String serviceName, String fileHash, Handler<AsyncResult<String>> handler) {
    log.info("Deploying verticle {}", serviceName);
    vertx.deployVerticle(
      serviceName,
      getDeploymentOptions(serviceName),
      deploymentEvent -> {
        if (deploymentEvent.succeeded()) {
          log.info("Deployment of {} verticle succeeded", serviceName);
          handler.handle(Future.succeededFuture(deploymentEvent.result()));
          saveDeployedService(serviceName, DeployedService.of(deploymentEvent.result(), fileHash));
        } else {
          log.error("Deployment of {} verticle failed, cause: {}", serviceName, deploymentEvent.cause());
          handler.handle(Future.failedFuture("Could not deploy verticle " + serviceName + ": " + deploymentEvent.cause()));
        }
      });
  }

  private DeploymentOptions getDeploymentOptions(String serviceName) {
    return new DeploymentOptions()
      .setIsolationGroup(serviceName)
      .setExtraClasspath(Collections.singletonList(getVerticlesConfigurationDirectory()));
  }

  private String getServiceNameFromFile(String file) {
    return "service:" + FilenameUtils.getBaseName(file);
  }

  private void saveDeployedService(String serviceName, DeployedService service) {
    deployedServices.put(serviceName, service);
  }

  private void getFileHash(String file, Handler<AsyncResult<String>> handler) {
    vertx.fileSystem().readFile(file, fileReadEvent -> {
      if (fileReadEvent.succeeded()) {
        String hash = DigestUtils.md5Hex(fileReadEvent.result().getBytes());
        handler.handle(Future.succeededFuture(hash));
      }
    });
  }

  private void getVerticleServiceFiles(Handler<AsyncResult<List<String>>> handler) {
    String configFilesDir = getVerticlesConfigurationDirectory();
    log.debug("Reading verticles configuration from {}", configFilesDir);
    vertx.fileSystem().readDir(configFilesDir, ".*\\.json", handler);
  }

  private String getVerticlesConfigurationDirectory() {
    return config().getString("directory");
  }

}