package com.cloudentity.tools.vertx.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;

import java.util.function.Consumer;

public class LoggingWithTracing {
  public static LoggingWithTracing getLogger(Class<?> clazz) {
    return new LoggingWithTracing(LoggerFactory.getLogger(clazz));
  }

  public static LoggingWithTracing getLogger(String name) {
    return new LoggingWithTracing(LoggerFactory.getLogger(name));
  }

  private final Logger log;

  public LoggingWithTracing(Logger log) {
    this.log = log;
  }

  private void logIf(TracingContext ctx, Boolean condition, Consumer<Void> action) {
    if (condition) {
      ctx.consumeContext(MDC::put);
      action.accept(null);
      ctx.consumeContext((k, v) -> MDC.remove(k));
    }
  }

  public String getName() {
    return log.getName();
  }

  public boolean isTraceEnabled() {
    return log.isTraceEnabled();
  }

  public void trace(TracingContext ctx, String msg) {
    logIf(ctx, isTraceEnabled(), (n) -> log.trace(msg));
  }

  public void trace(TracingContext ctx, String var1, Object var2) {
    logIf(ctx, isTraceEnabled(), (n) -> log.trace(var1, var2));
  }

  public void trace(TracingContext ctx, String var1, Object var2, Object var3) {
    logIf(ctx, isTraceEnabled(), (n) -> log.trace(var1, var2, var3));
  }

  public void trace(TracingContext ctx, String var1, Object... var2) {
    logIf(ctx, isTraceEnabled(), (n) -> log.trace(var1, var2));
  }

  public void trace(TracingContext ctx, String var1, Throwable var2) {
    logIf(ctx, isTraceEnabled(), (n) -> log.trace(var1, var2));
  }

  public boolean isTraceEnabled(Marker m) {
    return log.isTraceEnabled(m);
  }

  public void trace(TracingContext ctx, Marker var1, String var2) {
    logIf(ctx, isTraceEnabled(), (n) -> log.trace(var1, var2));
  }

  public void trace(TracingContext ctx, Marker var1, String var2, Object var3) {
    logIf(ctx, isTraceEnabled(), (n) -> log.trace(var1, var2, var3));
  }

  public void trace(TracingContext ctx, Marker var1, String var2, Object var3, Object var4) {
    logIf(ctx, isTraceEnabled(), (n) -> log.trace(var1, var2, var3, var4));
  }

  public void trace(TracingContext ctx, Marker var1, String var2, Object... var3) {
    logIf(ctx, isTraceEnabled(), (n) -> log.trace(var1, var2, var3));
  }

  public void trace(TracingContext ctx, Marker var1, String var2, Throwable var3) {
    logIf(ctx, isTraceEnabled(), (n) -> log.trace(var1, var2, var3));
  }

  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }

  public void debug(TracingContext ctx, String var1) {
    logIf(ctx, isDebugEnabled(), (n) -> log.debug(var1));
  }

  public void debug(TracingContext ctx, String var1, Object var2) {
    logIf(ctx, isDebugEnabled(), (n) -> log.debug(var1, var2));
  }

  public void debug(TracingContext ctx, String var1, Object var2, Object var3) {
    logIf(ctx, isDebugEnabled(), (n) -> log.debug(var1, var2, var3));
  }

  public void debug(TracingContext ctx, String var1, Object... var2) {
    logIf(ctx, isDebugEnabled(), (n) -> log.debug(var1, var2));
  }

  public void debug(TracingContext ctx, String var1, Throwable var2) {
    logIf(ctx, isDebugEnabled(), (n) -> log.debug(var1, var2));
  }

  public boolean isDebugEnabled(Marker var1) {
    return isDebugEnabled(var1);
  }

  public void debug(TracingContext ctx, Marker var1, String var2) {
    logIf(ctx, isDebugEnabled(), (n) -> log.debug(var1, var2));
  }

  public void debug(TracingContext ctx, Marker var1, String var2, Object var3) {
    logIf(ctx, isDebugEnabled(), (n) -> log.debug(var1, var2, var3));
  }

  public void debug(TracingContext ctx, Marker var1, String var2, Object var3, Object var4) {
    logIf(ctx, isDebugEnabled(), (n) -> log.debug(var1, var2, var3, var4));
  }

  public void debug(TracingContext ctx, Marker var1, String var2, Object... var3) {
    logIf(ctx, isDebugEnabled(), (n) -> log.debug(var1, var2, var3));
  }

  public void debug(TracingContext ctx, Marker var1, String var2, Throwable var3) {
    logIf(ctx, isDebugEnabled(), (n) -> log.debug(var1, var2, var3));
  }

  public boolean isInfoEnabled() {
    return log.isInfoEnabled();
  }

  public void info(TracingContext ctx, String var1) {
    logIf(ctx, isInfoEnabled(), (n) -> log.info(var1));
  }

  public void info(TracingContext ctx, String var1, Object var2) {
    logIf(ctx, isInfoEnabled(), (n) -> log.info(var1, var2));
  }

  public void info(TracingContext ctx, String var1, Object var2, Object var3) {
    logIf(ctx, isInfoEnabled(), (n) -> log.info(var1, var2, var3));
  }

  public void info(TracingContext ctx, String var1, Object... var2) {
    logIf(ctx, isInfoEnabled(), (n) -> log.info(var1, var2));
  }

  public void info(TracingContext ctx, String var1, Throwable var2) {
    logIf(ctx, isInfoEnabled(), (n) -> log.info(var1, var2));
  }

  public boolean isInfoEnabled(Marker var1) {
    return log.isInfoEnabled(var1);
  }

  public void info(TracingContext ctx, Marker var1, String var2) {
    logIf(ctx, isInfoEnabled(), (n) -> log.info(var1, var2));
  }

  public void info(TracingContext ctx, Marker var1, String var2, Object var3) {
    logIf(ctx, isInfoEnabled(), (n) -> log.info(var1, var2, var3));
  }

  public void info(TracingContext ctx, Marker var1, String var2, Object var3, Object var4) {
    logIf(ctx, isInfoEnabled(), (n) -> log.info(var1, var2, var3, var4));
  }

  public void info(TracingContext ctx, Marker var1, String var2, Object... var3) {
    logIf(ctx, isInfoEnabled(), (n) -> log.info(var1, var2, var3));
  }

  public void info(TracingContext ctx, Marker var1, String var2, Throwable var3) {
    logIf(ctx, isInfoEnabled(), (n) -> log.info(var1, var2, var3));
  }

  public boolean isWarnEnabled() {
    return log.isWarnEnabled();
  }

  public void warn(TracingContext ctx, String var1) {
    logIf(ctx, isWarnEnabled(), (n) -> log.warn(var1));
  }

  public void warn(TracingContext ctx, String var1, Object var2) {
    logIf(ctx, isWarnEnabled(), (n) -> log.warn(var1, var2));
  }

  public void warn(TracingContext ctx, String var1, Object... var2) {
    logIf(ctx, isWarnEnabled(), (n) -> log.warn(var1, var2));
  }

  public void warn(TracingContext ctx, String var1, Object var2, Object var3) {
    logIf(ctx, isWarnEnabled(), (n) -> log.warn(var1, var2, var3));
  }

  public void warn(TracingContext ctx, String var1, Throwable var2) {
    logIf(ctx, isWarnEnabled(), (n) -> log.warn(var1, var2));
  }

  public boolean isWarnEnabled(Marker var1) {
    return log.isWarnEnabled(var1);
  }

  public void warn(TracingContext ctx, Marker var1, String var2) {
    logIf(ctx, isWarnEnabled(), (n) -> log.warn(var1, var2));
  }

  public void warn(TracingContext ctx, Marker var1, String var2, Object var3) {
    logIf(ctx, isWarnEnabled(), (n) -> log.warn(var1, var2, var3));
  }

  public void warn(TracingContext ctx, Marker var1, String var2, Object var3, Object var4) {
    logIf(ctx, isWarnEnabled(), (n) -> log.warn(var1, var2, var3, var4));
  }

  public void warn(TracingContext ctx, Marker var1, String var2, Object... var3) {
    logIf(ctx, isWarnEnabled(), (n) -> log.warn(var1, var2, var3));
  }

  public void warn(TracingContext ctx, Marker var1, String var2, Throwable var3) {
    logIf(ctx, isWarnEnabled(), (n) -> log.warn(var1, var2, var3));
  }

  public boolean isErrorEnabled() {
    return log.isErrorEnabled();
  }

  public void error(TracingContext ctx, String var1) {
    logIf(ctx, isErrorEnabled(), (n) -> log.error(var1));
  }

  public void error(TracingContext ctx, String var1, Object var2) {
    logIf(ctx, isErrorEnabled(), (n) -> log.error(var1, var2));
  }

  public void error(TracingContext ctx, String var1, Object var2, Object var3) {
    logIf(ctx, isErrorEnabled(), (n) -> log.error(var1, var2, var3));
  }

  public void error(TracingContext ctx, String var1, Object... var2) {
    logIf(ctx, isErrorEnabled(), (n) -> log.error(var1, var2));
  }

  public void error(TracingContext ctx, String var1, Throwable var2) {
    logIf(ctx, isErrorEnabled(), (n) -> log.error(var1, var2));
  }

  public boolean isErrorEnabled(Marker var1) {
    return log.isErrorEnabled(var1);
  }

  public void error(TracingContext ctx, Marker var1, String var2) {
    logIf(ctx, isErrorEnabled(), (n) -> log.error(var1, var2));
  }

  public void error(TracingContext ctx, Marker var1, String var2, Object var3) {
    logIf(ctx, isErrorEnabled(), (n) -> log.error(var1, var2, var3));
  }

  public void error(TracingContext ctx, Marker var1, String var2, Object var3, Object var4) {
    logIf(ctx, isErrorEnabled(), (n) -> log.error(var1, var2, var3, var4));
  }

  public void error(TracingContext ctx, Marker var1, String var2, Object... var3) {
    logIf(ctx, isErrorEnabled(), (n) -> log.error(var1, var2, var3));
  }

  public void error(TracingContext ctx, Marker var1, String var2, Throwable var3) {
    logIf(ctx, isErrorEnabled(), (n) -> log.error(var1, var2, var3));
  }
}
