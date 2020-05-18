package com.cloudentity.tools.vertx.tracing;

import com.cloudentity.tools.vertx.bus.VertxEndpointClient;
import com.cloudentity.tools.vertx.bus.ServiceVerticle;
import com.cloudentity.tools.vertx.conf.ConfService;
import com.cloudentity.tools.vertx.tracing.internals.JaegerTracing;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TracingVerticle extends ServiceVerticle implements TracingService {
  private static final Logger log = LoggerFactory.getLogger(TracingVerticle.class);

  private ConfService confService;
  private TracingManager tracing;

  private static boolean tracingEnabled;

  @Override
  public void start(Future<Void> start) {
    confService = VertxEndpointClient.make(vertx, ConfService.class);
    toFuture(super::start).compose(x -> JaegerTracing.getTracingConfiguration(confService)
      .compose(t -> {
        this.tracing = t;
        tracingEnabled = true;
        return Future.<Void>succeededFuture();
      })).setHandler(start);
  }

  public static boolean isTracingEnabled(Vertx vertx) {
    return tracingEnabled;
  }

  @Override
  public Future<TracingManager> getTracingManager() {
    return Future.succeededFuture(tracing);
  }

  @Override
  protected boolean tracingEnabled() {
    return false;
  }
}
