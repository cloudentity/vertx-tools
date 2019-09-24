package com.cloudentity.tools.vertx.server.api.errors;

import java.util.Objects;

public class ApiErrorBody {


  private String code;
  private String message;
  private Object details;
  private boolean detailsEncoded;

  public ApiErrorBody() {
  }

  public ApiErrorBody(String code, String message) {
    this.code = code;
    this.message = message;
    this.detailsEncoded = false;
  }

  public ApiErrorBody(String code, String message, Object details) {
    this.code = code;
    this.message = message;
    this.details = details;
    this.detailsEncoded = false;
  }

  public ApiErrorBody(String code, String message, Object details, boolean detailsEncoded) {
    this.code = code;
    this.message = message;
    this.details = details;
    this.detailsEncoded = detailsEncoded;
  }

  public String getCode() {
    return code;
  }

  public Object getDetails() {
    return details;
  }

  public String getMessage() {
    return message;
  }

  public boolean isDetailsEncoded() {
    return detailsEncoded;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ApiErrorBody that = (ApiErrorBody) o;
    return detailsEncoded == that.detailsEncoded &&
      Objects.equals(code, that.code) &&
      Objects.equals(message, that.message) &&
      Objects.equals(details, that.details);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, message, details, detailsEncoded);
  }

  @Override
  public String toString() {
    return "ApiErrorBody{" +
      "code='" + code + '\'' +
      ", message='" + message + '\'' +
      ", details=" + details +
      ", detailsEncoded=" + detailsEncoded +
      '}';
  }
}