package com.cloudentity.tools.vertx.futures;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class FutureUtils {

  public static <T> Future<T> completableFutureToVertxFuture(CompletableFuture<T> completableFuture) {
    Future<T> future = Future.future();
    completableFuture.handle((result, throwable) -> {
      if (result != null) {
        future.complete(result);
        return result;
      } else {
        future.fail(throwable);
        return throwable;
      }
    });
    return future;
  }

  public static <T> CompletableFuture<T> vertxFutureToCompletableFuture(Future<T> future) {
    CompletableFuture<T> completable = new CompletableFuture();
    future.setHandler(result -> {
      if (result.succeeded()) {
        completable.complete(result.result());
      } else {
        completable.completeExceptionally(future.cause());
      }
    });
    return completable;
  }

  public static <T> Future<T> withException(Future<T> source, Consumer<Throwable> exceptionHandler) {
    Future future = Future.future();
    source.setHandler(result -> {
      if (result.succeeded()) {
        future.complete(result.result());
      } else {
        exceptionHandler.accept(result.cause());
        future.fail(result.cause());
      }
    });
    return future;
  }

  public static <T> Future<List<T>> sequence(List<Future<T>> fs) {
    Future<List<T>> promise = Future.future();

    CompositeFuture.all((List<Future>) ((Object) fs)).setHandler(cf -> {
      if (cf.succeeded()) {
        promise.complete(cf.result().list());
      } else {
        promise.fail(cf.cause());
      }
    });

    return promise;
  }

  public static <T> Future<T> asFuture(Consumer<Handler<AsyncResult<T>>> f) {
    Future<T> promise = Future.future();

    f.accept(async -> {
      if (async.succeeded()) promise.complete(async.result());
      else promise.fail(async.cause());
    });

    return promise;
  }
}
