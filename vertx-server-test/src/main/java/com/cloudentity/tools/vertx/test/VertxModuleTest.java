package com.cloudentity.tools.vertx.test;

import com.cloudentity.tools.vertx.conf.ConfVerticleDeploy;
import com.cloudentity.tools.vertx.futures.FutureUtils;
import com.cloudentity.tools.vertx.json.VertxJson;
import com.cloudentity.tools.vertx.registry.RegistryVerticle;
import com.google.common.collect.Lists;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(VertxUnitRunner.class)
abstract public class VertxModuleTest extends VertxUnitTest {
  @BeforeClass
  public static void initVertxModuleTest() {
    VertxJson.registerJsonObjectDeserializer();
  }

  /**
   * Deploys configuration verticle with given module configuration and starts verticle registries.
   *
   * @param module configuration module to load
   * @param registries types of registries to deploy
   * @return
   */
  public Future<List<String>> deployModule(String module, String... registries) {
    return deployModule(module, new JsonObject(), registries);
  }

  /**
   * Deploys configuration verticle with given module and extra configuration and starts verticle registries.
   *
   * @param module configuration module to load
   * @param extraConfig extra configuration object
   * @param registries types of registries to deploy
   * @return
   */
  public Future<List<String>> deployModule(String module, JsonObject extraConfig, String... registries) {
    JsonObject config = extraConfig.copy().put("modules", new JsonArray().add(module));
    JsonArray configStores = new JsonArray().add(new JsonObject().put("type", "json").put("format", "json").put("config", config));
    JsonObject metaConfig = new JsonObject().put("scanPeriod", 100).put("stores", configStores);

    return ConfVerticleDeploy.deployVerticleFromMetaConfig(vertx(), metaConfig)
      .compose(x -> FutureUtils.sequence(Lists.newArrayList(registries).stream().map(r -> RegistryVerticle.deploy(vertx(), r)).collect(Collectors.toList())));
  }

  /**
   * Deploys configuration verticle with given module configuration and extra configuration from file and starts verticle registries.
   *
   * @param module configuration module to load
   * @param registries types of registries to deploy
   * @return
   */
  public Future<List<String>> deployModuleWithFileConfig(String module, String configPath, String... registries) {
    try {
      JsonObject extraConfig = new JsonObject(new String(Files.readAllBytes(Paths.get(configPath))));
      return deployModule(module, extraConfig, registries);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}