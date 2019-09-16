package com.cloudentity.tools.vertx.sd.circuit;

import io.vertx.circuitbreaker.CircuitBreakerState;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class NoopCBTest {
  @Rule
  public RunTestOnContext rule = new RunTestOnContext();

  @Test
  public void shouldReturnOperationResult(TestContext ctx) {
    NoopCB cb = new NoopCB("");

    cb.execute(f -> {
      rule.vertx().setTimer(20, t -> f.complete(100));
    }).setHandler(ctx.asyncAssertSuccess(result -> ctx.assertEquals(100, result)));
  }

  @Test
  public void shouldReturnOperationFailure(TestContext ctx) {
    NoopCB cb = new NoopCB("");

    cb.execute(f -> {
      rule.vertx().setTimer(20, t -> f.fail("error"));
    }).setHandler(ctx.asyncAssertFailure(failure -> ctx.assertEquals("error", failure.getMessage())));
  }

  @Test
  public void shouldNeverOpen(TestContext ctx) {
    NoopCB cb = new NoopCB("");

    for (int i = 0; i < 1000; i++) {
      cb.execute(f -> f.fail(""));
    }

    ctx.assertEquals(CircuitBreakerState.CLOSED, cb.state());
  }
}
