package com.cloudentity.tools.vertx.clusters;

import io.vertx.core.VertxOptions;

public interface ExternalCluster {

  public VertxOptions appendClusterOptions(Boolean isClustered, VertxOptions other);
}
