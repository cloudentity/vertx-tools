package com.cloudentity.tools.vertx.verticles;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.util.function.Supplier;

public class VertxUtil {
  public static <T> Future<T> executeBlocking(Vertx vertx, Supplier<T> supp) {
    Future<T> f = Future.future();
    vertx.<T>executeBlocking(s -> {
      try {
        s.complete(supp.get());
      } catch (Throwable t){
        s.fail(t);
      }
    }, result -> f.handle(result));
    return f;
  }
}
