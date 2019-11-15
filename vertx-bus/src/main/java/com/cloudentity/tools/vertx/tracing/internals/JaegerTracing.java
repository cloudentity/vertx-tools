package com.cloudentity.tools.vertx.tracing.internals;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.propagation.B3TextMapCodec;
import io.jaegertracing.internal.propagation.TextMapCodec;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.spi.Codec;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import com.cloudentity.tools.vertx.conf.ConfService;
import com.cloudentity.tools.vertx.tracing.TracingManager;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

public class JaegerTracing {
  private static final Logger log = LoggerFactory.getLogger(JaegerTracing.class);

  private static final String UBER_TRACE_ID = "uber-trace-id";
  private static final String UBER_BAGGAGE_PREFIX = "uberctx-";

  private static final String CLOUDENTITY_TRACE_ID = "x-trace-id";
  private static final String CLOUDENTITY_BAGGAGE_PREFIX = "x-ctx-";

  private static final String ZIPKIN_TRACE_ID = "X-B3-TraceId";
  private static final String ZIPKIN_BAGGAGE_PREFIX = "baggage-";

  /**
   * Creates a Jaeger Tracer using properties from global config json object "tracing" or return an dummy tracer
   * if this key does not exist.
   *
   * See available properties here https://github.com/jaegertracing/jaeger-client-java/blob/master/jaeger-core/README.md
   */
  public static Future<TracingManager> getTracingConfiguration(ConfService confService) {
    return confService.
      getConf("tracing")
      .map(JaegerTracing::getTracer)
      .recover(t -> {
        log.warn("No tracing configuration. Using noTracing");
        return Future.succeededFuture(noTracing);
      });
  }

  public static final TracingManager noTracing = buildNoTracing();

  private static TracingManager buildNoTracing() {
    JaegerTracer.Builder builder = new JaegerTracer.Builder("service")
      .withReporter(new DummyReporter())
      .withSampler(new ConstSampler(true));
    CodecConfig codecConfig = getCodec(new JsonObject().put("TRACE_ID", UBER_TRACE_ID).put("BAGGAGE_PREFIX", UBER_BAGGAGE_PREFIX));
    registerCodec(builder, codecConfig);

    return TracingManager.of(builder.build(), UBER_TRACE_ID, UBER_BAGGAGE_PREFIX);
  }


  public static TracingManager getTracer(JsonObject json) {
    if (Objects.isNull(json)) {
      log.warn("No tracing configuration. Using noTracing");
      return noTracing;
    }

    json.getMap().forEach((k, v) -> {
      if (v instanceof String) {
        System.setProperty(k, (String) v);
      }
    });

    Configuration config = Configuration.fromEnv();
    JaegerTracer.Builder builder = config.getTracerBuilder();
    CodecConfig codecConfig = getCodec(json);
    registerCodec(builder, codecConfig);

    return TracingManager.of(builder.build(), codecConfig.traceId, codecConfig.baggagePrefix);
  }

  private static void registerCodec(JaegerTracer.Builder builder, CodecConfig codecConfig) {
    builder.registerExtractor(Format.Builtin.HTTP_HEADERS, codecConfig.codec);
    builder.registerInjector(Format.Builtin.HTTP_HEADERS, codecConfig.codec);
    builder.registerExtractor(Format.Builtin.TEXT_MAP, codecConfig.codec);
    builder.registerInjector(Format.Builtin.TEXT_MAP, codecConfig.codec);
  }

  public static CodecConfig getCodec(JsonObject json) {
    String format = json.getString("FORMAT", "cloudentity");
    switch (format) {
      case "cloudentity":
        String traceId = Optional.ofNullable(json.getString("TRACE_ID")).orElse(CLOUDENTITY_TRACE_ID);
        String baggagePrefix = Optional.ofNullable(json.getString("BAGGAGE_PREFIX")).orElse(CLOUDENTITY_BAGGAGE_PREFIX);

        TextMapCodec codec = TextMapCodec.builder()
          .withSpanContextKey(traceId)
          .withBaggagePrefix(baggagePrefix)
          .withUrlEncoding(false)
          .build();

        return new CodecConfig(codec, traceId, baggagePrefix);

      case "jaeger":
        return new CodecConfig(new TextMapCodec(false), UBER_TRACE_ID, UBER_BAGGAGE_PREFIX);

      case "zipkin":
        return new CodecConfig(new B3TextMapCodec(), ZIPKIN_TRACE_ID, ZIPKIN_BAGGAGE_PREFIX);

      default:
        throw new IllegalArgumentException("Unknown tracing format " + format + ". Available formats: jaeger, zipkin");
    }
  }

  public static class CodecConfig {
    public Codec<TextMap> codec; // codec implementation
    public String traceId;       // trace id header name
    public String baggagePrefix; // header name prefix for baggage items

    public CodecConfig(Codec<TextMap> codec, String traceId, String baggagePrefix) {
      this.codec = codec;
      this.traceId = traceId;
      this.baggagePrefix = baggagePrefix;
    }

    @Override
    public String toString() {
      return "CodecConfig{" +
        "codec=" + codec +
        ", traceId='" + traceId + '\'' +
        ", baggagePrefix='" + baggagePrefix + '\'' +
        '}';
    }
  }
}
