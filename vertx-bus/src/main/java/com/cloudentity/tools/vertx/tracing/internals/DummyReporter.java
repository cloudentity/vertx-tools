package com.cloudentity.tools.vertx.tracing.internals;


import io.jaegertracing.internal.JaegerSpan;
import io.jaegertracing.spi.Reporter;

public class DummyReporter implements Reporter {
  @Override
  public void report(JaegerSpan jaegerSpan) {

  }

  @Override
  public void close() {

  }
}
