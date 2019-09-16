package com.cloudentity.tools.vertx.conf;

public class ResolvedRef {
  public final Ref ref;
  public final Object resolvedValue;

  public ResolvedRef(Ref ref, Object resolvedValue) {
    this.ref = ref;
    this.resolvedValue = resolvedValue;
  }
}