package com.cloudentity.tools.vertx.logging;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class InitLog {
  private Logger log;
  private final Marker initMarker = MarkerFactory.getMarker("INIT");

  public InitLog(Logger log) {
    this.log = log;
  }

  public static InitLog of(Logger log) {
    return new InitLog(log);
  }

  public void error(String format, Object... arguments) {
    log.error(initMarker, format, arguments);
  }

  public void error(String message, Throwable e) {
    log.error(initMarker, message, e);
  }

  public void warn(String format, Object... arguments) {
    log.warn(initMarker, format, arguments);
  }

  public void info(String format, Object... arguments) {
    log.info(initMarker, format, arguments);
  }

  public void debug(String format, Object... arguments) {
    log.debug(initMarker, format, arguments);
  }

  public void trace(String format, Object... arguments) {
    log.trace(initMarker, format, arguments);
  }
}

