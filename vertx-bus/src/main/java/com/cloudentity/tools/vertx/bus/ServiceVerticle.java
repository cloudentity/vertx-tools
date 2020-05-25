package com.cloudentity.tools.vertx.bus;

import io.vertx.core.Future;

import java.util.Optional;

/**
 * This base verticle provides abstraction over event bus communication.
 * Instead of consuming from event bus and responding to the messages, the implementor of the verticle needs only
 * to provide implementation of interface with methods annotated with {@link com.cloudentity.tools.vertx.bus.VertxEndpoint}.
 *
 * The consumer registration and response dispatching is handled seamlessly.
 *
 * For the example usage see {@link examples.ServiceVerticleExamples}
 */
public abstract class ServiceVerticle extends ComponentVerticle {

  protected Optional<String> vertxServiceAddressPrefix() {
    if (Boolean.TRUE.equals(config().getValue("prefix"))) {
      return Optional.of(verticleId());
    } else return Optional.ofNullable(config().getString("prefix"));
  }

  @Override
  public void start(Future<Void> start) {
    toFuture(super::start).map(x -> {
      VertxEndpointServer.init(vertx, this, vertxServiceAddressPrefix());
      initService();
      return (Void) null;
    }).compose(x -> initServiceAsync()).setHandler(start);
  }

  /**
   * Override this method to execute verticle's initialization logic.
   * It's executed at the end of asynchronous ServiceVerticle.start().
   *
   * Use this method instead of overriding asynchronous AbstractVerticle.start().
   */
  protected Future<Void> initServiceAsync() {
    return Future.succeededFuture();
  }

  /**
   * Override this method to execute verticle's initialization logic.
   * It's executed in sync ServiceVerticle.start() as soon as the verticle's configuration is loaded.
   *
   * Use this method instead of overriding AbstractVerticle.start().
   */
  protected void initService() {
  }
}
