package com.cloudentity.tools.vertx.bus;

import com.google.common.collect.Lists;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.cloudentity.tools.vertx.bus.VertxEndpointClient.*;

public class VertxEndpointServer {
  private static final Logger log = LoggerFactory.getLogger(VertxEndpointServer.class);

  public static void init(Vertx vertx, Object verticle, Optional<String> addressPrefixOpt) {
    List<Class> interfaces = VertxEndpointServer.allInterfaces(verticle.getClass());

    interfaces.stream().filter(i -> {
      List<Method> methods = Lists.newArrayList(i.getMethods());
      return methods.stream().filter(m -> m.getAnnotation(VertxEndpoint.class) != null).findAny().isPresent();
    }).forEach(vertxService -> init(vertx, verticle, vertxService, addressPrefixOpt));
  }

  private static void init(Vertx vertx, Object verticle, Class vertxService, Optional<String> addressPrefixOpt) {
    List<VertxEndpointInterface> vertxEndpoints = getVertxEndpoints(vertxService, addressPrefixOpt);
    VertxEndpointServer.assertVertxServiceInterfaceImplemented(vertxService, verticle);
    VertxEndpointServer.assertVertxEndpointsReturnFutureOrVoid(vertxService, vertxEndpoints);

    VertxEndpointServer.registerConsumers(vertx, verticle, vertxEndpoints);
  }

  private static void assertVertxServiceInterfaceImplemented(Class vertxService, Object verticle) {
    if (!allInterfaces(verticle.getClass()).contains(vertxService)) {
      String errMsg = "Declared ServiceVerticle.service()=" + vertxService.getName() +" interface is not implemented by this class=" + verticle.getClass().getName();
      log.error(errMsg);
      throw new IllegalStateException(errMsg);
    }
  }

  private static List<Class> allInterfaces(Class clazz) {
    return allInterfaces(clazz, Lists.newLinkedList());
  }

  private static List<Class> allInterfaces(Class clazz, List<Class> acc) {
    if (clazz != null) {
      acc.addAll(Lists.newArrayList(clazz.getInterfaces()));
      return allInterfaces(clazz.getSuperclass(), acc);
    } else {
      return acc;
    }
  }

  private static void assertVertxEndpointsReturnFutureOrVoid(Class vertxService, List<VertxEndpointInterface> vertxEndpoints) {
    List<VertxEndpointInterface> nonFutureMethods = vertxEndpoints.stream().filter(endpoint -> endpoint.method.getReturnType() != Future.class && endpoint.method.getReturnType() != Void.TYPE).collect(Collectors.toList());
    if (!nonFutureMethods.isEmpty()) {
      String errMsg = "Methods on ServiceVerticle.service()=" + vertxService.getName() + " interface annotated with @VertxEndpoint must return io.vertx.core.Future or void: " + nonFutureMethods;
      log.error(errMsg);
      throw new IllegalStateException(errMsg);
    }
  }

  private static void registerConsumers(Vertx vertx, Object verticle, List<VertxEndpointInterface> vertxEndpoints) {
    vertxEndpoints.forEach(e -> registerConsumer(vertx, verticle, e));
  }

  private static void registerConsumer(Vertx vertx, Object verticle, VertxEndpointInterface endpoint) {
    // we will find the implementation because we executed `assertVertxServiceInterfaceImplemented`
    Method methodImpl = findMethodImpl(verticle, endpoint.method);

    if (methodImpl.getReturnType() == Future.class) {
      registerSendConsumer(vertx, verticle, endpoint, methodImpl);
    } else {
      registerPublishConsumer(vertx, verticle, endpoint, methodImpl);
    }
  }

  private static void registerPublishConsumer(Vertx vertx, Object verticle, VertxEndpointInterface endpoint, Method methodImpl) {
    VertxBus.consumePublished(vertx.eventBus(), endpoint.address, ServiceRequest.class, request -> {
      try {
        invoke(verticle, methodImpl, request);
      } catch (Throwable ex) {
        log.error("Invoking method={} on address={} with request={} threw an exception", methodImpl, endpoint.address, request, ex);
      }
    });
  }

  private static void registerSendConsumer(Vertx vertx, Object verticle, VertxEndpointInterface endpoint, Method methodImpl) {
    VertxBus.<ServiceRequest, ServiceResponse>consume(vertx.eventBus(), endpoint.address, ServiceRequest.class, request -> {
      try {
        // we can cast result to Future because we checked `methodImpl.getReturnType() == Future.class`
        Future future = (Future) invoke(verticle, methodImpl, request);
        return future.map(x -> new ServiceResponse(x));
      } catch (Throwable ex) {
        log.error("Invoking method={} on address={} with request={} threw an exception", methodImpl, endpoint.address, request, ex);
        return Future.failedFuture(ex);
      }
    });
  }

  private static Method findMethodImpl(Object verticle, Method m) {
    return
    Arrays.asList(verticle.getClass().getMethods()).stream()
      .filter(mm -> stringEquals(m.getName(), mm.getName()) && Arrays.equals(m.getParameterTypes(), mm.getParameterTypes()))
      .findAny()
      .get();
  }

  private static boolean stringEquals(String a, String b) {
    if (a == null && b == null) return true;
    else if (a != null) return a.equals(b);
    else return false;
  }

  private static Object invoke(Object verticle, Method m, ServiceRequest request) throws InvocationTargetException, IllegalAccessException {
    return m.invoke(verticle, request.values.toArray());
  }
}
