package com.cloudentity.tools.vertx.messageCodecs;

import io.netty.util.CharsetUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.Json;

public abstract class PojoJsonMessageCodec<T> implements MessageCodec<T, T> {

  @Override
  public void encodeToWire(Buffer buffer, T t) {
    String strJson = Json.encode(t);
    byte[] encoded = strJson.getBytes(CharsetUtil.UTF_8);
    buffer.appendInt(encoded.length);
    Buffer buff = Buffer.buffer(encoded);
    buffer.appendBuffer(buff);
  }

  @Override
  public T decodeFromWire(int pos, Buffer buffer) {
    int length = buffer.getInt(pos);
    pos += 4;
    byte[] encoded = buffer.getBytes(pos, pos + length);
    String str = new String(encoded, CharsetUtil.UTF_8);
    return Json.decodeValue(str, getType());
  }

  public abstract Class<T> getType();

  @Override
  public T transform(T t) {
    return t;
  }

  @Override
  public String name() {
    return getType().getSimpleName() + "Codec";
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
