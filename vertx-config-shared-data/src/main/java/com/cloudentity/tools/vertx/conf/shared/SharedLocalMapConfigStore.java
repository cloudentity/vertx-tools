package com.cloudentity.tools.vertx.conf.shared;

import io.vertx.config.spi.ConfigStore;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class SharedLocalMapConfigStore implements ConfigStore {

  private final String name;
  private final String key;

  public SharedLocalMapConfigStore(JsonObject config) {
    this.name = config.getString("name");
    this.key = config.getString("key");
  }

  @Override
  public void get(Handler<AsyncResult<Buffer>> handler) {
    JsonObject config = (JsonObject) Vertx.currentContext().owner().sharedData().getLocalMap(name).get(key);
    handler.handle(Future.succeededFuture(config.toBuffer()));
  }
}
