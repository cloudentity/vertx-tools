package com.cloudentity.tools.vertx.server.api.handlers;

import com.cloudentity.tools.vertx.server.api.tracing.RoutingWithTracing;
import com.cloudentity.tools.vertx.tracing.LoggingWithTracing;
import com.cloudentity.tools.vertx.tracing.TracingContext;
import com.cloudentity.tools.vertx.tracing.TracingManager;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class AccessLogHandler implements Handler<RoutingContext> {
  private static final LoggingWithTracing log = LoggingWithTracing.getLogger(AccessLogHandler.class);
  private static final Marker accessMarker = MarkerFactory.getMarker("ACCESS");

  private TracingManager tracing;

  public AccessLogHandler(TracingManager tracing) {
    this.tracing = tracing;
  }

  public void handle(RoutingContext ctx) {
    RoutingWithTracing.getOrCreate(ctx, tracing);
    long timestamp = System.currentTimeMillis();
    ctx.addBodyEndHandler(v -> log(ctx, timestamp));
    ctx.next();
  }

  private void log(RoutingContext ctx, long timestamp) {
    String remoteClient = getClientAddress(ctx.request().remoteAddress());
    String xff = getRequestHeader(ctx.request(), "X-Forwarded-For");
    String referer = getRequestHeader(ctx.request(), "Referer");
    String agent = getRequestHeader(ctx.request(), "User-Agent");
    HttpVersion httpVersion = ctx.request().version();
    HttpMethod method = ctx.request().method();
    int status = ctx.response().getStatusCode();
    String uri = ctx.request().uri();
    long requestLength = getRequestLength(ctx);
    long responseLength = ctx.response().bytesWritten();
    TracingContext tracingCtx = RoutingWithTracing.getOrCreate(ctx, tracing);
    String accessLogMessage = String.format("[%s] [%s] %s [%s] %s %s %s %d %d %d %d[ms]",
        remoteClient, xff, referer, agent, formatHttpVersion(httpVersion),
        method, uri, requestLength, status, responseLength, System.currentTimeMillis() - timestamp);
    log.info(tracingCtx, accessMarker, accessLogMessage);
  }

  private long getRequestLength(RoutingContext ctx) {
    try {
      String length = ctx.request().getHeader("Content-Length");
      if (length != null) return Long.valueOf(length);
      else return 0;
    } catch (Exception ex) {
      return 0;
    }
  }

  private String getClientAddress(SocketAddress inetSocketAddress) {
    if (inetSocketAddress == null) {
      return "-";
    }
    return inetSocketAddress.host();
  }

  private String getRequestHeader(HttpServerRequest request, String key) {
    String val = request.getHeader(key);
    return val == null ? "-" : val;
  }

  private String formatHttpVersion(HttpVersion hv) {
    String versionFormatted = "-";
    switch (hv) {
      case HTTP_1_0:
        versionFormatted = "HTTP/1.0";
        break;
      case HTTP_1_1:
        versionFormatted = "HTTP/1.1";
        break;
      case HTTP_2:
        versionFormatted = "HTTP/2.0";
        break;
    }

    return versionFormatted;
  }
}