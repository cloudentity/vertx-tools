package com.cloudentity.tools.vertx.server.api;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

public class ApiServerTest extends ApiServerTestBase {
  String configPath = "src/test/resources/api-server/conf.json";

  @Test
  public void shouldDeployWorkingJavaRouteVerticle(TestContext ctx) throws IOException {
    shouldDeployWorkingRouteVerticle(ctx, "/test", configPath);
  }

  @Test
  public void shouldDeployWorkingScalaRouteVerticle(TestContext ctx) throws IOException {
    shouldDeployWorkingRouteVerticle(ctx, "/scala-test", configPath);
  }

  @Test
  public void shouldEndRequestWith500WhenRouteVerticleThrowsException(TestContext ctx) throws IOException {
    // given
    deployRoutes()
      .compose(client -> {
        // when
        Future promise = Future.future();
        client.get("/exception")
          .handler(resp -> {
            // then
            ctx.assertTrue(resp.statusCode() == 500);
            promise.complete();
          })
          .exceptionHandler(ex -> promise.fail(ex))
          .end();
        return promise;
      }).setHandler(ctx.asyncAssertSuccess());
  }

  protected Future<HttpClient> deployRoutes() {
    return deployRoutes(configPath);
  }

  @Test
  public void shouldDeployExtraRoute(TestContext ctx) throws IOException {
    // given
    String path = "/extra-test";

    deployRoutes()
      .compose(client -> {
        // when
        Future promise = Future.future();
        client.get(path)
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

  @Test
  public void shouldReturn413IfBodyTooBig(TestContext ctx) {
    // given
    String bigBody = UUID.randomUUID().toString().replaceAll("\\.*", UUID.randomUUID().toString());

    deployRoutes()
    .compose(client -> {
      // when
      Future promise = Future.future();
      client.post("/limit")
        .handler(resp -> {
          // then
          ctx.assertTrue(resp.statusCode() == 413);
          promise.complete();
        })
        .exceptionHandler(ex -> promise.fail(ex))
        .end(bigBody);
      return promise;
    }).setHandler(ctx.asyncAssertSuccess());
  }

  @Test
  public void shouldApplyFilterAndExecuteHandlerIfFilterDidNotEndResponse(TestContext ctx) {
    // given
    deployRoutes()
    .compose(client -> {
      // when
      Future promise = Future.future();
      client.get("/passing-filter")
      .handler(resp -> {
        // then
        ctx.assertEquals(201, resp.statusCode());
        ctx.assertEquals("passing-filter", resp.getHeader("passing-filter"));
        promise.complete();
      })
      .exceptionHandler(ex -> promise.fail(ex))
      .end();
      return promise;
    }).setHandler(ctx.asyncAssertSuccess());
  }

  @Test
  public void shouldApplyFilterAndNotExecuteHandlerIfFilterEndedResponse(TestContext ctx) {
    // given
    deployRoutes()
    .compose(client -> {
      // when
      Future promise = Future.future();
      client.get("/aborting-filter")
      .handler(resp -> {
        // then
        ctx.assertEquals(400, resp.statusCode());
        ctx.assertEquals("aborting-filter", resp.getHeader("aborting-filter"));
        promise.complete();
      })
      .exceptionHandler(ex -> promise.fail(ex))
      .end();
      return promise;
    }).setHandler(ctx.asyncAssertSuccess());
  }

  @Test
  public void shouldPassRegexRoute(TestContext ctx) {
    String pathPrefix = "something";
    // given
    deployRoutes()
            .compose(client -> {
              // when
              Future promise = Future.future();
              client.get("/"+pathPrefix+"/regex")
                      .handler(resp -> {
                        // then
                        resp.bodyHandler(body ->{
                          ctx.assertTrue(resp.statusCode() == 201);
                          ctx.assertTrue(pathPrefix.equals(body.toString("UTF-8")));
                          promise.complete();

                        });
                      })
                      .exceptionHandler(ex -> promise.fail(ex))
                      .end();
              return promise;
            }).setHandler(ctx.asyncAssertSuccess());
  }
}
