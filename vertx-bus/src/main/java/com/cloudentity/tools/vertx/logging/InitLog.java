package com.cloudentity.tools.vertx.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class InitLog {
  private static final Logger log = LoggerFactory.getLogger(InitLog.class);
  private static final Marker initMarker = MarkerFactory.getMarker("INIT");

  public InitLog() {
  }

  public static void error(String format, Object... arguments) {
    log.error(initMarker, format, arguments);
  }

  public static void error(String message, Throwable e) {
    log.error(initMarker, message, e);
  }

  public static void warn(String format, Object... arguments) {
    log.warn(initMarker, format, arguments);
  }

  public static void info(String format, Object... arguments) {
    log.info(initMarker, format, arguments);
  }

  public static void debug(String format, Object... arguments) {
    log.debug(initMarker, format, arguments);
  }

  public static void trace(String format, Object... arguments) {
    log.trace(initMarker, format, arguments);
  }
}

