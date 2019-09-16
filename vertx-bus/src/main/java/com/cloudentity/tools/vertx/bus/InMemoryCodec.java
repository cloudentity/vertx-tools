package com.cloudentity.tools.vertx.bus;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class InMemoryCodec<T> implements MessageCodec<T, T> {
  private Class<T> clazz;

  public InMemoryCodec(Class<T> clazz) {
    this.clazz = clazz;
  }

  @Override
  public void encodeToWire(Buffer buffer, T t) {
    throw new UnsupportedOperationException(name() + " can be used only in Vetx non-clustered mode");
  }

  @Override
  public T decodeFromWire(int pos, Buffer buffer) {
    throw new UnsupportedOperationException(name() + " can be used only in Vetx non-clustered mode");
  }

  @Override
  public T transform(T t) {
    return t;
  }

  @Override
  public String name() {
    return clazz.getSimpleName();
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
