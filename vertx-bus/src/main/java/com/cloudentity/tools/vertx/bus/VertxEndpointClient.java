package com.cloudentity.tools.vertx.bus;

import com.cloudentity.tools.vertx.tracing.TracingManager;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;

import java.util.Optional;

public class VertxEndpointClient {
  public static <T> T makeWithTracing(Vertx vertx, TracingManager tracing, Class<T> clazz, Optional<String> addressPrefixOpt, DeliveryOptions opts) {
    return ServiceClientFactory.makeWithTracing(vertx.eventBus(), tracing, clazz, addressPrefixOpt, opts);
  }

  public static <T> T makeWithTracing(Vertx vertx, TracingManager tracing, Class<T> clazz, Optional<String> addressPrefixOpt) {
    return ServiceClientFactory.makeWithTracing(vertx.eventBus(), tracing, clazz, addressPrefixOpt);
  }

  public static <T> T makeWithTracing(Vertx vertx, TracingManager tracing, Class<T> clazz) {
    return ServiceClientFactory.makeWithTracing(vertx.eventBus(), tracing, clazz);
  }

  public static <T> T make(Vertx vertx, Class<T> clazz, Optional<String> addressPrefixOpt, DeliveryOptions opts) {
    return ServiceClientFactory.make(vertx.eventBus(), clazz, addressPrefixOpt, opts);
  }

  public static <T> T make(Vertx vertx, Class<T> clazz, Optional<String> addressPrefixOpt) {
    return ServiceClientFactory.make(vertx.eventBus(), clazz, addressPrefixOpt);
  }

  public static <T> T make(Vertx vertx, Class<T> clazz) {
    return ServiceClientFactory.make(vertx.eventBus(), clazz);
  }
}
