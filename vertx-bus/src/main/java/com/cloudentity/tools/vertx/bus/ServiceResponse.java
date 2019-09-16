package com.cloudentity.tools.vertx.bus;

public class ServiceResponse {
  public final Object value;

  public ServiceResponse(Object value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "ServiceResponse{" +
    "value=" + value +
    '}';
  }
}
