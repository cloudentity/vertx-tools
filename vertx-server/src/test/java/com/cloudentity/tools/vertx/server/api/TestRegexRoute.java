package com.cloudentity.tools.vertx.server.api;

import com.cloudentity.tools.vertx.server.api.routes.RouteVerticle;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRegexRoute extends RouteVerticle {

  private static final Logger log = LoggerFactory.getLogger(TestRegexRoute.class);

  @Override
  public void handle(RoutingContext ctx) {
    String pathRegexParam = ctx.pathParam("param0");
    log.debug("Param value of the path regex group: " +pathRegexParam);
    ctx.response().setStatusCode(201).putHeader("test-header", "test-header-value").end(pathRegexParam);
  }
}
