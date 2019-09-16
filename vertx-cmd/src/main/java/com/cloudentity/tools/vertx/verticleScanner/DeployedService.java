package com.cloudentity.tools.vertx.verticleScanner;

public class DeployedService {

  private String deploymentId;
  private String configHash;

  private DeployedService(String deploymentId, String configHash) {
    this.configHash = configHash;
    this.deploymentId = deploymentId;
  }

  public static DeployedService of(String deploymentId, String configHash) {
    return new DeployedService(deploymentId, configHash);
  }

  public String getConfigHash() {
    return configHash;
  }

  public String getDeploymentId() {
    return deploymentId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DeployedService that = (DeployedService) o;

    if (deploymentId != null ? !deploymentId.equals(that.deploymentId) : that.deploymentId != null) return false;
    return configHash != null ? configHash.equals(that.configHash) : that.configHash == null;

  }

  @Override
  public int hashCode() {
    int result = deploymentId != null ? deploymentId.hashCode() : 0;
    result = 31 * result + (configHash != null ? configHash.hashCode() : 0);
    return result;
  }
}
