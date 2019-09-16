package com.cloudentity.tools.vertx.conf.retriever;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigRetrieverConf {
  private Integer scanPeriod;
  private List<ConfigStoreConf> stores;

  public ConfigRetrieverConf() {

  }

  public ConfigRetrieverConf(Integer scanPeriod, List<ConfigStoreConf> stores) {
    this.scanPeriod = scanPeriod;
    this.stores = stores;
  }

  public Integer getScanPeriod() {
    return scanPeriod;
  }

  public List<ConfigStoreConf> getStores() {
    return stores;
  }
}
