package com.cloudentity.tools.vertx.configs;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.lang.reflect.Field;
import java.util.*;

public class ConfigFactory {
  /**
   * Converts JsonObject to config POJO.
   * Enforces all POJO's fields to be not-null and not-primitive (in order to prevent defaulting the primitive and to detect it's missing).
   * Throws a RuntimeException if POJO has not-null or primitive values.
   */
  public static <T> T build(JsonObject from, Class<T> to) {
    T t = Json.mapper.convertValue(from, to);
    List<String> errors = collectPrimitiveOrNullFieldErrors(to, t);

    if (!errors.isEmpty()) {
      StringJoiner msg = new StringJoiner("\n   ");
      errors.forEach(error -> msg.add(error));
      throw new RuntimeException("\nCould not create `" + to.getName() + "`:\n[\n   " + msg.toString() + "\n]");
    }

    return t;
  }

  private static <T> List<String> collectPrimitiveOrNullFieldErrors(Class<T> to, T t) {
    List<String> errors = new ArrayList<>();

    for (Field f : collectFields(to)) {
      boolean accessible = f.isAccessible();
      f.setAccessible(true);

      if (f.getType().isPrimitive()) {
        errors.add("`" + f.getName() + "` is primitive - use wrapper class instead");
      }

      try {
        if (f.get(t) == null) {
          errors.add("`" + f.getName() + "` can't be null");
        }
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      } finally {
        f.setAccessible(accessible);
      }
    }

    return errors;
  }

  private static List<Field> collectFields(Class clazz) {
    if (clazz == null) return new ArrayList();
    List<Field> acc = new ArrayList(Arrays.asList(clazz.getDeclaredFields()));
    acc.addAll(collectFields(clazz.getSuperclass()));
    return acc;
  }
}
