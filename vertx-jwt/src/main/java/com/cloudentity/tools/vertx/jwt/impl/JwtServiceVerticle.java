package com.cloudentity.tools.vertx.jwt.impl;

import io.orchis.tools.jwt.*;
import com.cloudentity.tools.vertx.bus.ServiceVerticle;
import com.cloudentity.tools.vertx.jwt.JwtVertxConfigurationException;
import com.cloudentity.tools.vertx.jwt.api.JwtService;
import io.vavr.control.Either;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

public class JwtServiceVerticle extends ServiceVerticle implements JwtService {

  private static final Logger log = LoggerFactory.getLogger(JwtServiceVerticle.class);

  private JwtConfig jwtConfig;
  private JwtGenerator jwtGenerator;

  public String verticleId() {
    return "jwt-service";
  }

  @Override
  protected void initService() {
    jwtConfig = readJwtConfiguration();
    jwtGenerator = new OrchisJwtGenerator();
  }

  private JwtConfig readJwtConfiguration() {
    JsonObject config = getConfig();
    String secret = readSecretFromConfiguration(config);
    String issuer = readIssuerFromConfiguration(config);
    return new JwtConfig(secret, issuer);
  }

  private String readSecretFromConfiguration(JsonObject config) {
    return readFromConfigurationOrThrow(config, "secret", new JwtVertxConfigurationException("Missing secret configuration"));
  }

  private String readIssuerFromConfiguration(JsonObject config) {
    return readFromConfigurationOrThrow(config, "issuer", new JwtVertxConfigurationException("Missing issuer configuration"));
  }

  private String readFromConfigurationOrThrow(JsonObject config, String propKey, JwtVertxConfigurationException ex) {
    Optional<String> prop = Optional.ofNullable(config.getString(propKey));
    prop.orElseThrow(() -> ex);
    return prop.get();
  }

  @Override
  public Future<Boolean> canDecode(String jwt) {
    Either<Throwable, JwtClaims> parseResult = parseJwt(jwt, jwtConfig);
    return Future.succeededFuture(handleParseResponse(parseResult));
  }

  @Override
  public Future<Either<Throwable, JwtClaims>> decode(String jwt) {
    return Future.succeededFuture(parseJwt(jwt, jwtConfig));
  }

  @Override
  public Future<Either<Throwable, String>> sign() {
    return Future.succeededFuture(jwtGenerator.sign(jwtConfig));
  }

  @Override
  public Future<Either<Throwable, String>> sign(Map<String, Object> claims) {
    return Future.succeededFuture(jwtGenerator.sign(jwtConfig, claims));
  }

  private Either<Throwable, JwtClaims> parseJwt(String jwt, JwtConfig jwtConfig) {
    return jwtGenerator.parse(jwt, jwtConfig);
  }

  private Boolean handleParseResponse(Either<Throwable, JwtClaims> parseResult) {
    Boolean canDecode = parseResult.isRight();
    if (parseResult.isLeft()) {
      Throwable left = parseResult.getLeft();
      log.error("Could not parse jwt. {}, caused by: ", left.getMessage(), left.getCause());
    } else {
      log.debug("Jwt issued by {} can be decoded", parseResult.get().getIss());
    }
    return canDecode;
  }
}
