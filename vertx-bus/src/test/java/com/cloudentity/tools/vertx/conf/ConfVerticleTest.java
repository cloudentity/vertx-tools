package com.cloudentity.tools.vertx.conf;

import com.cloudentity.tools.vertx.bus.ServiceClientFactory;
import com.cloudentity.tools.vertx.bus.ServiceVerticle;
import com.cloudentity.tools.vertx.bus.VertxBus;
import com.cloudentity.tools.vertx.bus.VertxEndpoint;
import com.cloudentity.tools.vertx.verticles.VertxDeploy;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@RunWith(VertxUnitRunner.class)
public class ConfVerticleTest {
  @Rule
  public RunTestOnContext rule = new RunTestOnContext();

  Vertx vertx = Vertx.vertx();

  public interface TestService {
    @VertxEndpoint(address = "load-config")
    Future<JsonObject> loadConfig();
  }

  public static class TestVerticle extends ServiceVerticle implements TestService {
    private String id;

    public TestVerticle(String id) {
      this.id = id;
    }

    @Override
    public Future<JsonObject> loadConfig() {
      return Future.succeededFuture(getConfig());
    }

    @Override
    public String verticleId() {
      return id;
    }
  }

  @Test
  public void serviceVerticleShouldGetItsConfigOnStartByVerticleId(TestContext ctx) throws IOException {

    // given
    String verticleId = "verticleA";
    String configPath = "src/test/resources/conf/conf.json";
    JsonObject config = new JsonObject(new String(Files.readAllBytes(Paths.get(configPath))));

    VertxBus.registerPayloadCodec(vertx.eventBus());

    // when
    ConfVerticleDeploy.deployFileConfVerticle(vertx, configPath)
      .compose(x -> VertxDeploy.deploy(vertx, new TestVerticle(verticleId)))
      .compose(x -> {
        TestService client = ServiceClientFactory.make(vertx.eventBus(), TestService.class);
        return client.loadConfig();
      })
      .map(conf -> {
        ctx.assertTrue(conf.equals(config.getJsonObject(verticleId)));
        return null;
      })
      .setHandler(ctx.asyncAssertSuccess());
  }

  @Test
  public void serviceVerticleShouldGetItsConfigOnStartByConfigPath(TestContext ctx) throws IOException {
    // given
    String verticleId = "verticleB";
    String verticleConfigPath = "verticleB.conf";
    String configPath = "src/test/resources/conf/conf.json";
    JsonObject config = new JsonObject(new String(Files.readAllBytes(Paths.get(configPath))));

    VertxBus.registerPayloadCodec(vertx.eventBus());

    // when
    ConfVerticleDeploy.deployFileConfVerticle(vertx, configPath)
      .compose(x -> {
        return VertxDeploy.deploy(vertx, new TestVerticle(verticleId), new DeploymentOptions().setConfig(new JsonObject().put("configPath", verticleConfigPath)));
      })
      .compose(x -> {
        TestService client = ServiceClientFactory.make(vertx.eventBus(), TestService.class);
        return client.loadConfig();
      })
      .map(conf -> {
        String[] path = verticleConfigPath.split("\\.");
        ctx.assertTrue(conf.equals(config.getJsonObject(path[0]).getJsonObject(path[1])));
        return null;
      })
      .setHandler(ctx.asyncAssertSuccess());
  }

  @Test
  public void serviceVerticleShouldChangeConfig(TestContext ctx) throws IOException {
    // given
    String verticleId = "verticleA";

    String srcConfigPath = "src/test/resources/conf/conf.json";
    String configPath = "target/serviceVerticleShouldChangeConfig.json";
    Files.copy(Paths.get(srcConfigPath), Paths.get(configPath), StandardCopyOption.REPLACE_EXISTING);

    JsonObject config = new JsonObject(new String(Files.readAllBytes(Paths.get(configPath))));

    VertxBus.registerPayloadCodec(vertx.eventBus());

    // when
    ConfVerticleDeploy.deployFileConfVerticle(vertx, configPath, new ConfigRetrieverOptions().setScanPeriod(100))
      .compose(x -> VertxDeploy.deploy(vertx, new TestVerticle(verticleId)))
      .compose(x -> {
        vertx.fileSystem().deleteBlocking(configPath);
        config.getJsonObject(verticleId).put("newKey", "newValue");
        Buffer buffer = Buffer.buffer(config.toString());
        vertx.fileSystem().writeFileBlocking(configPath, buffer);
        return Future.succeededFuture();
      })
      .compose(x -> {
        Future<Long> delay = Future.future();
        vertx.setTimer(500, t -> delay.complete(t));
        return delay;
      })
      .compose(x -> {
        TestService client = ServiceClientFactory.make(vertx.eventBus(), TestService.class);
        return client.loadConfig();
      })
      .map(conf -> {
        ctx.assertTrue(conf.equals(config.getJsonObject(verticleId)));
        return null;
      })
      .setHandler(ctx.asyncAssertSuccess());
  }

  @Test
  public void serviceVerticleShouldGetItsConfigWhenItIsReference(TestContext ctx) throws IOException {
    // given
    String verticleId = "verticleC";
    String referencedVerticleId = "verticleA";
    String configPath = "src/test/resources/conf/conf.json";
    JsonObject config = new JsonObject(new String(Files.readAllBytes(Paths.get(configPath))));

    VertxBus.registerPayloadCodec(vertx.eventBus());

    // when
    ConfVerticleDeploy.deployFileConfVerticle(vertx, configPath)
      .compose(x -> VertxDeploy.deploy(vertx, new TestVerticle(verticleId)))
      .compose(x -> {
        TestService client = ServiceClientFactory.make(vertx.eventBus(), TestService.class);
        return client.loadConfig();
      })
      .map(conf -> {
        ctx.assertTrue(conf.equals(config.getJsonObject(referencedVerticleId)));
        return null;
      })
      .setHandler(ctx.asyncAssertSuccess());
  }

  @Test
  public void modulesShouldBeLoadedFromClasspathAndBeOverriddenBYRootConfigAndEachOtherInCorrectOrder(TestContext ctx) throws IOException {
    // given
    String configPath = "src/test/resources/conf/conf-modules.json";
    VertxBus.registerPayloadCodec(vertx.eventBus());

    // when
    ConfVerticleDeploy.deployFileConfVerticle(vertx, configPath)
    .map(x -> ServiceClientFactory.make(vertx.eventBus(), ConfService.class))
    .compose((ConfService conf) -> {
      return conf.getGlobalConf().map(global -> {
        ctx.assertTrue(global.getBoolean("x"));
        ctx.assertTrue(global.getBoolean("should-be-overridden-by-root"));
        ctx.assertTrue(global.getBoolean("should-be-overridden-by-module-b"));
        return global;
      });
    })
    .setHandler(ctx.asyncAssertSuccess());
  }

  @Test
  public void modulesShouldBeLoadedFromClasspathAndEnvVariablesPopulated(TestContext ctx) throws IOException {
    // given
    String configPath = "src/test/resources/conf/conf-modules.json";
    VertxBus.registerPayloadCodec(vertx.eventBus());

    // when
    ConfVerticleDeploy.deployFileConfVerticle(vertx, configPath)
    .map(x -> ServiceClientFactory.make(vertx.eventBus(), ConfService.class))
    .compose((ConfService conf) -> {
      return conf.getGlobalConf().map(global -> {
        ctx.assertTrue(global.getBoolean("env-a"));
        return global;
      });
    })
    .setHandler(ctx.asyncAssertSuccess());
  }
}
