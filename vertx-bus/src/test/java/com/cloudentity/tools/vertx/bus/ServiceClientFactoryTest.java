package com.cloudentity.tools.vertx.bus;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Optional;

@RunWith(VertxUnitRunner.class)
public class ServiceClientFactoryTest {
  Vertx vertx = Vertx.vertx();
  String address = "service-verticle-dostuff";

  @Before
  public void init() {
    VertxBus.registerPayloadCodec(vertx.eventBus());
  }

  interface MyService {
    @VertxEndpoint(address = "service-verticle-dostuff")
    Future<String> doStuff(String x, int y);
  }

  @Test
  public void clientShouldSendAndReceiveMessageProperlyWithoutAddressPrefix(TestContext ctx) {
    testClientShouldSendAndReceiveMessageProperly(ctx, Optional.empty());
  }

  @Test
  public void clientShouldSendAndReceiveMessageProperlyWithAddressPrefix(TestContext ctx) {
    testClientShouldSendAndReceiveMessageProperly(ctx, Optional.of("prefix:"));
  }

  private void testClientShouldSendAndReceiveMessageProperly(TestContext ctx, Optional<String> addressPrefix) {
    Async async = ctx.async();
    // given
    String x = "x";
    int y = 3;
    String response = "xxx";

    VertxBus.consume(vertx.eventBus(), addressPrefix.orElse("") + address, ServiceRequest.class,
      request -> {
        ctx.assertEquals(2, request.values.size());
        ctx.assertEquals(x, request.values.get(0));
        ctx.assertEquals(y, request.values.get(1));

        return Future.succeededFuture(new ServiceResponse(response));
      });

    // when
    MyService client = VertxEndpointClient.make(vertx, MyService.class, addressPrefix);

    // then
    client.doStuff(x, y).setHandler(result -> {
      ctx.assertTrue(result.succeeded());
      ctx.assertEquals(response, result.result());
      async.complete();
    });
  }

  @Test
  public void clientShouldReturnFailedFutureWhenNoServiceProviderConsumingRequests(TestContext ctx) {
    Async async = ctx.async();
    // given
    MyService client = VertxEndpointClient.make(vertx, MyService.class);

    // when
    Future<String> future = client.doStuff("x", 3);

    // then
    future.setHandler(result -> {
      ctx.assertTrue(result.failed());
      async.complete();
    });
  }

  interface NonFutureService {
    @VertxEndpoint(address = "service-verticle-dostuff")
    String doStuff(String x, int y);
  }

  @Test(expected = IllegalStateException.class)
  public void makeShouldThrowExceptionWhenSomeInterfaceMethodsDoesNotReturnFuture() {
    VertxEndpointClient.make(vertx, NonFutureService.class);
  }

  interface NonVertxEndpointService {
    @VertxEndpoint(address = "service-verticle-dostuff")
    String doStuff(String x, int y);

    String doSomethingElse(String x, int y);
  }

  @Test(expected = IllegalStateException.class)
  public void makeShouldThrowExceptionWhenSomeInterfaceMethodsAreNotVertxEndpoints() {
    VertxEndpointClient.make(vertx, NonVertxEndpointService.class);
  }
}
