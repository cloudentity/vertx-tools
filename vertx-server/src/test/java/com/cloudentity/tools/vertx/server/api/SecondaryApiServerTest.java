package com.cloudentity.tools.vertx.server.api;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;

public class SecondaryApiServerTest extends ApiServerTest {
  String configPath = "src/test/resources/api-server/secondary-conf.json";

  protected Future<HttpClient> deployRoutes() {
    return deployRoutes(configPath, "secondaryApiServer");
  }
}
