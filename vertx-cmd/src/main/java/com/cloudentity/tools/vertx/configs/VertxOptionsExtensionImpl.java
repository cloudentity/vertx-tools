package com.cloudentity.tools.vertx.configs;

import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.micrometer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VertxOptionsExtensionImpl implements VertxOptionsExtension {
  private static final Logger log = LoggerFactory.getLogger(VertxOptionsExtensionImpl.class);
  
  @Override
  public VertxOptions extendVertxOptions(JsonObject conf, VertxOptions opts) {

    log.info("Extend Vertx options:  {} {}", conf, opts);

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
        opts.setMetricsOptions(micrometerMetricsOpts(extOpts));
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

  //projects requiring any of the metrics module should include the artifact as well as the configs
  //https://github.com/vert-x3/vertx-examples/blob/master/micrometer-metrics-examples/pom.xml
  private MicrometerMetricsOptions micrometerMetricsOpts(VertxOptions extOpts) {
    MicrometerMetricsOptions options = new MicrometerMetricsOptions();
    options.setEnabled(false);
    if(extOpts.getMetricsOptions().isEnabled()){
      options.setEnabled(true);
      configurePrometheusOpts(extOpts, options);
      configureJmxMetricsOpts(extOpts, options);
      configureInfluxDbOpts(extOpts, options);
    }
    return options;
  }

  private boolean isMetricsTypeEnabled(VertxOptions extOpts, String type) {
    return  extOpts.getMetricsOptions().toJson().getJsonObject(type,
        new JsonObject()).getBoolean("enabled", Boolean.valueOf(false));
  }

  private JsonObject metricsConfig(VertxOptions extOpts, String type) {
    return  extOpts.getMetricsOptions().toJson().getJsonObject(type,
        new JsonObject());
  }

  private void configurePrometheusOpts(VertxOptions extOpts, MicrometerMetricsOptions options) {
    if(isMetricsTypeEnabled(extOpts, "prometheus")) {
      options.setPrometheusOptions(new VertxPrometheusOptions(metricsConfig(extOpts, "prometheus")));
    }
  }

  private void configureJmxMetricsOpts(VertxOptions extOpts, MicrometerMetricsOptions options) {
    if(isMetricsTypeEnabled(extOpts, "jmx")) {
      options.setJmxMetricsOptions(new VertxJmxMetricsOptions(metricsConfig(extOpts, "jmx")));
    }
  }

  private void configureInfluxDbOpts(VertxOptions extOpts, MicrometerMetricsOptions options) {
    if(isMetricsTypeEnabled(extOpts, "influx")) {
      options.setInfluxDbOptions(new VertxInfluxDbOptions(metricsConfig(extOpts, "influx")));
    }
  }
}
