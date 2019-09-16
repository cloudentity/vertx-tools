package com.cloudentity.tools.vertx.server.api;

import com.cloudentity.tools.api.errors.ApiError;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.List;

public class ResponseHelpers {

  public static final String APPLICATION_JSON = "application/json";
  public static final String CONTENT_TYPE = "content-type";

  public static void respondWithError(RoutingContext ctx, ApiError error) {
    List<String> fields = new ArrayList<>();
    fields.add("\"code\":\"" + error.getBody().getCode() + "\"");
    fields.add("\"message\":\"" + error.getBody().getMessage() + "\"");

    if (error.getBody().getDetails() != null) {
      if (error.getBody().isDetailsEncoded()) {
        fields.add("\"details\":" + error.getBody().getDetails().toString());
      } else {
        fields.add("\"details\":" + Json.encode(error.getBody().getDetails()));
      }
    }

    String body = "{" + String.join(",", fields) + "}";

    ctx.response().setStatusCode(error.getStatusCode()).putHeader(CONTENT_TYPE, APPLICATION_JSON).end(body);
  }

  public static void respondWithSuccessfulJsonBody(RoutingContext ctx, String body) {
    ctx.response().setStatusCode(200).putHeader(CONTENT_TYPE, APPLICATION_JSON).end(body);
  }
}
