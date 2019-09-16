package com.cloudentity.tools.vertx.jwt.api;

import io.orchis.tools.jwt.JwtClaims;
import com.cloudentity.tools.vertx.bus.VertxEndpoint;
import io.vavr.control.Either;
import io.vertx.core.Future;

import java.util.Map;

public interface JwtService {

  @VertxEndpoint
  public Future<Boolean> canDecode(String jwt);

  @VertxEndpoint
  public Future<Either<Throwable, JwtClaims>> decode(String jwt);

  @VertxEndpoint
  public Future<Either<Throwable, String>> sign();

  @VertxEndpoint
  public Future<Either<Throwable, String>> sign(Map<String, Object> claims);

}
