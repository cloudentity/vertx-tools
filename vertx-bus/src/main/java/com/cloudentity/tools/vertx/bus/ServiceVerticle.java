package com.cloudentity.tools.vertx.bus;

import com.google.common.collect.Lists;
import io.vertx.core.Future;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

  /**
   * Deprecated: no need to declare service interface.
   * It's automatically calculated in {@link ServiceVerticle#vertxServices}
   */
  @Deprecated
  protected Class vertxService() {
    return null;
  }

  /**
   * Returns vertx-service interfaces with methods annotated with @VertxEndpoint implemented by this verticle.
   * Based on that we can figure out what addresses we should consume on and what methods to call to handle the incoming messages.
   *
   * @return interface with methods annotated with @VertxEndpoint, implemented by the class extending ServiceVerticle.
   */
  public List<Class> vertxServices() {
    List<Class> interfaces = ServiceVerticleTools.allInterfaces(this.getClass());

    return interfaces.stream().filter(i -> {
      List<Method> methods = Lists.newArrayList(i.getMethods());
      return methods.stream().filter(m -> m.getAnnotation(VertxEndpoint.class) != null).findAny().isPresent();
    }).collect(Collectors.toList());
  }

  protected Optional<String> vertxServiceAddressPrefix() {
    if (Boolean.TRUE.equals(config().getValue("prefix"))) {
      return Optional.of(verticleId());
    } else return Optional.ofNullable(config().getString("prefix"));
  }

  @Override
  public void start(Future<Void> start) {
    toFuture(super::start).compose(x -> {
      List<Class> vertxServices = vertxServices();
      if (!vertxServices.isEmpty()) {
        vertxServices.forEach(vertxService -> {
          ServiceVerticleTools.init(vertx, this, vertxService, vertxServiceAddressPrefix());
        });
        return Future.succeededFuture();
      } else {
        return Future.failedFuture(this.getClass().getName() + " is a ServiceVerticle, but does not implement any @VertxEndpoints");
      }
    }).map(x -> {
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
