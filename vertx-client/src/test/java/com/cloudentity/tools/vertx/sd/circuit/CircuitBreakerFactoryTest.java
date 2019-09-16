package com.cloudentity.tools.vertx.sd.circuit;

import com.cloudentity.tools.vertx.bus.VertxBus;
import com.cloudentity.tools.vertx.conf.fixed.FixedConfVerticle;
import com.cloudentity.tools.vertx.sd.NodeId;
import com.cloudentity.tools.vertx.sd.ServiceName;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.circuitbreaker.CircuitBreakerState;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import scala.Option;

@RunWith(VertxUnitRunner.class)
public class CircuitBreakerFactoryTest {
  @Rule
  public RunTestOnContext rule = new RunTestOnContext();

  int resetTimeout = 1000;
  int waitForCbUpdate = 100;

  CircuitBreakerOptions opts = new CircuitBreakerOptions().setMaxFailures(1).setResetTimeout(resetTimeout);

  @Test
  public void shouldReturnCircuitBreakerThatOpensAfterOneFailure(TestContext ctx) {
    VertxBus.registerPayloadCodec(rule.vertx().eventBus());

    // given
    Option<JsonObject> opts = Option.<JsonObject>apply(this.opts.toJson());

    // when
    CircuitBreakerFactory.fromConfig(rule.vertx(), ServiceName.apply("authz"), opts)
      // then
      .compose(factory -> testCircuitBreakerFactoryConfig(factory, true))
      .setHandler(ctx.asyncAssertSuccess());
  }

  @Test
  public void shouldReturnInactiveCircuitBreakerIfConfMissing(TestContext ctx) {
    VertxBus.registerPayloadCodec(rule.vertx().eventBus());

    // given
    Option<JsonObject> opts = Option.<JsonObject>empty();
    
      // when
    CircuitBreakerFactory.fromConfig(rule.vertx(), ServiceName.apply("authz"), opts)
      .compose(factory -> testCircuitBreakerFactoryConfig(factory, false))
      // then
      .setHandler(ctx.asyncAssertSuccess());
  }

  @Test
  public void shouldReturnInactiveCircuitBreakerIfOffAttributeSet(TestContext ctx) {
    VertxBus.registerPayloadCodec(rule.vertx().eventBus());

    // given
    Option<JsonObject> opts = Option.<JsonObject>apply(this.opts.toJson().put("off", true));

    // when
    CircuitBreakerFactory.fromConfig(rule.vertx(), ServiceName.apply("authz"), opts)
      .compose(factory -> testCircuitBreakerFactoryConfig(factory, false))
      // then
      .setHandler(ctx.asyncAssertSuccess());
  }

  // verifies that the factory builds circuit-breaker opens circuit after one responseFailed
  private Future testCircuitBreakerFactoryConfig(CircuitBreakerFactory cbf, boolean opensCircuit) {
    Future promise = Future.future();
    CircuitBreaker cb = cbf.build().apply(NodeId.apply("id"));

    Assert.assertEquals(CircuitBreakerState.CLOSED, cb.state());
    cb.execute(future -> future.fail(""));

    rule.vertx().setTimer(waitForCbUpdate, t -> {
      try {
        CircuitBreakerState expectedState;
        if (opensCircuit) expectedState = CircuitBreakerState.OPEN;
        else expectedState = CircuitBreakerState.CLOSED;

        Assert.assertEquals(expectedState, cb.state());
        promise.complete();
      } catch (Exception ex) {
        promise.fail(ex);
      }
    });
    return promise;
  }
}
