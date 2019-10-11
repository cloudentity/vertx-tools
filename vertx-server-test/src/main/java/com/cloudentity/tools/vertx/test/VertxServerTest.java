package com.cloudentity.tools.vertx.test;

import com.cloudentity.tools.vertx.conf.ConfReference;
import com.cloudentity.tools.vertx.conf.retriever.ConfigRetrieverConf;
import com.cloudentity.tools.vertx.configs.ConfigFactory;
import com.cloudentity.tools.vertx.json.VertxJson;
import com.cloudentity.tools.vertx.shutdown.ShutdownVerticle;
import com.cloudentity.tools.vertx.verticles.VertxDeploy;
import io.restassured.RestAssured;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@RunWith(VertxUnitRunner.class)
abstract public class VertxServerTest {

  private static final Logger log = LoggerFactory.getLogger(VertxServerTest.class);

  public VertxServerTest() {
    System.setProperty(ShutdownVerticle.DISABLE_EXIT_SYSTEM_PROPERTY_NAME, "true");
  }

  protected String getMetaConfPath() {
    return System.getProperty("server.conf.path", "src/test/resources/meta-config-test.json");
  }

  protected abstract String getMainVerticle();

  protected void cleanup() {
  }

  protected Vertx vertx;

  public Vertx getVertx() {
    return vertx;
  }

  @Before
  public void setUp(TestContext context) throws Exception {
    JsonObject metaConf = getMetaConf();
    configureRestAssured(metaConf);
    start(context, metaConf);
  }

  private void start(TestContext context, JsonObject serverConf) {
    vertx = Vertx.vertx();
    cleanup();
    startApp(serverConf).setHandler(context.asyncAssertSuccess());
  }

  @After
  public void tearDown(TestContext context) throws Exception {
    vertx.close(context.asyncAssertSuccess());
  }

  private Future<String> startApp(JsonObject config) {
    return VertxDeploy.deploy(vertx, getMainVerticle(), new DeploymentOptions().setConfig(config));
  }

  protected JsonObject getMetaConf() throws IOException {
    String configPath = getMetaConfPath();
    log.debug("Load config from: {}", configPath);
    return new JsonObject(new String(Files.readAllBytes(Paths.get(configPath))));
  }

  protected void configureRestAssured(JsonObject metaConfJson) throws IOException {
    VertxJson.registerJsonObjectDeserializer();
    VertxJson.configureJsonMapper();

    ConfigRetrieverConf metaConf = ConfigFactory.build(metaConfJson, ConfigRetrieverConf.class);
    String configPath = metaConf.getStores().get(0).getConfig().getString("path");

    JsonObject apiServer = new JsonObject(new String(Files.readAllBytes(Paths.get(configPath)))).getJsonObject("apiServer");
    Integer port = readPort(apiServer.getJsonObject("http"));
    log.debug("Configure rest assured, port: {}", port);
    RestAssured.reset();
    RestAssured.baseURI = "http://127.0.0.1";
    RestAssured.port = port;
  }

  private Integer readPort(JsonObject httpConf) {
    Object value = httpConf.getValue("port");
    if (value instanceof Integer) {
      return (Integer) value;
    } else {
      return ConfReference.populateRefs(httpConf, httpConf).getInteger("port");
    }
  }
}
