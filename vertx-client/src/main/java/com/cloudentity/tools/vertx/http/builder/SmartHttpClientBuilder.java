package com.cloudentity.tools.vertx.http.builder;

import com.cloudentity.tools.vertx.http.SmartHttpClient;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;

public interface SmartHttpClientBuilder extends CallValuesBuilderImpl<SmartHttpClientBuilder> {
  /**
   * No default value.
   * Build returns failed Future if `serviceName` missing.
   */
  SmartHttpClientBuilder serviceName(String n);

  /**
   * Default value: empty list.
   */
  SmartHttpClientBuilder serviceTags(String... n);

  /**
   * Default value: new HttpClientOptions()
   */
  SmartHttpClientBuilder httpClientOptions(HttpClientOptions o);

  /**
   * Optional. If not present or field off=true then circuit-breaker is inactive.
   */
  SmartHttpClientBuilder circuitBreakerConfig(JsonObject c);

  Future<SmartHttpClient> build();
}
