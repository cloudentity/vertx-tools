package com.cloudentity.tools.vertx.server.api;

import com.cloudentity.tools.vertx.conf.ConfVerticleDeploy;
import com.cloudentity.tools.vertx.test.VertxUnitTest;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.ext.unit.TestContext;

import java.io.IOException;

public class ApiServerTestBase extends VertxUnitTest {
  public Future<HttpClient> deployRoutes(String configPath) {
    return deployRoutes(configPath, null);
  }

  public Future<HttpClient> deployRoutes(String configPath, String verticleId) {
    HttpClient client = vertx().createHttpClient(new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(7774));

    return ConfVerticleDeploy.deployFileConfVerticle(vertx(), configPath)
    .compose(x -> ApiServerDeployer.deployServer(vertx(), verticleId))
    .map(x -> client);
  }

  public void shouldDeployWorkingRouteVerticle(TestContext ctx, String urlPath, String configPath) throws IOException {
    // given
    deployRoutes(configPath)
      .compose(client -> {
        // when
        Future promise = Future.future();
        client.get(urlPath)
        .handler(resp -> {
          // then
          ctx.assertTrue(resp.statusCode() == 201);
          ctx.assertEquals("test-header-value", resp.headers().get("test-header"));

          promise.complete();
        })
        .exceptionHandler(ex -> promise.fail(ex))
        .end();
        return promise;
      }).setHandler(ctx.asyncAssertSuccess());
  }
}
