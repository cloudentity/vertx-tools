package com.cloudentity.tools.vertx.server.api.routes.impl;

import com.cloudentity.tools.api.errors.ApiError;
import io.orchis.tools.jwt.JwtClaims;
import io.orchis.tools.jwt.OrchisClaims;
import com.cloudentity.tools.vertx.jwt.api.JwtService;
import com.cloudentity.tools.vertx.server.api.ResponseHelpers;
import com.cloudentity.tools.vertx.server.api.handlers.AccessLogHandler;
import com.cloudentity.tools.vertx.server.api.routes.RouteVerticle;
import com.cloudentity.tools.vertx.server.api.tracing.RoutingWithTracing;
import com.cloudentity.tools.vertx.tracing.LoggingWithTracing;
import com.cloudentity.tools.vertx.tracing.TracingContext;
import io.vavr.control.Either;
import io.vertx.core.*;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class JwtFilter extends RouteVerticle {

  public static final String JWT_CONTENT_KEY = "jwtContent";
  public static final String JWT_CLAIMS_KEY = "jwtClaims";

  private static final LoggingWithTracing log = LoggingWithTracing.getLogger(AccessLogHandler.class);

  private JwtService jwtClient;

  @Override
  protected void initService() {
    jwtClient = createClient(JwtService.class);
  }

  @Override
  protected void handle(RoutingContext ctx) {
    TracingContext tracingContext = RoutingWithTracing.getOrCreate(ctx, getTracing());
    log.debug(tracingContext, "Handling Jwt token");
    Optional<String> jwt = jwtTokenFromAuthorizationHeader(ctx.request().headers());
    if (jwt.isPresent()) {
      log.debug(tracingContext, "Jwt token found");
      jwtClient.decode(jwt.get()).setHandler(respondWithDecodeResult(ctx));
    } else {
      log.warn(tracingContext, "Jwt token not found in authorization header");
      tracingContext.logError("Jwt token not found");
      unauthorized(ctx);
    }
  }

  private Optional<String> jwtTokenFromAuthorizationHeader(MultiMap headers) {
    String header = headers.get("Authorization");
    if (Objects.isNull(header) || header.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(header.replace("Bearer", "").replace("bearer", "").trim());
  }

  private Handler<AsyncResult<Either<Throwable, JwtClaims>>> respondWithDecodeResult(RoutingContext ctx) {
    return decodeEvent -> {
      TracingContext tracingContext = RoutingWithTracing.getOrCreate(ctx, getTracing());
      if (decodeEvent.succeeded()) {
        if (decodeEvent.result().isRight()) {
          OrchisClaims orchisClaims = decodeEvent.result().get().getOrchisClaims();
          ctx.put(JWT_CLAIMS_KEY, orchisClaims);
          Map<String, Object> jwtContent = orchisClaims.getAll();
          log.debug(tracingContext, "Jwt decoded successfully, claims: {}", jwtContent);
          jwtContent.forEach((key, value) -> ctx.data().put(key, value));
          tracingContext.setTag("jwt.claims", Json.encode(jwtContent));
          ctx.put(JWT_CONTENT_KEY, jwtContent);
          ctx.next();
        } else {
          log.error(tracingContext, "Failed to decode jwt: {}", decodeEvent.result().getLeft());
          tracingContext.logError(decodeEvent.result().getLeft());
          unauthorized(ctx);
        }
      } else {
        log.error(tracingContext, "Decode event failed, {}", decodeEvent.cause().getMessage());
        tracingContext.logError(decodeEvent.cause().getMessage());
        unauthorized(ctx);
      }
    };
  }

  private void unauthorized(RoutingContext ctx) {
    ResponseHelpers.respondWithError(ctx, ApiError.with(500, "InvalidInternalJwt", ""));
  }
}
