package com.cloudentity.tools.vertx.http.builder;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;

public class SmartHttpResponseImpl implements SmartHttpResponse {
  private final Buffer body;
  private final HttpClientResponse http;

  public SmartHttpResponseImpl(Buffer body, HttpClientResponse http) {
    this.body = body;
    this.http = http;
  }

  @Override
  public Buffer getBody() {
    return body;
  }

  @Override
  public HttpClientResponse getHttp() {
    return http;
  }
}
