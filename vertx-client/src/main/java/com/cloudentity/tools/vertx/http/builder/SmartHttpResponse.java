package com.cloudentity.tools.vertx.http.builder;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;

public interface SmartHttpResponse {
  Buffer getBody();
  HttpClientResponse getHttp();
}
