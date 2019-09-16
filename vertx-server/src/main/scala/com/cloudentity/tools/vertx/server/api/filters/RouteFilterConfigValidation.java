package com.cloudentity.tools.vertx.server.api.filters;

import java.util.Optional;

public class RouteFilterConfigValidation {
  private final boolean success;
  private final Optional<String> error;

  private RouteFilterConfigValidation(boolean success, Optional<String> error) {
    this.success = success;
    this.error = error;
  }

  public static RouteFilterConfigValidation success() {
    return new RouteFilterConfigValidation(true, Optional.empty());
  }

  public static RouteFilterConfigValidation failure(String errorMsg) {
    return new RouteFilterConfigValidation(false, Optional.ofNullable(errorMsg));
  }

  public boolean isSuccess() {
    return success;
  }

  public String getError() {
    return error.get();
  }
}
