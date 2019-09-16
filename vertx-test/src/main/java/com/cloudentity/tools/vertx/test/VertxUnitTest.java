package com.cloudentity.tools.vertx.test;

import com.cloudentity.tools.vertx.bus.VertxBus;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
abstract public class VertxUnitTest {
  @Rule
  public RunTestOnContext rule = new RunTestOnContext();

  private Vertx _vertx;
  public Vertx vertx() {
    return _vertx;
  }

  @Before
  public void initVertx() {
    _vertx = rule.vertx();
    VertxBus.registerPayloadCodec(_vertx.eventBus());
  }
}