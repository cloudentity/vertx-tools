package com.cloudentity.tools.vertx.bus;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(VertxUnitRunner.class)
public class VertxBusTest {
  Vertx vertx = Vertx.vertx();
  EventBus bus = vertx.eventBus();

  @Before
  public void init() {
    VertxBus.registerPayloadCodec(vertx.eventBus());
  }

  @Test
  public void shouldReturnResponseBackFromConsumerOnAsk(TestContext ctx) {
    Async async = ctx.async();

    // given
    String addr = UUID.randomUUID().toString();
    VertxBus.consume(bus, addr, String.class, a -> Future.succeededFuture(a + a));

    // when
    Future<String> future = VertxBus.ask(bus, addr, String.class, "echo");

    // then
    future.setHandler(result -> {
      assertTrue(result.succeeded());
      assertEquals("echoecho", result.result());
      async.complete();
    });
  }

  @Test
  public void shouldPassExceptionToSenderFromConsumersFailedFutureOnAsk(TestContext ctx) {
    Async async = ctx.async();

    // given
    String addr = UUID.randomUUID().toString();
    VertxBus.consume(bus, addr, String.class, a -> Future.failedFuture(new IllegalStateException("consumer-exception")));

    // when
    Future<String> future = VertxBus.ask(bus, addr, String.class, "echo");

    // then
    future.setHandler(result -> {
      assertTrue(result.failed());
      assertEquals("consumer-exception", result.cause().getMessage());
      assertEquals(IllegalStateException.class, result.cause().getClass());
      async.complete();
    });
  }

  @Test
  public void shouldPassExceptionToSenderThrownByConsumersOnAsk(TestContext ctx) {
    Async async = ctx.async();

    // given
    String addr = UUID.randomUUID().toString();
    VertxBus.consume(bus, addr, String.class, a -> { throw new IllegalStateException("consumer-exception"); });

    // when
    Future<String> future = VertxBus.ask(bus, addr, String.class, "echo");

    // then
    future.setHandler(result -> {
      assertTrue(result.failed());
      assertEquals("consumer-exception", result.cause().getMessage());
      assertEquals(IllegalStateException.class, result.cause().getClass());
      async.complete();
    });
  }

  @Test
  public void shouldReturnFailedFutureWhenConsumerBlocksThread(TestContext ctx) {
    Async async = ctx.async();

    // given
    String addr = UUID.randomUUID().toString();
    VertxBus.consume(bus, addr, String.class, a -> {
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return Future.succeededFuture(a + a);
    });

    // when
    Future<String> future = VertxBus.ask(bus, addr, String.class, "echo");

    // then
    future.setHandler(result -> {
      assertTrue(result.failed());
      async.complete();
    });
  }

  @Test
  public void shouldReturnFailedFutureWhenNoHandlersRegisteredOnAddress(TestContext ctx) {
    Async async = ctx.async();

    // given
    String addr = UUID.randomUUID().toString();

    // when
    Future<String> future = VertxBus.ask(bus, addr, String.class, "echo");

    // then
    future.setHandler(result -> {
      assertTrue(result.failed());
      async.complete();
    });
  }

  @Test
  public void shouldReturnFailedFutureWhenConsumerReturnsFailedFuture(TestContext ctx) {
    Async async = ctx.async();

    // given
    String addr = UUID.randomUUID().toString();
    VertxBus.consume(bus, addr, String.class, a -> Future.failedFuture("error"));

    // when
    Future<String> future = VertxBus.ask(bus, addr, String.class, "echo");

    // then
    future.setHandler(result -> {
      assertTrue(result.failed());
      async.complete();
    });
  }
}
