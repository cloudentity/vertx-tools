package com.cloudentity.tools.vertx.http.builder;

import com.cloudentity.tools.vertx.tracing.TracingContext;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;

public interface RequestCtxBuilder extends CallValuesBuilder<RequestCtxBuilder> {
  RequestCtxBuilder method(HttpMethod m);
  RequestCtxBuilder putHeader(String key, String value);

  Future<HttpClientResponse> end();
  Future<HttpClientResponse> end(Buffer b);
  Future<HttpClientResponse> end(String b);

  Future<HttpClientResponse> end(Handler<Buffer> bodyHandler);
  Future<HttpClientResponse> end(Buffer b, Handler<Buffer> bodyHandler);
  Future<HttpClientResponse> end(String b, Handler<Buffer> bodyHandler);

  Future<SmartHttpResponse> endWithBody();
  Future<SmartHttpResponse> endWithBody(Buffer b);
  Future<SmartHttpResponse> endWithBody(String b);

  Future<HttpClientResponse> end(TracingContext tracingContext);
  Future<HttpClientResponse> end(TracingContext tracingContext, Buffer b);
  Future<HttpClientResponse> end(TracingContext tracingContext, String b);

  Future<HttpClientResponse> end(TracingContext tracingContext, Handler<Buffer> bodyHandler);
  Future<HttpClientResponse> end(TracingContext tracingContext, Buffer b, Handler<Buffer> bodyHandler);
  Future<HttpClientResponse> end(TracingContext tracingContext, String b, Handler<Buffer> bodyHandler);

  Future<SmartHttpResponse> endWithBody(TracingContext tracingContext);
  Future<SmartHttpResponse> endWithBody(TracingContext tracingContext, Buffer b);
  Future<SmartHttpResponse> endWithBody(TracingContext tracingContext, String b);
}
