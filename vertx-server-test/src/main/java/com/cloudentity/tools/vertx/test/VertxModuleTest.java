package com.cloudentity.tools.vertx.test;

import com.cloudentity.tools.vertx.conf.ConfVerticleDeploy;
import com.cloudentity.tools.vertx.futures.FutureUtils;
import com.cloudentity.tools.vertx.json.VertxJson;
import com.cloudentity.tools.vertx.registry.RegistryVerticle;
import com.cloudentity.tools.vertx.shutdown.ShutdownVerticle;
import com.cloudentity.tools.vertx.verticles.VertxDeploy;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(VertxUnitRunner.class)
abstract public class VertxModuleTest extends VertxUnitTest {
  @BeforeClass
  public static void initVertxModuleTest() {
    VertxJson.registerJsonObjectDeserializer();
  }

  /**
   * Return true if verticles you are testing depend on ShutdownVerticle. Otherwise false.
   *
   * If true ShutdownVerticle will be deployed before verticle registries.
   * @return flag controlling ShutdownVerticle deployment
   */
  protected boolean withShutdown() {
    return false;
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
   * Deploys configuration verticle with given multiple modules configuration and starts verticle registries.
   *
   * @param modules configuration modules to load
   * @param registries types of registries to deploy
   * @return
   */
  public Future<List<String>> deployModules(List<String> modules, String... registries) {
    return deployModules(modules, new JsonObject(), registries);
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
    return deployModules(Arrays.asList(module), extraConfig, registries);
  }

  /**
   * Deploys configuration verticle with given module configuration and extra configuration from file and starts verticle registries.
   *
   * @param module configuration module to load
   * @param configPath path to extra config json file
   * @param registries types of registries to deploy
   * @return
   */
  public Future<List<String>> deployModuleWithFileConfig(String module, String configPath, String... registries) {
    return deployModulesWithFileConfig(Arrays.asList(module), configPath, registries);
  }

  /**
   * Deploys configuration verticle with given multiple modules configuration and extra configuration from file and starts verticle registries.
   *
   * @param modules configuration modules to load
   * @param configPath path to extra config json file
   * @param registries types of registries to deploy
   * @return
   */
  public Future<List<String>> deployModulesWithFileConfig(List<String> modules, String configPath, String... registries) {
    try {
      JsonObject extraConfig = new JsonObject(new String(Files.readAllBytes(Paths.get(configPath))));
      return deployModules(modules, extraConfig, registries);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Deploys configuration verticle with given multiple modules and extra configuration and starts verticle registries.
   *
   * @param modules configuration module to load
   * @param extraConfig extra configuration object
   * @param registries types of registries to deploy
   * @return
   */
  public Future<List<String>> deployModules(List<String> modules, JsonObject extraConfig, String... registries) {
    vertx().sharedData().getLocalMap("module-test").put("config", extraConfig);

    JsonObject modulesConfig = new JsonObject().put("modules", new JsonArray(modules));
    JsonObject localMapExtraConfig = new JsonObject().put("name", "module-test").put("key", "config");

    JsonArray configStores =
      new JsonArray()
        .add(new JsonObject().put("type", "json").put("format", "json").put("config", modulesConfig))
        .add(new JsonObject().put("type", "shared-local-map").put("format", "json").put("config", localMapExtraConfig));

    return deployModulesWithConfigStores(configStores, registries);
  }

  /**
   * Deploys configuration verticle with given config stores and starts verticle registries.
   *
   * @param configStores config stores to load
   * @param registries types of registries to deploy
   * @return
   */
  public Future<List<String>> deployModulesWithConfigStores(JsonArray configStores, String... registries) {
    JsonObject metaConfig = new JsonObject().put("scanPeriod", 100).put("stores", configStores);

    return ConfVerticleDeploy.deployVerticleFromMetaConfig(vertx(), metaConfig)
      .compose(x -> {
        if (withShutdown()) return VertxDeploy.deploy(vertx(), new ShutdownVerticle());
        else return Future.succeededFuture();
      })
      .compose(x -> FutureUtils.sequence(Lists.newArrayList(registries).stream().map(r -> RegistryVerticle.deploy(vertx(), r)).collect(Collectors.toList())));
  }
}