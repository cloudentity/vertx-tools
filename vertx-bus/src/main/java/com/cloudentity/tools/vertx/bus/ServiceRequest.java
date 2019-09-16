package com.cloudentity.tools.vertx.bus;

import java.util.List;

public class ServiceRequest {
  public final List<Object> values;

  public ServiceRequest(List<Object> values) {
    this.values = values;
  }

  @Override
  public String toString() {
    return "ServiceRequest{" +
    "values=" + values +
    '}';
  }
}
