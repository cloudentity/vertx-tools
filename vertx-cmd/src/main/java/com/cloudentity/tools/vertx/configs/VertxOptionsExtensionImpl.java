package com.cloudentity.tools.vertx.configs;

import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VertxOptionsExtensionImpl implements VertxOptionsExtension {
  private static final Logger log = LoggerFactory.getLogger(VertxOptionsExtensionImpl.class);
  
  @Override
  public VertxOptions extendVertxOptions(JsonObject conf, VertxOptions opts) {

    if (conf.getJsonObject("vertx") != null && conf.getJsonObject("vertx").getJsonObject("options") != null) {
      JsonObject ext = conf.getJsonObject("vertx").getJsonObject("options");
      VertxOptions extOpts = new VertxOptions(ext);


      if (ext.getValue("addressResolverOptions") != null) {
        opts.setAddressResolverOptions(extOpts.getAddressResolverOptions());
      }
      if (ext.getValue("blockedThreadCheckInterval") != null) {
        opts.setBlockedThreadCheckInterval(extOpts.getBlockedThreadCheckInterval());
      }
      if (ext.getValue("clusterHost") != null) {
        opts.setClusterHost(extOpts.getClusterHost());
      }
      if (ext.getValue("clusterPingInterval") != null) {
        opts.setClusterPingInterval(extOpts.getClusterPingInterval());
      }
      if (ext.getValue("clusterPingReplyInterval") != null) {
        opts.setClusterPingReplyInterval(extOpts.getClusterPingReplyInterval());
      }
      if (ext.getValue("clusterPort") != null) {
        opts.setClusterPort(extOpts.getClusterPort());
      }
      if (ext.getValue("clusterPublicHost") != null) {
        opts.setClusterPublicHost(extOpts.getClusterPublicHost());
      }
      if (ext.getValue("clusterPublicPort") != null) {
        opts.setClusterPublicPort(extOpts.getClusterPublicPort());
      }
      if (ext.getValue("clustered") != null) {
        opts.setClustered(extOpts.isClustered());
      }
      if (ext.getValue("eventBusOptions") != null) {
        opts.setEventBusOptions(extOpts.getEventBusOptions());
      }
      if (ext.getValue("eventLoopPoolSize") != null) {
        opts.setEventLoopPoolSize(extOpts.getEventLoopPoolSize());
      }
      if (ext.getValue("haEnabled") != null) {
        opts.setHAEnabled(extOpts.isHAEnabled());
      }
      if (ext.getValue("haGroup") != null) {
        opts.setHAGroup(extOpts.getHAGroup());
      }
      if (ext.getValue("internalBlockingPoolSize") != null) {
        opts.setInternalBlockingPoolSize(extOpts.getInternalBlockingPoolSize());
      }
      if (ext.getValue("maxEventLoopExecuteTime") != null) {
        opts.setMaxEventLoopExecuteTime(extOpts.getMaxEventLoopExecuteTime());
      }
      if (ext.getValue("maxWorkerExecuteTime") != null) {
        opts.setMaxWorkerExecuteTime(extOpts.getMaxWorkerExecuteTime());
      }
      if (ext.getValue("metricsOptions") != null) {

        // Deploy with embedded server: prometheus metrics will be automatically exposed,
        // independently from any other HTTP server defined
        //https://vertx.io/docs/vertx-micrometer-metrics/java/

        //TODO: Add flag to disable/enable
        MicrometerMetricsOptions options = new MicrometerMetricsOptions()
            .setPrometheusOptions(new VertxPrometheusOptions()
                .setStartEmbeddedServer(true)
                .setEmbeddedServerOptions(new HttpServerOptions().setPort(8881))
                .setEnabled(true))
            .setEnabled(true);

        log.warn("Enabling promethus metrics options with: " + options);
        opts.setMetricsOptions(options);

        //opts.setMetricsOptions(extOpts.getMetricsOptions());
      }
      if (ext.getValue("quorumSize") != null) {
        opts.setQuorumSize(extOpts.getQuorumSize());
      }
      if (ext.getValue("warningExceptionTime") != null) {
        opts.setWarningExceptionTime(extOpts.getWarningExceptionTime());
      }
      if (ext.getValue("workerPoolSize") != null) {
        opts.setWorkerPoolSize(extOpts.getWorkerPoolSize());
      }

      log.info("Extended Vertx options: " + opts.toString());
    }
    return opts;
  }
}
