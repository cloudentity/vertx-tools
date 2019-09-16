package com.cloudentity.tools.vertx.http;

import com.cloudentity.tools.vertx.http.builder.RequestCtxBuilder;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;

public interface SmartHttpClient {
  RequestCtxBuilder request(HttpMethod method, String uri);

  default RequestCtxBuilder post(String uri) { return this.request(HttpMethod.POST, uri); }
  default RequestCtxBuilder get(String uri) { return this.request(HttpMethod.GET, uri); }
  default RequestCtxBuilder put(String uri) { return this.request(HttpMethod.PUT, uri); }
  default RequestCtxBuilder delete(String uri) { return this.request(HttpMethod.DELETE, uri); }
  default RequestCtxBuilder options(String uri) { return this.request(HttpMethod.OPTIONS, uri); }
  default RequestCtxBuilder head(String uri) { return this.request(HttpMethod.HEAD, uri); }
  default RequestCtxBuilder trace(String uri) { return this.request(HttpMethod.TRACE, uri); }
  default RequestCtxBuilder connect(String uri) { return this.request(HttpMethod.CONNECT, uri); }
  default RequestCtxBuilder patch(String uri) { return this.request(HttpMethod.PATCH, uri); }

  Future<Void> close();
}
