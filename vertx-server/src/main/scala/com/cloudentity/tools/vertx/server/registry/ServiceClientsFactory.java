package com.cloudentity.tools.vertx.server.registry;

import com.cloudentity.tools.vertx.registry.RegistryService;
import com.cloudentity.tools.vertx.bus.ServiceClientFactory;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Optional;

import static com.cloudentity.tools.vertx.futures.FutureUtils.withException;

public class ServiceClientsFactory {

  private static final Logger log = LoggerFactory.getLogger(ServiceClientsFactory.class);

  public static <T> Future<ServiceClientsRepository<T>> build(EventBus eventBus, String registryType, Class<T> clazz) {
    return build(eventBus, registryType, clazz, new DeliveryOptions());
  }

  /**
   * Produces ServiceClientsRepository with connectors of type @registryType and class @clazz
   */
  public static <T> Future<ServiceClientsRepository<T>> build(EventBus eventBus, String registryType, Class<T> clazz, DeliveryOptions ops) {
    ServiceClientsRepository<T> clients = new ServiceClientsRepository<>();
    RegistryService registry = ServiceClientFactory.make(eventBus, RegistryService.class, Optional.of(registryType), ops);
    return withException(registry.getVerticleIds().map(ids -> {
      ids.forEach(id -> clients.put(id, ServiceClientFactory.make(eventBus, clazz, Optional.of(id))));
      return clients;
    }), exception -> log.error("Failed to load connectors: {} {}", registryType, exception));
  }

}
