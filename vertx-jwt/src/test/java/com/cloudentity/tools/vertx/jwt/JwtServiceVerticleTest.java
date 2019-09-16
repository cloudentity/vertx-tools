package com.cloudentity.tools.vertx.jwt;

import io.orchis.tools.jwt.JwtConfig;
import io.orchis.tools.jwt.JwtGenerator;
import io.orchis.tools.jwt.OrchisJwtGenerator;
import com.cloudentity.tools.vertx.jwt.api.JwtService;
import com.cloudentity.tools.vertx.jwt.impl.JwtServiceVerticle;
import com.cloudentity.tools.vertx.test.ServiceVerticleUnitTest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(VertxUnitRunner.class)
public class JwtServiceVerticleTest extends ServiceVerticleUnitTest<JwtServiceVerticle, JwtService> {

  private String passingJwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJ0ZXN0IiwiaWF0IjoxNTA3MDI2MDg4LCJleHAiOjQ2OTQyMzU2ODgsImF1ZCI6InJpc2stc2VydmljZSIsInN1YiI6InJpc2sifQ.M3dxx7r_vFm47BxCMAfJ_lfIGjGxzFVovsSQAgY3Xp4";
  private String failingJwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJ0ZXN0IiwiaWF0IjoxNTA3MDI2MDg4LCJleHAiOjQ2OTQyMzU2ODgsImF1ZCI6InJpc2stc2VydmljZSIsInN1YiI6InJpc2sifQ.9Ea7ueP8QWxa0fSxp7zgNk6pK5Wd7oLYL0e0S-Slqas";

  @Test
  public void whenCorrectConfigAndCorrectJwtThenSuccess(TestContext ctx) {
    deployVerticle("jwt-service", passingConfig())
      .compose(v -> client().canDecode(passingJwt))
      .map(r -> {
        ctx.assertTrue(r);
        return null;
      })
      .setHandler(ctx.asyncAssertSuccess());
  }

  @Test
  public void whenPassingConfigAndFailingJwtThenFail(TestContext ctx) {
    deployVerticle("jwt-service", passingConfig())
      .compose(v -> client().canDecode(failingJwt))
      .map(r -> {
        ctx.assertFalse(r);
        return null;
      })
      .setHandler(ctx.asyncAssertSuccess());
  }

  @Test
  public void whenFailingConfigAndPassingJwtThenFail(TestContext ctx) {
    deployVerticle("jwt-service", failingConfig())
      .compose(v -> client().canDecode(passingJwt))
      .map(r -> {
        ctx.assertFalse(r);
        return null;
      })
      .setHandler(ctx.asyncAssertSuccess());
  }

  @Test
  public void decodeJwtWithClaims(TestContext ctx) {
    JwtGenerator generator = new OrchisJwtGenerator();
    JwtConfig config = new JwtConfig("53cr37", "test");
    Map<String, Object> claims = new HashMap<>();
    String userUuid = "123-456-789";
    String customer = "syntegrity";
    claims.put("uuid", userUuid);
    claims.put("customer", customer);
    String jwt = generator.sign(config, claims).get();

    deployVerticle("jwt-service", passingConfig())
      .compose(v -> client().decode(jwt))
      .map(r -> {
        ctx.assertTrue(r.isRight());
        Map<String, Object> parsedClaims = r.get().getOrchisClaims().getAll();
        ctx.assertEquals(userUuid, parsedClaims.get("uuid"));
        ctx.assertEquals(customer, parsedClaims.get("customer"));
        return null;
      })
      .setHandler(ctx.asyncAssertSuccess());
  }

  @Test
  public void decodeJwtWithInvalidSecret(TestContext ctx) {
    deployVerticle("jwt-service", passingConfig())
      .compose(v -> client().decode(failingJwt))
      .map(r -> {
        ctx.assertTrue(r.isLeft());
        return null;
      })
      .setHandler(ctx.asyncAssertSuccess());
  }

  @Test
  public void signEmptyJwt(TestContext ctx) {
    JwtGenerator g = new OrchisJwtGenerator();

    deployVerticle("jwt-service", passingConfig())
      .compose(v -> client().sign())
      .map(r -> {
        ctx.assertTrue(r.isRight());
        ctx.assertFalse(r.get().isEmpty());
        return null;
      })
    .setHandler(ctx.asyncAssertSuccess());
  }

  @Test
  public void signJwt(TestContext ctx) {
    JwtGenerator g = new OrchisJwtGenerator();
    Map<String, Object> claims = new HashMap<>();
    claims.put("uuid", "72735b09-614c-49bd-b3d9-41ebcc2b2d91");

    deployVerticle("jwt-service", passingConfig())
      .compose(v -> client().sign(claims))
      .map(r -> {
        ctx.assertTrue(r.isRight());
        ctx.assertFalse(r.get().isEmpty());
        return null;
      })
    .setHandler(ctx.asyncAssertSuccess());
  }

  private JsonObject passingConfig() {
    JsonObject conf = new JsonObject();
    conf.put("secret", "53cr37");
    conf.put("issuer", "test");
    return conf;
  }

  private JsonObject failingConfig() {
    JsonObject conf = passingConfig();
    conf.put("secret", "fail");
    return conf;
  }

  @Override
  protected JwtServiceVerticle createVerticle() {
    return new JwtServiceVerticle();
  }
}


