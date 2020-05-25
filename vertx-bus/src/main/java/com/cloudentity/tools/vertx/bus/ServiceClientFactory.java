package com.cloudentity.tools.vertx.bus;

import com.cloudentity.tools.vertx.tracing.TracingManager;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;

import java.util.Optional;

/**
 * Deprecated, use com.cloudentity.tools.vertx.bus.VertxEndpointClient static methods.
 */
@Deprecated()
public class ServiceClientFactory {
  public static <T> T makeWithTracing(EventBus bus, TracingManager tracing, Class<T> clazz, Optional<String> addressPrefixOpt, DeliveryOptions opts) {
    return VertxEndpointClient.makeWithTracing(Vertx.currentContext().owner(), tracing, clazz, addressPrefixOpt, opts);
  }

  public static <T> T makeWithTracing(EventBus bus, TracingManager tracing, Class<T> clazz, Optional<String> addressPrefixOpt) {
    return VertxEndpointClient.makeWithTracing(Vertx.currentContext().owner(), tracing, clazz, addressPrefixOpt);
  }

  public static <T> T makeWithTracing(EventBus bus, TracingManager tracing, Class<T> clazz) {
    return VertxEndpointClient.makeWithTracing(Vertx.currentContext().owner(), tracing, clazz);
  }

  public static <T> T make(EventBus bus, Class<T> clazz, Optional<String> addressPrefixOpt, DeliveryOptions opts) {
    return VertxEndpointClient.make(Vertx.currentContext().owner(), clazz, addressPrefixOpt, opts);
  }

  public static <T> T make(EventBus bus, Class<T> clazz, Optional<String> addressPrefixOpt) {
    return VertxEndpointClient.make(Vertx.currentContext().owner(), clazz, addressPrefixOpt);
  }

  public static <T> T make(EventBus bus, Class<T> clazz) {
    return VertxEndpointClient.make(Vertx.currentContext().owner(), clazz);
  }
}
