package com.cloudentity.tools.vertx.server.registry;

import com.cloudentity.tools.vertx.registry.RegistryVerticle;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
  Simple wrapper for Map<RegistryVerticle.VerticleId, T>
 */
public class ServiceClientsRepository<T> {

  private Map<RegistryVerticle.VerticleId, T> clients;

  public ServiceClientsRepository() {
    this.clients = new HashMap<>();
  }

  public boolean exists(String verticleId) {
    return clients.containsKey(new RegistryVerticle.VerticleId(verticleId));
  }

  public void put(String verticleId, T client) {
    clients.put(new RegistryVerticle.VerticleId(verticleId), client);
  }

  public T get(String verticleId) {
    return clients.get(new RegistryVerticle.VerticleId(verticleId));
  }

  public Set<String> keySet() {
    return clients.keySet().stream().map(e -> e.value()).collect(Collectors.toSet());
  }

}
