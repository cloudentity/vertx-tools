package com.cloudentity.tools.vertx.conf;

import java.util.Optional;

public class Ref {
  private final String originalRef;
  public final String path;
  public final Optional<String> valueType;
  public final Optional<String> defaultValue;
  public final boolean optional;

  private Ref(String originalRef, String path, Optional<String> valueType, Optional<String> defaultValue, boolean optional) {
    this.originalRef = originalRef;
    this.path = path;
    this.valueType = valueType;
    this.defaultValue = defaultValue;
    this.optional = optional;
  }

  public static Ref fromString(String reference) {
    String[] parts =
      reference.replace("\\:", "ESCAPED_COLON")
        .split(":", -1);

    String path = parts[0].replace("ESCAPED_COLON", ":");

    boolean optional = false;
    if (path.startsWith("?")) {
      optional = true;
      path = path.substring(1);
    }

    Optional<String> valueType = Optional.empty();
    Optional<String> defaultValue = Optional.empty();
    if (parts.length > 1) valueType = Optional.of(parts[1]);
    if (parts.length > 2) defaultValue = Optional.of(parts[2].replace("ESCAPED_COLON", ":"));

    return new Ref(reference, path, valueType, defaultValue, optional);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Ref ref = (Ref) o;

    if (path != null ? !path.equals(ref.path) : ref.path != null) return false;
    if (valueType != null ? !valueType.equals(ref.valueType) : ref.valueType != null) return false;
    return defaultValue != null ? defaultValue.equals(ref.defaultValue) : ref.defaultValue == null;
  }

  @Override
  public int hashCode() {
    int result = path != null ? path.hashCode() : 0;
    result = 31 * result + (valueType != null ? valueType.hashCode() : 0);
    result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return originalRef;
  }
}