package com.cloudentity.tools.vertx.conf;

import com.cloudentity.tools.vertx.json.JsonExtractor;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfReference {
  private static final Logger log = LoggerFactory.getLogger(ConfReference.class);

  static Pattern confRefPattern = Pattern.compile("\\$ref\\:(.+)");
  static Pattern sysRefPattern = Pattern.compile("\\$sys\\:(.+)");
  static Pattern envRefPattern = Pattern.compile("\\$env\\:(.+)");

  static Map<String, Function<String, Object>> converters = new HashMap<>();
  static {
    converters.put("string", x -> x);
    converters.put("int", Integer::valueOf);
    converters.put("double", Double::valueOf);
    converters.put("boolean", Boolean::valueOf);
    converters.put("object", x -> new JsonObject(x));
    converters.put("array", x -> new JsonArray(x));
  }

  /**
   * Replaces configuration references in `conf`.
   * Uses values from `globalConf`, system properties and environment properties.
   *
   * {@see ConfReference#populateInnerRefs}, {@see ConfReference#populateSysRefs}, {@see ConfReference#populateEnvRefs}.
   */
  public static JsonObject populateRefs(JsonObject conf, JsonObject globalConf) {
    JsonObject envFallback = conf.getJsonObject("env", new JsonObject());
    JsonObject sysFallback = conf.getJsonObject("sys", new JsonObject());

    JsonObject withConfRefs = ConfReference.populateInnerRefs(conf, globalConf);
    JsonObject withSysRefs = ConfReference.populateSysRefs(withConfRefs, sysFallback);
    JsonObject withEnvRefs = ConfReference.populateEnvRefs(withSysRefs, envFallback);
    return withEnvRefs;
  }

  /**
   * Replaces references in `conf` with values from `globalConf`.
   * Reference is a string with following format: "$ref:{ref-path}:{default-value-type}:{default-value}"
   *
   * ref-path - comma-separated path to value in `globalConf`, if starts with '?' then the reference is optional (if ref is not optional and missing then warning will be logged)
   * default-value-type - optional, one of {"string", "int", "double", "boolean", "array", "object"}, default value is cast to the provided type
   * default-value - optional
   *
   * `conf` is not modified.
   */
  public static JsonObject populateInnerRefs(JsonObject conf, JsonObject globalConf) {
    List<Ref> refs = findRefs(conf, confRefPattern);

    if (refs.isEmpty()) {
      return conf;
    } else {
      List<ResolvedRef> resolvedRefs = resolveConfSubstitutions(globalConf, refs);
      String newJson = replaceRefs(conf, resolvedRefs, "$ref");
      return populateInnerRefs(new JsonObject(newJson), globalConf);
    }
  }

  private static List<ResolvedRef> resolveConfSubstitutions(JsonObject globalConf, List<Ref> refs) {
    List<ResolvedRef> resolvedRefs = new ArrayList<>();

    refs.forEach(ref -> {
      final Object defaultValue = defaultInnerReferenceValue(ref);
      Optional<Object> resolvedValue = JsonExtractor.resolveValue(globalConf, ref.path);
      if (resolvedValue.isPresent()) {
        resolvedRefs.add(new ResolvedRef(ref, resolvedValue.get()));
      } else {
        resolvedRefs.add(new ResolvedRef(ref, defaultValue));
      }
    });

    return resolvedRefs;
  }

  private static Object defaultInnerReferenceValue(Ref ref) {
    if (!ref.defaultValue.isPresent() && !ref.valueType.isPresent()) {
      return null;
    } else if (ref.defaultValue.isPresent() && ref.valueType.isPresent()) {
      return convertValue(ref.defaultValue.get(), ref.valueType.get(), ref, "$ref");
    } else {
      log.error("Invalid configuration reference format. Should be $ref:{ref-path} or $ref:{ref-path}:{default-value-type}:{default-value} was $ref:{}", ref);
      return null;
    }
  }

  /**
   * Replaces references in `conf` with values from System properties.
   * Reference is a string with following format: "$sys:{property-name}:{property-type}:{default-value}"
   *
   * property-name - argument for System.getProperty
   * property-type - one of {"string", "int", "double", "boolean", "array", "object"}, property value is cast to the provided type
   * default-value - optional
   *
   * `conf` is not modified.
   *
   * In case of array/object `property_type`, the sys value is expected to be JSON representation of the value.
   */
  public static JsonObject populateSysRefs(JsonObject conf, JsonObject sysFallback) {
    return populatePropertyRefs(conf, sysRefPattern, "$sys", resolveSysVariableWithFallback(sysFallback));
  }

  private static Function<String, String> resolveSysVariableWithFallback(JsonObject fallback) {
    return ref -> Optional.ofNullable(getVariableFallback(fallback, ref)).orElse(System.getProperty(ref));
  }

  /**
   * Replaces references in `conf` with values from ENV.
   * Reference is a string with following format: "$env:{property-name}:{property-type}:{default-value}"
   *
   * property-name - argument for System.getenv
   * property-type - one of {"string", "int", "double", "boolean", "array", "object"}, property value is cast to the provided type
   * default-value - optional
   *
   * `conf` is not modified.
   *
   * In case of array/object `property_type`, the sys value is expected to be JSON representation of the value.
   */
  public static JsonObject populateEnvRefs(JsonObject conf, JsonObject envFallback) {
    return populatePropertyRefs(conf, envRefPattern, "$env", resolveEnvVariableWithFallback(envFallback));
  }

  private static Function<String, String> resolveEnvVariableWithFallback(JsonObject fallback) {
    return ref -> Optional.ofNullable(getVariableFallback(fallback, ref)).orElse(System.getenv(ref));
  }

  private static String getVariableFallback(JsonObject fallback, String variableName) {
    return Optional.ofNullable(fallback.getValue(variableName)).map(Object::toString).orElse(null);
  }
  /**
   * Extracts references from `conf` matching `refPattern` and substitutes them with corresponding values from `resolveValue`.
   * Reference is a string with following format: "`refPrefix`:{property-name-or-value-pattern}:{property-type}:{default-value}"
   *
   * {property-name-or-value-expression} can be name of property (e.g. 'PATH') or an expression embedding name of the property in curly braces.
   * Given PATH=user and value expression '/apis/{PATH}' the resolved value is '/apis/user'.
   */
  private static JsonObject populatePropertyRefs(JsonObject conf, Pattern refPattern, String refPrefix, Function<String, String> resolveValue) {
    List<Ref> refs = findRefs(conf, refPattern);
    List<ResolvedRef> resolvedRefs = resolveRefs(refs, resolveValue, refPrefix);

    return new JsonObject(replaceRefs(conf, resolvedRefs, refPrefix));
  }

  private static List<ResolvedRef> resolveRefs(List<Ref> refs, Function<String, String> resolveValue, String refPrefix) {
    List<ResolvedRef> substitutions = new ArrayList<>();

    refs.forEach(ref -> {
      if (ref.valueType.isPresent()) {
        String propertyNameOrValueExpression = ref.path;
        String propertyType = ref.valueType.get();

        String defaultValue = ref.defaultValue.orElse(null);

        Object value = resolvePropertyValue(propertyNameOrValueExpression, propertyType, defaultValue, resolveValue, ref, refPrefix);
        substitutions.add(new ResolvedRef(ref, value));
      } else {
        log.error("Invalid configuration reference format. Should be {}:{property-name}:{property-type}:{default-value} was {}:{}", refPrefix, refPrefix, ref);
        substitutions.add(new ResolvedRef(ref, null));
      }
    });

    return substitutions;
  }

  private static Pattern propertyValueExpressionPattern = Pattern.compile(".*\\{([^}]+)}.*");

  private static Object resolvePropertyValue(String propertyNameOrValueExpression, String propertyType, String defaultValue, Function<String, String> resolveValue, Ref ref, String refPrefix) {
    Object value;

    Matcher matcher = propertyValueExpressionPattern.matcher(propertyNameOrValueExpression);
    if (matcher.matches()) {
      String propertyName = matcher.group(1);
      String propertyValue = Optional.ofNullable(resolveValue.apply(propertyName)).orElse(defaultValue);
      if (propertyValue != null) {
        value = propertyNameOrValueExpression.replace("{" + propertyName + "}", propertyValue);
      } else {
        value = null;
      }
    }
    else {
      value = Optional.ofNullable(resolveValue.apply(propertyNameOrValueExpression)).orElse(defaultValue);
    }

    Function<String,  Object> converter = converters.get(propertyType);

    if (value == null) {
      if (!ref.optional) {
        log.debug("Could not resolve configuration reference: {}:{}. Setting null value.", refPrefix, ref);
      }
      return null;
    } else if (converter != null) {
      return convertValue(value, propertyType, ref, refPrefix);
    } else {
      log.error("Unsupported value type '{}' in configuration reference: {}:{}", propertyType, refPrefix, ref);
      return null;
    }
  }

  private static Object convertValue(Object value, String valueType, Ref ref, String refPrefix) {
    Function<String,  Object> converter = converters.get(valueType);
    try {
      return converter.apply(value.toString());
    } catch (Exception ex) {
      log.error("Could not convert value in configuration reference: {}:{}.", refPrefix, ref);
      return null;
    }
  }

  private static String replaceRefs(JsonObject json, List<ResolvedRef> resolvedRefs, String refPrefix) {
    String result = json.toString();
    for (ResolvedRef ref : resolvedRefs) {
      Object value = ref.resolvedValue;

      if (value instanceof String) value = "\"" + value + "\"";

      String substitutionValue = Optional.ofNullable(value).map(v -> v.toString()).orElse("null");
      String escapedReference = escapeReference(ref); // we need to escape special JSON characters in the reference because we are replacing in encoded JSON object

      result = result.replace("\"" + refPrefix + ":" + escapedReference + "\"", substitutionValue);
    }
    return result;
  }

  private static String escapeReference(ResolvedRef ref) {
    String encoded = Json.encode(ref.ref.toString());
    return encoded.substring(1, encoded.length() - 1);
  }

  private static List<Ref> findRefs(JsonObject json, Pattern pattern) {
    return findRefsRec(json, pattern);
  }

  private static List<Ref> findRefsRec(Object json, Pattern pattern) {
    if (json == null) {
      return Arrays.asList();
    } else if (json instanceof String) {
      Matcher matcher = pattern.matcher((String) json);
      if (matcher.find()) {
        return Arrays.asList(Ref.fromString(matcher.group(1)));
      } else {
        return Arrays.asList();
      }
    } else if (json instanceof JsonObject) {
      return findRefsInCollection(((JsonObject) json).getMap().values(), pattern);
    } else if (json instanceof JsonArray) {
      return findRefsInCollection((JsonArray) json, pattern);
    } else if (json instanceof List) { // internal representation of JsonArray is List
      return findRefsInCollection((List) json, pattern);
    } else if (json instanceof Map) { // internal representation of JsonObject is Map
      return findRefsInCollection(((Map) json).values(), pattern);
    } else {
      return Arrays.asList();
    }
  }

  private static List<Ref> findRefsInCollection(Iterable<Object> jsons, Pattern pattern) {
    List<Ref> refs = new ArrayList<>();
    jsons.forEach(json -> refs.addAll(findRefsRec(json, pattern)));
    return refs;
  }

  /**
   * Returns map from environment variable referenced in the conf to resolved value.
   */
  public static List<ResolvedRef> findResolvedEnvRefs(JsonObject conf) {
    List<Ref> refs = findRefs(conf, envRefPattern);
    return resolveRefs(refs, resolveEnvVariableWithFallback(conf), "env");
  }
}
