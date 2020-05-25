package com.cloudentity.tools.vertx.bus;

import com.cloudentity.tools.vertx.tracing.TracingManager;
import com.cloudentity.tools.vertx.tracing.internals.JaegerTracing;
import com.google.common.collect.Lists;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VertxEndpointClient {
  private static final int VERTX_SERVICE_CLIENT_TIMEOUT;

  static {
    if (System.getenv("VERTX_SERVICE_CLIENT_TIMEOUT") != null) {
      VERTX_SERVICE_CLIENT_TIMEOUT = Integer.valueOf(System.getenv("VERTX_SERVICE_CLIENT_TIMEOUT"));
    } else if (System.getProperty("VERTX_SERVICE_CLIENT_TIMEOUT") != null) {
      VERTX_SERVICE_CLIENT_TIMEOUT = Integer.valueOf(System.getProperty("VERTX_SERVICE_CLIENT_TIMEOUT"));
    } else {
      VERTX_SERVICE_CLIENT_TIMEOUT = 30000;
    }
  }

  /**
   * Builds a client that sends request over Vertx' event bus.
   * Each method in `clazz` interface with @VertxEndpoint is implemented by sending a request with method's arguments
   * wrapped in ServiceRequest object at VertxEndpoint.address().
   */
  public static <T> T makeWithTracing(Vertx vertx, TracingManager tracing, Class<T> clazz, Optional<String> addressPrefixOpt, DeliveryOptions opts) {
    List<VertxEndpointInterface> vertxEndpoints = getVertxEndpoints(clazz, addressPrefixOpt);

    assertAllMethodsAreVertxEndpoints(clazz, vertxEndpoints);
    assertAllReturnFutureOrVoid(clazz, vertxEndpoints);

    Map<Method, Function<Object[], Future<Object>>> methodHandlers = buildFutureMethodHandlers(vertx.eventBus(), tracing, vertxEndpoints, opts);
    Map<Method, Consumer<Object[]>> methodVoidHandlers = buildVoidMethodHandlers(vertx.eventBus(), vertxEndpoints, opts);

    InvocationHandler handler = new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getReturnType() == Future.class) {
          return methodHandlers.get(method).apply(args == null ? new Object[0] : args);
        } else {
          methodVoidHandlers.get(method).accept(args == null ? new Object[0] : args);
          return null;
        }
      }
    };

    return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, handler);
  }

  public static <T> T makeWithTracing(Vertx vertx, TracingManager tracing, Class<T> clazz, Optional<String> addressPrefixOpt) {
    return makeWithTracing(vertx, tracing, clazz, addressPrefixOpt, new DeliveryOptions().setSendTimeout(VERTX_SERVICE_CLIENT_TIMEOUT));
  }

  public static <T> T makeWithTracing(Vertx vertx, TracingManager tracing, Class<T> clazz) {
    return makeWithTracing(vertx, tracing, clazz, Optional.empty(), new DeliveryOptions().setSendTimeout(VERTX_SERVICE_CLIENT_TIMEOUT));
  }

  public static <T> T make(Vertx vertx, Class<T> clazz, Optional<String> addressPrefixOpt, DeliveryOptions opts) {
    return makeWithTracing(vertx, JaegerTracing.noTracing, clazz, addressPrefixOpt, opts);
  }

  public static <T> T make(Vertx vertx, Class<T> clazz, Optional<String> addressPrefixOpt) {
    return make(vertx, clazz, addressPrefixOpt, new DeliveryOptions().setSendTimeout(VERTX_SERVICE_CLIENT_TIMEOUT));
  }

  public static <T> T make(Vertx vertx, Class<T> clazz) {
    return make(vertx, clazz, Optional.empty(), new DeliveryOptions().setSendTimeout(VERTX_SERVICE_CLIENT_TIMEOUT));
  }

  private static void assertAllMethodsAreVertxEndpoints(Class clazz, List<VertxEndpointInterface> vertxEndpoints) {
    if (clazz.getMethods().length != vertxEndpoints.size()) {
      throw new IllegalStateException("All methods in " + clazz.getName() + " must be annotated with @VertxEndpoint.");
    }
  }

  private static Map<Method, Function<Object[], Future<Object>>> buildFutureMethodHandlers(EventBus bus, TracingManager tracing,
                                                                                           List<VertxEndpointInterface> vertxEndpoints, DeliveryOptions opts) {
    Map<Method, Function<Object[], Future<Object>>> handlers = new HashMap<>();
    vertxEndpoints.forEach(endpoint -> handlers.put(endpoint.method, buildMethodHandler(bus, tracing, endpoint, opts)));
    return handlers;
  }

  private static Map<Method, Consumer<Object[]>> buildVoidMethodHandlers(EventBus bus, List<VertxEndpointInterface> vertxEndpoints, DeliveryOptions opts) {
    Map<Method, Consumer<Object[]>> handlers = new HashMap<>();
    vertxEndpoints.forEach(endpoint -> handlers.put(endpoint.method, buildVoidMethodHandler(bus, endpoint, opts)));
    return handlers;
  }

  private static Consumer<Object[]> buildVoidMethodHandler(EventBus bus, VertxEndpointInterface endpoint, DeliveryOptions opts) {
    return (objects) -> VertxBus.publish(bus, endpoint.address, opts, new ServiceRequest(Arrays.asList(objects)));
  }

  private static Function<Object[], Future<Object>> buildMethodHandler(EventBus bus, TracingManager tracing, VertxEndpointInterface endpoint, DeliveryOptions opts) {
    return (objects) -> {
      ServiceTracing.ObjectsWithContext obt = ServiceTracing.createNewSpanForTracingContext(tracing, endpoint, objects);
      return VertxBus
        .ask(bus, endpoint.address, opts, ServiceResponse.class, new ServiceRequest(Arrays.asList(obt.getObjectsWithContext())))
        .map(event -> { obt.finishSpan(); return event; })
        .recover(t -> { obt.logErrorAndFinish(t); return Future.failedFuture(t); })
        .map(event -> event.value);
    };
  }

  private static void assertAllReturnFutureOrVoid(Class clazz, List<VertxEndpointInterface> methods) {
    List<VertxEndpointInterface> methodsNotReturningFutures = methods.stream().filter(endpoint -> (endpoint.method.getReturnType() != Future.class && endpoint.method.getReturnType() != Void.TYPE)).collect(Collectors.toList());
    if (!methodsNotReturningFutures.isEmpty()) {
      throw new IllegalStateException("All VertxEndpoint methods in " + clazz.getName() + " need to return io.vertx.core.Future or void: " + methodsNotReturningFutures.toString());
    }
  }

  /**
   * Returns a list of methods that are annotated with @VertxEndpoint.
   */
  public static List<VertxEndpointInterface> getVertxEndpoints(Class clazz, Optional<String> addressPrefixOpt) {
    List<VertxEndpointInterface> endpoints = new LinkedList<>();
    String addressPrefix = addressPrefixOpt.orElse("");

    Arrays.asList(clazz.getMethods())
      .forEach(method -> {
        VertxEndpoint[] endpointAnnotations = method.getDeclaredAnnotationsByType(VertxEndpoint.class);

        if (endpointAnnotations.length == 1) {
          VertxEndpoint a = endpointAnnotations[0];
          String address = getEndpointAddress(a, method);
          endpoints.add(new VertxEndpointInterface(addressPrefix + address, method));
        }
      });

    return endpoints;
  }

  /**
   * Returns `VertxEndpoint.address` if it was explicitly set.
   * Otherwise returns full name of annotated method: {class-name}.{method-name}({comma-separated-parameters})
   */
  private static String getEndpointAddress(VertxEndpoint e, Method method) {
    if (!VertxEndpoint.DERIVE_ADDRESS.equals(e.address())) {
      return e.address();
    } else {
      String parameters = String.join(", ", Lists.newArrayList(method.getParameterTypes()).stream().map(clazz -> clazz.getSimpleName()).collect(Collectors.toList()));
      return String.format("%s.%s(%s)", method.getDeclaringClass().getName(), method.getName(), parameters);
    }
  }

  public static class VertxEndpointInterface {
    public final String address;
    public final Method method;

    public VertxEndpointInterface(String address, Method method) {
      this.address = address;
      this.method = method;
    }

    @Override
    public String toString() {
      return "VertxEndpointInterface{" +
        "address='" + address + '\'' +
        ", method=" + method +
        '}';
    }
  }
}
