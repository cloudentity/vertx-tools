package com.cloudentity.tools.vertx.conf.fixed;

import io.vertx.config.ConfigChange;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;

import java.util.function.Function;

public class FixedConfigRetriever implements ConfigRetriever {
  private JsonObject config;

  public FixedConfigRetriever(JsonObject config) {
    this.config = config;
  }

  @Override
  public void getConfig(Handler<AsyncResult<JsonObject>> completionHandler) {
    completionHandler.handle(Future.succeededFuture(config));
  }

  @Override
  public void close() {

  }

  @Override
  public JsonObject getCachedConfig() {
    return config;
  }

  @Override
  public void listen(Handler<ConfigChange> listener) {

  }

  @Override
  public ConfigRetriever setBeforeScanHandler(Handler<Void> handler) {
    return this;
  }

  @Override
  public ConfigRetriever setConfigurationProcessor(Function<JsonObject, JsonObject> function) {
    return this;
  }

  @Override
  public ReadStream<JsonObject> configStream() {
    return null;
  }
}