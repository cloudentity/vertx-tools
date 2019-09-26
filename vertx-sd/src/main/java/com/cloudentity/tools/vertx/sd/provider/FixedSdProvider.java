package com.cloudentity.tools.vertx.sd.provider;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.cloudentity.tools.vertx.bus.ComponentVerticle;
import com.cloudentity.tools.vertx.bus.ServiceClientFactory;
import com.cloudentity.tools.vertx.sd.SdService;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.spi.ServiceImporter;
import io.vertx.servicediscovery.spi.ServicePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers in SdVerticle as ServiceImporter providing node records from configuration.
 *
 * Records configuration should be stored in ConfVerticle with key = {@link FixedSdProvider#VERTICLE_ID} and following structure:
 *
 * {
 *   "records": [
 *     {
 *       "name": "authz",
 *       "metadata": {
 *         "ID": "authz-1"
 *       },
 *       "location": {
 *         "host": "localhost",
 *         "port": 8080,
 *         "ssl": false,
 *         "root": "/authz"
 *       }
 *     }
 *   ]
 * }
 *
 * 'metadata' is optional, it is defaulted to "${name}:${location.host}:${location.port}"
 * 'location.root' is optional.
 */
public class FixedSdProvider extends ComponentVerticle {

  private static final Logger log = LoggerFactory.getLogger(FixedSdProvider.class);

  public static final String VERTICLE_ID = "fixed-sd-provider";

  public static class FixedServiceImporter implements ServiceImporter {
    private List<JsonObject> records;

    public FixedServiceImporter(List<JsonObject> records) {
      this.records = records;
    }

    @Override
    public void start(Vertx vertx, ServicePublisher publisher, JsonObject configuration, Promise<Void> promise) {
      CompositeFuture.all(records.stream().map(r -> publish(publisher, r)).collect(Collectors.toList()))
        .map(x -> (Void) null)
        .setHandler(promise);
    }

    private Future publish(ServicePublisher publisher, JsonObject record) {
      Future promise = Future.future();
      publisher.publish(new Record(record), promise);
      return promise;
    }
  }

  @Override
  public void start(Future promise) {
    toFuture(super::start)
      .compose(x -> {
        SdService sd = ServiceClientFactory.make(vertx.eventBus(), SdService.class);

        JsonObject conf = getConfig();
        List<JsonObject> records =
          Lists.newArrayList(
            conf.getJsonArray("records").stream()
              // unfortunately, sometimes JSON object in io.vertx.core.json.JsonArray is represented
              // as io.vertx.core.json.JsonObject and sometimes as Map
              .map(record -> {
                JsonObject obj;
                if (record instanceof java.util.Map)
                  obj = new JsonObject((java.util.Map) record);
                else obj = (JsonObject) record;

                return createDefaultMetadata(obj);
              }).iterator()
          ); // can't make it compile with Stream::collect

        return sd.registerServiceImporter(new FixedServiceImporter(records), new JsonObject());
      }).setHandler(promise);
  }

  private JsonObject createDefaultMetadata(JsonObject record) {
    if (record.getJsonObject("metadata") == null) {
      JsonObject location = record.getJsonObject("location", new JsonObject());
      String nodeId = String.format("%s:%s:%d", record.getString("name"), location.getString("host"), location.getInteger("port"));
      record.put("metadata", new JsonObject().put("ID", nodeId));
    }

    return record;
  }

  @Override
  public String verticleId() {
    return VERTICLE_ID;
  }
}
