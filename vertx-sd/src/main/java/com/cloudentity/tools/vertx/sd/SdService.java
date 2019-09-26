package com.cloudentity.tools.vertx.sd;

import java.util.List;

import com.cloudentity.tools.vertx.bus.VertxEndpoint;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.spi.ServiceImporter;

public interface SdService {
  @VertxEndpoint(address = "sd.discover")
  Future<List<Record>> discover();

  @VertxEndpoint(address = "sd.discover.by-name")
  Future<List<Record>> discover(String serviceName);

  @VertxEndpoint(address = "sd.register.service-importer")
  Future<Void> registerServiceImporter(ServiceImporter importer, JsonObject conf);
}
