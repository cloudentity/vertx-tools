package com.cloudentity.tools.vertx.bus;

import java.lang.annotation.*;

@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VertxEndpoint {
  String address() default DERIVE_ADDRESS; // if address is not set explicitly then it will be derived from Method full name
  String DERIVE_ADDRESS = "";
}
