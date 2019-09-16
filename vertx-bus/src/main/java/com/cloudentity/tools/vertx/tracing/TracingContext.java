package com.cloudentity.tools.vertx.tracing;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;
import io.opentracing.tag.Tags;
import com.cloudentity.tools.vertx.tracing.internals.JaegerTracing;
import com.cloudentity.tools.vertx.tracing.internals.MapTextMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * TracingContext is our wrapper for open tracing Span.
 */
public class TracingContext {
  private static final Logger log = LoggerFactory.getLogger(TracingContext.class);

  private final Span span;
  private MapTextMap contextMap;
  private String spanContextKey;

  public TracingContext(TracingManager tracing, Span span) {
    this.span = span;
    this.contextMap = new MapTextMap();
    this.spanContextKey = tracing.spanContextKey;
    tracing.tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, contextMap);
  }

  public static TracingContext of(TracingManager tracing, Span span) {
    return new TracingContext(tracing, span);
  }

  /**
   * Start tracing by creating a new span
   */
  public static TracingContext newSpan(TracingManager tracing, String operationName) {
    return of(tracing, tracing.tracer.buildSpan(operationName).ignoreActiveSpan().start());
  }

  public static TracingContext ofParent(TracingManager tracing, SpanContext parentContext, String operationName) {
    return of(tracing, tracing.tracer.buildSpan(operationName).asChildOf(parentContext).ignoreActiveSpan().start());
  }

  public static TracingContext ofParent(TracingManager tracing, Map<String, String> context, String operationName) {
    try {
      SpanContext ctx = tracing.tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapAdapter(context));
      return ofParent(tracing, ctx, operationName);
    } catch (Exception e) {
      log.error("Failed to create a span context from: " + context, e);
      return newSpan(tracing, operationName);
    }
  }

  /**
   * Create a dummy span for testing
   */
  public static TracingContext dummy() {
    return of(JaegerTracing.noTracing, JaegerTracing.noTracing.tracer.buildSpan("dummy").ignoreActiveSpan().start());
  }

  /**
   * Print the tracing context, example: x-traceId=1dasd x-ctx-some-key=some-value
   */
  public String printContext() {
    return contextMap.print();
  }

  /**
   * Log information that an exception occurred in the current span
   */
  public void logException(Throwable throwable) {
    logError(throwable);
  }

  /**
   * Log information that an error occurred in the current span
   */
  public void logError(Object error) {
    Tags.ERROR.set(span, Boolean.TRUE);
    Map<String, Object> log = new HashMap<>();
    log.put("event", Tags.ERROR.getKey());
    log.put("error.object", error.toString());
    span.log(log);
  }

  public void setTag(String key, String value) {
    span.setTag(key, value);
  }

  public void setOperationName(String name) {
    span.setOperationName(name);
  }

  /**
   * Start a new child span of current span
   */
  public TracingContext newChild(TracingManager tracing, String operationName) {
    Span childSpan = tracing.tracer.buildSpan(operationName)
      .asChildOf(span)
      .ignoreActiveSpan()
      .start();
    return of(tracing, childSpan);
  }

  /**
   * Get only the traceId from the span context.
   */
  public String getTraceId() {
    try {
      return contextMap.get(spanContextKey).split(":")[0];
    } catch (Exception e) {
      log.warn("Failed to get traceId", e);
      return "";
    }
  }

  /**
   * Iterates over span context and executes keyValueConsumer for each entry. Useful for mutable builders
   */
  public void consumeContext(BiConsumer<String, String> keyValueConsumer) {
    getSpanContextMap().iterator().forEachRemaining(e -> keyValueConsumer.accept(e.getKey(), e.getValue()));
  }

  /**
   * Folds over context. Needed for immutable builders.
   */
  public <A> A foldOverContext(A zero, BiFunction<Map.Entry<String, String>, A, A> aggregator) {
    for (Map.Entry<String, String> stringStringEntry : getSpanContextMap()) {
      zero = aggregator.apply(stringStringEntry, zero);
    }
    return zero;
  }

  /**
   * Get the span context (traceId:spanId:traceId:flags).
   */
  public String getSpanContext() {
    return contextMap.get(spanContextKey);
  }

  /**
   * Get all the key-values for the span context (traceId and baggage items)
   */
  public MapTextMap getSpanContextMap() {
    return contextMap;
  }

  /**
   * Use this to store external correlation id etc. Baggage items are propagated to the children contexts.
   */
  public void setBaggageItem(String key, String value) {
    span.setBaggageItem(key, value);
    contextMap.put(key, value);
  }

  public Span getSpan() {
    return span;
  }

  /**
   * Finish the current span
   */
  public void finish() {
    span.finish();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TracingContext that = (TracingContext) o;
    return Objects.equals(span, that.span) &&
      Objects.equals(contextMap, that.contextMap) &&
      Objects.equals(spanContextKey, that.spanContextKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(span, contextMap, spanContextKey);
  }

  @Override
  public String toString() {
    return "TracingContext{" +
      "span=" + span +
      ", contextMap=" + contextMap +
      ", spanContextKey='" + spanContextKey + '\'' +
      '}';
  }
}
