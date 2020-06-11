package com.cloudentity.tools.vertx.bus;

import com.google.common.base.Strings;
import com.cloudentity.tools.vertx.conf.ConfVerticle;
import com.cloudentity.tools.vertx.verticles.VertxDeploy;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Optional;

@RunWith(VertxUnitRunner.class)
public class ServiceVerticleTest {
  @Rule
  public RunTestOnContext rule = new RunTestOnContext();

  Vertx vertx = Vertx.vertx();

  interface MyService {
    @VertxEndpoint(address = "service-verticle-dostuff")
    Future<String> doStuff(String x, int y);
  }

  @Test
  public void shouldRespondToRequestWithoutAddressPrefix(TestContext ctx) {
    testShouldRespondToRequest(ctx, Optional.empty());
  }

  @Test
  public void shouldRespondToRequestWithAddressPrefix(TestContext ctx) {
    testShouldRespondToRequest(ctx, Optional.of("prefix:"));
  }

  static class MyVerticle extends ServiceVerticle implements MyService {
    private Optional<String> addressPrefixOpt;
    public MyVerticle(Optional<String> addressPrefixOpt) {
      this.addressPrefixOpt = addressPrefixOpt;
    }

    public MyVerticle() {
      this.addressPrefixOpt = Optional.empty();
    }

    public Future<String> doStuff(String x, int y) {
      return Future.succeededFuture(Strings.repeat(x, y));
    }

    @Override
    protected Optional<String> vertxServiceAddressPrefix() {
      return addressPrefixOpt;
    }
  }

  private void testShouldRespondToRequest(TestContext ctx, final Optional<String> addressPrefixOpt) {
    Async async = ctx.async();

    VertxBus.registerPayloadCodec(vertx.eventBus());

    // given
    MyService client = VertxEndpointClient.make(vertx, MyService.class, addressPrefixOpt);

    vertx.deployVerticle(new MyVerticle(addressPrefixOpt), deployResult -> {
      // when
      Future<String> future = client.doStuff("x", 3);

      // then
      future.setHandler(result -> {
        ctx.assertTrue(result.succeeded());
        ctx.assertEquals("xxx", result.result());
        async.complete();
      });
    });
  }

  @Test
  public void shouldFailDeploymentWhenNoDeclaredVertxServices(TestContext ctx) {
    Async async = ctx.async();

    // given
    class MyVerticle extends ServiceVerticle {
    }

    // when
    vertx.deployVerticle(new MyVerticle(), deployResult -> {
      // then
      ctx.assertTrue(deployResult.failed());
      async.complete();
    });
  }

  @Test
  public void shouldFailUndeploymentWhenSyncCleanupThrowsException(TestContext ctx) {
    Async async = ctx.async();

    VertxBus.registerPayloadCodec(vertx.eventBus());

    // given
    class MyVerticle extends ServiceVerticle implements MyService {
      @Override
      protected void cleanup() {
        throw new RuntimeException("cleanup");
      }

      @Override
      public Future<String> doStuff(String x, int y) {
        return null;
      }
    }

    // when
    vertx.deployVerticle(new MyVerticle(), deployResult -> {
      vertx.undeploy(deployResult.result(), undeployResult -> {
        // then
        ctx.assertTrue(undeployResult.failed());
        ctx.assertEquals("cleanup", undeployResult.cause().getMessage());
        async.complete();
      });
    });
  }

  @Test
  public void shouldFailUndeploymentWhenAsyncCleanupReturnsFailedFuture(TestContext ctx) {
    Async async = ctx.async();

    VertxBus.registerPayloadCodec(vertx.eventBus());

    // given
    class MyVerticle extends ServiceVerticle implements MyService {
      @Override
      protected Future cleanupAsync() {
        return Future.failedFuture("cleanup");
      }

      @Override
      public Future<String> doStuff(String x, int y) {
        return null;
      }
    }

    // when
    vertx.deployVerticle(new MyVerticle(), deployResult -> {
      vertx.undeploy(deployResult.result(), undeployResult -> {
        // then
        ctx.assertTrue(undeployResult.failed());
        ctx.assertEquals("cleanup", undeployResult.cause().getMessage());
        async.complete();
      });
    });
  }

  interface MyPublishService {
    @VertxEndpoint
    void accept(Async async);
  }

  class MyPublishVerticle extends ServiceVerticle implements MyPublishService {
    public void accept(Async async) {
      System.out.println("Publish message received");
      async.complete();
    }
  }

  @Test
  public void shouldAcceptPublishMessage(TestContext ctx) {
    Async async = ctx.async();

    VertxBus.registerPayloadCodec(vertx.eventBus());

    // given
    MyPublishService client = VertxEndpointClient.make(vertx, MyPublishService.class);

    // when
    vertx.deployVerticle(new MyPublishVerticle(), deployResult -> {
      // then
      ctx.assertTrue(deployResult.succeeded());
      client.accept(async);
    });
  }

  @Test
  public void shouldFailDeploymentWhenAfterServiceStartAsyncReturnsFailedFuture(TestContext ctx) {
    Async async = ctx.async();

    // given
    MyVerticle verticle = new MyVerticle() {
      @Override
      protected Future<Void> initServiceAsync() {
        return Future.failedFuture("error");
      }
    };

    // when
    vertx.deployVerticle(verticle, deployResult -> {
      // then
      ctx.assertTrue(deployResult.failed());
      async.complete();
    });
  }

  @Test
  public void shouldFailDeploymentWhenAfterServiceStartAsyncThrowsException(TestContext ctx) {
    Async async = ctx.async();

    // given
    MyVerticle verticle = new MyVerticle() {
      @Override
      protected Future<Void> initServiceAsync() {
        throw new RuntimeException("error");
      }
    };

    // when
    vertx.deployVerticle(verticle, deployResult -> {
      // then
      ctx.assertTrue(deployResult.failed());
      async.complete();
    });
  }

  @Test
  public void shouldFailDeploymentWhenAfterComponentStartAsyncReturnsFailedFuture(TestContext ctx) {
    Async async = ctx.async();

    // given
    MyVerticle verticle = new MyVerticle() {
      @Override
      protected Future<Void> initComponentAsync() {
        return Future.failedFuture("error");
      }
    };

    // when
    vertx.deployVerticle(verticle, deployResult -> {
      // then
      ctx.assertTrue(deployResult.failed());
      async.complete();
    });
  }

  @Test
  public void shouldFailDeploymentWhenAfterComponentStartAsyncThrowsException(TestContext ctx) {
    Async async = ctx.async();

    // given
    MyVerticle verticle = new MyVerticle() {
      @Override
      protected Future<Void> initComponentAsync() {
        throw new RuntimeException("error");
      }
    };

    // when
    vertx.deployVerticle(verticle, deployResult -> {
      // then
      ctx.assertTrue(deployResult.failed());
      async.complete();
    });
  }

  interface NonFutureService {
    @VertxEndpoint(address = "service-verticle-dostuff")
    String doStuff(String x, int y);
  }

  @Test
  public void shouldFailDeploymentWhenVertxEndpointDoesNotReturnFuture(TestContext ctx) {
    Async async = ctx.async();

    // given
    class MyVerticle extends ServiceVerticle implements NonFutureService {
      @Override
      public String doStuff(String x, int y) {
        return Strings.repeat(x, y);
      }
    }

    // when
    vertx.deployVerticle(new MyVerticle(), deployResult -> {
      // then
      ctx.assertTrue(deployResult.failed());
      async.complete();
    });
  }

  interface MyAnotherService {
    @VertxEndpoint(address = "service-verticle-do-another-stuff")
    Future<String> doAnotherStuff(String x);
  }

  static class MyAnotherVerticle extends ServiceVerticle implements MyService, MyAnotherService {

    @Override
    public Future<String> doStuff(String x, int y) {
      return Future.succeededFuture(Strings.repeat(x, y));
    }

    @Override
    public Future<String> doAnotherStuff(String x) {
      return Future.succeededFuture(x);
    }
  }

  @Test
  public void shouldRespondOnAllVertxServices(TestContext ctx) {
    Async async = ctx.async();

    VertxBus.registerPayloadCodec(vertx.eventBus());

    // given
    MyService client = VertxEndpointClient.make(vertx, MyService.class);
    MyAnotherService anotherClient = VertxEndpointClient.make(vertx, MyAnotherService.class);

    vertx.deployVerticle(new MyAnotherVerticle(), deployResult -> {
      // when
      Future<String> future = client.doStuff("x", 3);

      // then
      future.compose(result -> {
        ctx.assertEquals("xxx", result);
        return Future.succeededFuture();
      }).compose(x -> anotherClient.doAnotherStuff("x"))
        .setHandler(result -> {
          ctx.assertTrue(result.succeeded());
          ctx.assertEquals("x", result.result());
          async.complete();
        });
    });
  }

  interface MyServiceWithoutAddress {
    @VertxEndpoint
    Future<String> doStuff(String x, int y);
  }

  static class MyVerticleWithoutAddress extends ServiceVerticle implements MyServiceWithoutAddress {
    private Optional<String> addressPrefixOpt;
    public MyVerticleWithoutAddress(Optional<String> addressPrefixOpt) {
      this.addressPrefixOpt = addressPrefixOpt;
    }

    public MyVerticleWithoutAddress() {
      this.addressPrefixOpt = Optional.empty();
    }

    public Future<String> doStuff(String x, int y) {
      return Future.succeededFuture(Strings.repeat(x, y));
    }

    @Override
    protected Optional<String> vertxServiceAddressPrefix() {
      return addressPrefixOpt;
    }
  }

  @Test
  public void shouldRespondToRequestWithoutAddressPrefixOnDerivedAddress(TestContext ctx) {
    testShouldRespondToRequestOnDerivedAddress(ctx, Optional.empty());
  }

  @Test
  public void shouldRespondToRequestWithAddressPrefixOnDerivedAddress(TestContext ctx) {
    testShouldRespondToRequestOnDerivedAddress(ctx, Optional.of("prefix:"));
  }

  private void testShouldRespondToRequestOnDerivedAddress(TestContext ctx, final Optional<String> addressPrefixOpt) {
    Async async = ctx.async();

    VertxBus.registerPayloadCodec(vertx.eventBus());

    // given
    MyServiceWithoutAddress client = VertxEndpointClient.make(vertx, MyServiceWithoutAddress.class, addressPrefixOpt);

    vertx.deployVerticle(new MyVerticleWithoutAddress(addressPrefixOpt), deployResult -> {
      // when
      Future<String> future = client.doStuff("x", 3);

      // then
      future.setHandler(result -> {
        ctx.assertTrue(result.succeeded());
        ctx.assertEquals("xxx", result.result());
        async.complete();
      });
    });
  }

  @Test
  public void shouldUseAddressPrefixFromDeploymentConfig(TestContext ctx) {
    VertxBus.registerPayloadCodec(vertx.eventBus());

    // given
    MyService client = VertxEndpointClient.make(vertx, MyService.class, Optional.of("address-prefix"));
    DeploymentOptions opts = new DeploymentOptions().setConfig(new JsonObject().put("prefix", "address-prefix"));

    vertx.deployVerticle(new MyVerticleWithoutAddressPrefixOverwrite(), opts, deployResult -> {
      // when
      Future<String> future = client.doStuff("x", 3);

      // then
      future.compose(result -> {
        ctx.assertEquals("xxx", result);
        return Future.succeededFuture();
      }).setHandler(ctx.asyncAssertSuccess());
    });
  }

  @Test
  public void shouldUseVerticleIdAsAddressPrefixIfPrefixTrue(TestContext ctx) {
    VertxBus.registerPayloadCodec(vertx.eventBus());

    // given
    MyService client = VertxEndpointClient.make(vertx, MyService.class, Optional.of("my-verticle"));
    DeploymentOptions opts = new DeploymentOptions().setConfig(new JsonObject().put("verticleId", "my-verticle").put("prefix", true));

    vertx.deployVerticle(new MyVerticleWithoutAddressPrefixOverwrite(), opts, deployResult -> {
      // when
      Future<String> future = client.doStuff("x", 3);

      // then
      future.compose(result -> {
        ctx.assertEquals("xxx", result);
        return Future.succeededFuture();
      }).setHandler(ctx.asyncAssertSuccess());
    });
  }

  static class MyVerticleWithoutAddressPrefixOverwrite extends ServiceVerticle implements MyService {
    public Future<String> doStuff(String x, int y) {
      return Future.succeededFuture(Strings.repeat(x, y));
    }
  }

  interface InheritedService {
    @VertxEndpoint
    Future<String> doStuff(String x);
  }

  static abstract class MyAbstractVerticle extends ServiceVerticle implements InheritedService {
  }

  static class MyConcreteWithImplementationVerticle extends MyAbstractVerticle {
    @Override
    public Future<String> doStuff(String x) {
      return null;
    }
  }

  static class MyConcreteWithoutImplementationVerticle extends MyConcreteWithImplementationVerticle {
  }

  @Test
  public void shouldSuccessfullyDeployMyConcreteWithImplementationVerticle(TestContext ctx) {
    VertxBus.registerPayloadCodec(vertx.eventBus());
    vertx.deployVerticle(new MyConcreteWithImplementationVerticle(), ctx.asyncAssertSuccess());
  }

  @Test
  public void shouldSuccessfullyDeployMyConcreteWithoutImplementationVerticle(TestContext ctx) {
    VertxBus.registerPayloadCodec(vertx.eventBus());
    vertx.deployVerticle(new MyConcreteWithoutImplementationVerticle(), ctx.asyncAssertSuccess());
  }

  static class MyVerticleThrowingException extends ServiceVerticle implements MyService {
    private boolean throwInsteadOfFail;

    public MyVerticleThrowingException(boolean throwInsteadOfFail) {
      this.throwInsteadOfFail = throwInsteadOfFail;
    }

    public Future<String> doStuff(String x, int y) {
      IllegalStateException ex = new IllegalStateException("my-error");
      if (throwInsteadOfFail) {
        throw ex;
      } else return Future.failedFuture(ex);
    }
  }

  @Test
  public void shouldPassThrownExceptionToServiceClient(TestContext ctx) {
    clientShouldReceiveServiceVerticleException(ctx, true);
  }

  @Test
  public void shouldPassFailedFutureToServiceClient(TestContext ctx) {
    clientShouldReceiveServiceVerticleException(ctx, false);
  }

  public void clientShouldReceiveServiceVerticleException(TestContext ctx, boolean  verticleThrowsExceptionInsteadOfFail) {
    VertxBus.registerPayloadCodec(vertx.eventBus());

    // given
    MyService client = VertxEndpointClient.make(vertx, MyService.class);

    vertx.deployVerticle(new MyVerticleThrowingException(verticleThrowsExceptionInsteadOfFail), deployResult -> {
      // when
      Future<String> future = client.doStuff("x", 3);

      // then
      future.recover(ex -> {
        ctx.assertEquals("my-error", ex.getMessage());
        ctx.assertEquals(IllegalStateException.class, ex.getClass());
        return Future.succeededFuture();
      }).setHandler(ctx.asyncAssertSuccess());
    });
  }

  interface ConfListener {
    @VertxEndpoint
    Future<JsonObject> changedConfig();
  }

  static class MyVerticleSelfConfChangeListener extends ServiceVerticle implements ConfListener {
    public static final String CONFIG_PATH = "my-verticle";

    JsonObject changed = new JsonObject();

    @Override
    public void initService() {
      registerSelfConfChangeListener(x -> changed = x);
    }

    @Override
    public String configPath() {
      return CONFIG_PATH;
    }

    @Override
    public Future<JsonObject> changedConfig() {
      return Future.succeededFuture(changed);
    }
  }

  @Test
  public void shouldReceiveNotificationOnSelfConfChangeWhenRegistered(TestContext ctx) {
    VertxBus.registerPayloadCodec(vertx.eventBus());

    JsonObject changedGlobalConf = new JsonObject().put(MyVerticleSelfConfChangeListener.CONFIG_PATH, new JsonObject().put("changed", true));
    String confStoreAddress = "test-conf-address";

    ConfigRetrieverOptions opts = new ConfigRetrieverOptions();
    opts.setScanPeriod(50).addStore(new ConfigStoreOptions().setType("event-bus").setConfig(new JsonObject().put("address", confStoreAddress)));

    VertxDeploy.deploy(vertx, new ConfVerticle(ConfigRetriever.create(vertx, opts))) // deploy ConfVerticle
      .compose(x -> VertxDeploy.deploy(vertx, new MyVerticleSelfConfChangeListener())) // deploy ServiceVerticle
      .map(x -> vertx.eventBus().publish(confStoreAddress, changedGlobalConf)) // change configuration
      .compose(x -> wait(vertx, 200)) // wait for configuration to be propagated
      .compose(x -> VertxEndpointClient.make(vertx, ConfListener.class).changedConfig()) // get changed configuration from ServiceVerticle
      .map(changedConf -> ctx.assertEquals(true, ((JsonObject)changedConf).getBoolean("changed"))) // assert
      .setHandler(ctx.asyncAssertSuccess());
  }

  private Future wait(Vertx vertx, long delayMs) {
    Future promise = Future.future();
    vertx.setTimer(delayMs, y -> promise.complete());
    return promise;
  }
}
