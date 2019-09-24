package com.cloudentity.tools.vertx.server.api.errors;

public class Errors {
  public static ApiError withDetails(ApiError error, Object details) {
    return ApiError.withDetails(error.getStatusCode(), error.getBody().getCode(), error.getBody().getMessage(), details);
  }
}