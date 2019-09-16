package com.cloudentity.tools.vertx.shutdown;

import com.cloudentity.tools.vertx.bus.ServiceVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * This verticle can be used to store cleanup operations that will be executed on ShutdownVerticle un-deployment.
 * It is deployed by VertxBootstrap on its start and un-deployed either on VertxBootstrap un-deployment or start failure.
 */
public class ShutdownVerticle extends ServiceVerticle implements ShutdownService {
  private static final Logger log = LoggerFactory.getLogger(ShutdownVerticle.class);

  List<Supplier<Future>> shutdownActions = new ArrayList<>();
  private boolean isShutdown = false;

  private static final String CONFIG_PATH = "shutdown-service";
  private static final String DISABLE_EXIT_PROPERTY_NAME =  "disableExit";
  private static final String EXIT_DELAY_PROPERTY_NAME =  "exitDelay";
  public static final String DISABLE_EXIT_SYSTEM_PROPERTY_NAME = CONFIG_PATH + "." + DISABLE_EXIT_PROPERTY_NAME;

  private boolean disableExitFromSystem = false;
  private boolean disableExit = false;
  private int exitDelay = 10;

  private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, runnable -> {
    Thread thread = Executors.defaultThreadFactory().newThread(runnable);
    thread.setDaemon(true);
    return thread;
  });


  /**
   * @param disableExitFromSystem if set to true JVM exit task would not be started
   *                              use it for tests
   */
  public ShutdownVerticle(boolean disableExitFromSystem) {
    this.disableExitFromSystem = disableExitFromSystem;
  }

  /**
   * by default setting `disableExitFromSystem` to true to disable JVM exit task
   */
  public ShutdownVerticle() {
    this(true);
  }

  @Override
  protected void initService() {
    this.disableExit = this.disableExitFromSystem
        || getDisabledConfigFromSystemProperties()
        || getDisabledConfigFromVerticleConfig();
    if (!disableExit) {
      this.exitDelay = getExitDelayConfigFromVerticleConfig();
    }
  }

  @Override
  public Future<Void> registerShutdownAction(Supplier<Future> action) {
    if (isShutdown) {
      log.warn("System is in shutdown state. Executing action immediately");
      return action.get()
          .compose(any -> Future.failedFuture(new SystemInShutdownStateException("System is in shutdown state. Action was executed. Failing as system is shutting down.")));
    }
    else {
      shutdownActions.add(action);
      log.info("New shutdown action has been registered. Keeping {} actions now", shutdownActions.size());
      return Future.succeededFuture();
    }
  }

  @Override
  public void stop(Future s) {
    isShutdown = true;
    log.info("Stopping ShutdownVerticle. Executing {} registered actions", shutdownActions.size());
    CompositeFuture.all(
      shutdownActions.stream().map(a -> a.get()).collect(Collectors.toList())
    ).map(x -> shutdownJVM()).setHandler(s);
  }

  private Void shutdownJVM() {
    if (!disableExit) {
      log.info("Creating background thread which will kill JVM if application would not shutdown properly in {} sec", exitDelay);
      scheduler.schedule(() -> {
        log.warn("Shutting down JVM with -1 signal");
        System.exit(-1);
      }, exitDelay, TimeUnit.SECONDS);
    } else {
      log.info("Shutdown jvm is disabled. Skipping");
    }
    scheduler.shutdown();
    return null;
  }

  private int getExitDelayConfigFromVerticleConfig() {
    return Optional.ofNullable(getConfig()).map(c -> c.getInteger(EXIT_DELAY_PROPERTY_NAME, 10)).orElse(10);
  }

  private boolean getDisabledConfigFromSystemProperties() {
    return Boolean.parseBoolean(System.getProperty(DISABLE_EXIT_SYSTEM_PROPERTY_NAME));
  }

  private boolean getDisabledConfigFromVerticleConfig() {
    return Optional.ofNullable(getConfig()).map(c -> c.getBoolean(DISABLE_EXIT_PROPERTY_NAME, false)).orElse(false);
  }

  public static class SystemInShutdownStateException extends RuntimeException {
    SystemInShutdownStateException(String message) {
      super(message);
    }
  }

  @Override
  public String configPath() {
    return CONFIG_PATH;
  }

}
