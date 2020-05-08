package com.cloudentity.tools.vertx.server.api;

import com.cloudentity.tools.vertx.bus.*;
import com.cloudentity.tools.vertx.registry.RegistryVerticle;
import com.cloudentity.tools.vertx.server.VertxBootstrap;
import com.cloudentity.tools.vertx.shutdown.ShutdownService;
import com.cloudentity.tools.vertx.shutdown.ShutdownVerticle;
import com.cloudentity.tools.vertx.test.VertxUnitTest;
import com.cloudentity.tools.vertx.verticles.VertxDeploy;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ShutdownTest extends VertxUnitTest {
  interface FlagService {
    @VertxEndpoint
    Future setFlag(boolean flag);
    @VertxEndpoint
    Future<Boolean> getFlag();
  }

  public static class FlagVerticle extends ServiceVerticle implements FlagService {
    boolean flag = false;

    @Override
    public Future setFlag(boolean flag) {
      this.flag = flag;
      return Future.succeededFuture();
    }

    @Override
    public Future<Boolean> getFlag() {
      return Future.succeededFuture(flag);
    }
  }

  public static class TestApp extends VertxBootstrap {

    public TestApp() {
      System.setProperty(ShutdownVerticle.DISABLE_EXIT_SYSTEM_PROPERTY_NAME, "true");
    }

    @Override
    protected Future beforeServerStart() {
      return RegistryVerticle.deploy(vertx, "components");
    }
  }

  public static class FailingVerticle extends ComponentVerticle {
    @Override
    public Future initComponentAsync() {
      ShutdownService shutdownService = createClient(ShutdownService.class);
      return shutdownService.registerShutdownAction(() -> createClient(FlagService.class).setFlag(true))
        .compose(x -> Future.failedFuture("FAILURE"));
    }
  }

  public static class SuccessfulVerticle extends ComponentVerticle {
    @Override
    public Future initComponentAsync() {
      ShutdownService shutdownService = createClient(ShutdownService.class);
      return shutdownService.registerShutdownAction(() -> createClient(FlagService.class).setFlag(true));
    }
  }

  String flagDeploymentId;

  @Before
  public void deployFlagVerticle(TestContext ctx) {
    VertxDeploy.deploy(vertx(), new FlagVerticle()).map(x -> flagDeploymentId = x).setHandler(ctx.asyncAssertSuccess());
  }

  @After
  public void unDeployFlagVerticle(TestContext ctx) {
    unDeploy(flagDeploymentId).setHandler(ctx.asyncAssertSuccess());
  }

  @Test
  public void shouldPerformShutdownActionWhenUndeployingBootstrap(TestContext ctx) {
    deployBootstrap(SuccessfulVerticle.class)
      .compose(this::unDeploy)
      .compose(x -> VertxEndpointClient.make(vertx(), FlagService.class).getFlag())
      .compose(flag -> {
        ctx.assertEquals(true, flag);
        return Future.succeededFuture();
      }).setHandler(ctx.asyncAssertSuccess());
  }

  @Test
  public void shouldNotPerformShutdownActionWhenNotUndeployingBootstrap(TestContext ctx) {
    deployBootstrap(SuccessfulVerticle.class)
      .compose(x -> VertxEndpointClient.make(vertx(), FlagService.class).getFlag())
      .compose(flag -> {
        ctx.assertEquals(false, flag);
        return Future.succeededFuture();
      }).setHandler(ctx.asyncAssertSuccess());
  }

  @Test
  public void shouldPerformShutdownActionWhenBootstrapFailedOnStartup(TestContext ctx) {
    deployBootstrap(FailingVerticle.class)
      .recover(ex -> Future.succeededFuture())
      .compose(x -> VertxEndpointClient.make(vertx(), FlagService.class).getFlag())
      .compose(flag -> {
        ctx.assertEquals(true, flag);
        return Future.succeededFuture();
      }).setHandler(ctx.asyncAssertSuccess());
  }

  private Future<String> deployBootstrap(Class verticleClass) {
    JsonObject config =
      new JsonObject()
        .put("apiServer", new JsonObject()
          .put("routes", new JsonArray())
          .put("http", new JsonObject().put("port", 8081))
        )
        .put("registry:routes", new JsonObject())
        .put("registry:components", new JsonObject()
          .put("verticle", new JsonObject()
          .put("main", verticleClass.getName()))
        );

    JsonObject metaConfig =
      new JsonObject()
        .put("scanPeriod", 1000)
        .put("stores", new JsonArray()
          .add(new JsonObject()
            .put("type", "json")
            .put("format", "json")
            .put("config", config))
        );

    return VertxDeploy.deploy(vertx(), new TestApp(), new DeploymentOptions().setConfig(metaConfig));
  }

  private Future unDeploy(String deploymentId) {
    Future promise = Future.future();
    vertx().undeploy(deploymentId, promise);
    return promise;
  }
}
