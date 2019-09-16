package com.cloudentity.tools.vertx.bus;

public class BusPayload {
  public final Object value;
  public final Throwable ex;

  public BusPayload(Object value) {
    this.value = value;
    this.ex = null;
  }

  public BusPayload(Object value, Throwable ex) {
    this.value = value;
    this.ex = ex;
  }

  public static BusPayload failed(Throwable ex) {
    return new BusPayload(null, ex);
  }

  public boolean isFailed() {
    return ex != null;
  }

  @Override
  public String toString() {
    return "BusPayload{" +
    "value=" + value +
    ", ex=" + ex +
    '}';
  }
}
