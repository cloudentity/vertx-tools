package com.cloudentity.tools.vertx.server.api.errors;

public class ApiError {

  private int statusCode;
  private ApiErrorBody body;

  private ApiError(int statusCode, ApiErrorBody body) {
    this.statusCode = statusCode;
    this.body = body;
  }

  public static ApiError with(int statusCode, String code, String message) {
    return new ApiError(statusCode, new ApiErrorBody(code, message));
  }

  public static ApiError withDetails(int statusCode, String code, String message, Object details) {
    return new ApiError(statusCode, new ApiErrorBody(code, message, details));
  }

  public static ApiError withEncodedDetails(int statusCode, String code, String message, String details) {
    return new ApiError(statusCode, new ApiErrorBody(code, message, details, true));
  }

  public ApiErrorBody getBody() {
    return body;
  }

  public int getStatusCode() {
    return statusCode;
  }

  @Override
  public String toString() {
    return "ApiError{" +
      "body=" + body +
      ", statusCode=" + statusCode +
      '}';
  }
}
