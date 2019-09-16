package com.cloudentity.tools.vertx.http.builder;

import io.vertx.core.http.HttpClientResponse;

import java.util.function.Function;

public interface CallValuesBuilder<Builder> {
  /**
   * Default value: no failed responses
   *
   * If the provided function returns true then circuit-breaker failure counter is increased.
   * If the provided function returns true and 'retryFailedResponse' is set to true then the request is retried
   */
  Builder responseFailure(Function<SmartHttpResponse, Boolean> rf);

  /**
   * Default value: true
   */
  Builder retryFailedResponse(boolean retry);

  /**
   * Default value: 0
   */
  Builder retries(int r);

  /**
   * Default value: true
   */
  Builder retryOnException(boolean r);

  /**
   * Default value: none
   */
  Builder responseTimeout(int t);
}
