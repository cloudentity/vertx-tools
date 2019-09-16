package com.cloudentity.tools.vertx.bus;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.impl.NoStackTraceThrowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This class provides methods to ask/consume over the Vertx' in-memory event bus.
 *
 * By using this class you get:
 * 1. {@link io.vertx.core.Future}'s clean programming model (e.g. {@link Future#map(Object)}, {@link Future#compose(Function)})
 *    instead of using Vertx's callback-based approach. VertxBus' methods return {@link io.vertx.core.Future}.
 * 2. Proper error handling - all thrown exceptions are logged and signaled by returning failed Future or failing received message.
 * 3. Debug logs of exchanged messages.
 *
 * The messages that are sent and received in ask/consume methods are wrapped in {@link BusPayload}
 * Before using VertxBus' ask/consume methods you should register message codec with {@link VertxBus#registerPayloadCodec(EventBus)}.
 */
public class VertxBus {
  private static final Logger log = LoggerFactory.getLogger(VertxBus.class);

  /**
   * Delegates to {@link VertxBus#ask(EventBus, String, DeliveryOptions, Class, Object)} with DeliveryOptions with 3000ms send-timeout.
   */
  public static <T> Future<T> ask(EventBus bus, String addr, Class<T> responseClazz, Object request) {
    return ask(bus, addr, new DeliveryOptions().setSendTimeout(3000), responseClazz, request);
  }

  /**
   * Sends a request to given address on provided event bus. The message is wrapped in {@link BusPayload}.
   * Returns failed Future if sending or decoding response failed.
   *
   * @param bus the event bus
   * @param addr address where to send the message
   * @param request the message body
   * @param opts delivery options
   * @tparam T response body type
   * @return response body wrapped in Future
   */
  public static <T> Future<T> ask(EventBus bus, String addr, DeliveryOptions opts, Class<T> responseClazz, Object request) {
    Future<T> f = Future.future();
    log.trace("Asking started on address={} with request={}", addr, request);
    bus.<BusPayload>send(addr, new BusPayload(request), opts, msg -> handleAskResponse(addr, responseClazz, request, f, msg));
    return f;
  }

  private static <T> void handleAskResponse(String addr, Class<T> responseClazz, Object request, Future<T> f, AsyncResult<Message<BusPayload>> msg) {
    if (msg.succeeded()) {
      BusPayload payload = msg.result().body();
      Object response = payload.value;
      if (payload.isFailed()) {
        f.fail(payload.ex);
      } else if (responseClazz.isInstance(response)) {
        log.trace("Asking completed on address={} with request={}, response={}", addr, request, response);
        f.complete((T) response);
      } else {
        log.debug("Asking failed on address={} with request={}, response={}. {} can't be cast to {}", addr, request, msg.cause(), classOf(response), responseClazz);
        f.fail(new NoStackTraceThrowable("Asking failed on address={}. {} can't be cast to {}"));
      }
    } else {
      log.debug("Asking failed on address={} with request={}", addr, request, msg.cause());
      f.fail(msg.cause());
    }
  }

  private static Class classOf(Object o) {
    if (o != null) return o.getClass();
    else return null;
  }

  public static void publish(EventBus bus, String addr, Object body) {
    bus.publish(addr, new BusPayload(body));
  }

  public static void publish(EventBus bus, String addr, DeliveryOptions opts, Object body) {
    bus.publish(addr, new BusPayload(body), opts);
  }

  /**
   * Handles consumption of requests sent to given address on provided event bus.
   * We are assuming that the message was sent via {@link VertxBus#ask} method,
   * i.e. the message is instance of {@link BusPayload}.
   * Fails the received io.vertx.scala.core.eventbus.Message[A] when:
   *   - BusPayload.value can't be cast to `A` (400 error code)
   *   - request handler threw an exception or returned failed Future (500 error code)
   *
   * @param bus the event bus
   * @param addr address where listen on for requests
   * @param handler request handler
   * @tparam A request body type
   * @tparam B response body type
   * @return either an error or response body wrapped in Future
   */
  public static <A, B> void consume(EventBus bus, String addr, Class<A> requestClazz, Function<A, Future<B>> handler) {
    bus.consumer(addr, msg -> {
      Object request = ((BusPayload)msg.body()).value;
      log.trace("Consuming started on address={}, request={}", addr, request);
      if (requestClazz.isInstance(request)) {
        try {
          handler.apply(requestClazz.cast(request)).setHandler(result -> {
            if (result.succeeded()) {
              log.trace("Consuming completed on address={}, request={}, response={}", addr, request, result.result());
              msg.reply(new BusPayload(result.result()));
            } else {
              String errMsg = String.format("Consuming failed on address=%s, request=%s. Request handler returned failed Future: %s", addr, request, result.cause().getMessage());
              log.debug(errMsg, result.cause());
              msg.reply(BusPayload.failed(result.cause()));
            }
          });
        } catch (Throwable ex) {
          String errMsg = String.format("Consuming failed on address=%s, request=%s. Request handler threw an exception: %s", addr, request, ex.getMessage());
          log.debug(errMsg, ex);
          msg.reply(BusPayload.failed(ex));
        }
      } else {
        String errMsg = String.format("Consuming failed on address=%s, request=%s. Could not convert request body to %s", addr, request, requestClazz.getName());
        log.debug(errMsg);
        msg.fail(400, errMsg);
      }
    });
  }

  public static <A> MessageConsumer<Object> consumePublished(EventBus bus, String addr, Class<A> requestClazz, Consumer<A> consumer) {
    return bus.consumer(addr, msg -> {
      Object request = ((BusPayload)msg.body()).value;
      log.trace("Consuming published message started on address={}, request={}", addr, request);
      if (requestClazz.isInstance(request)) {
        try {
          consumer.accept(requestClazz.cast(request));
        } catch (Throwable ex) {
          String errMsg = String.format("Consuming published message failed on address=%s, request=%s. Request consumer threw an exception: %s", addr, request, ex.getMessage());
          log.error(errMsg, ex);
        }
      } else {
        String errMsg = String.format("Consuming published message failed on address=%s, request=%s. Could not convert request body to %s", addr, request, requestClazz.getName());
        log.error(errMsg);
      }
    });
  }

  /**
   * Registers BusPayload codec. BusPayload wraps the actual objects that are exchanged between verticles.
   * Vertx should work in local mode (no cluster), so there is no need to provide implementation of encode/decode over wire.
   *
   * Returns true if executed for the first time with given EventBus.
   */
  public static boolean registerPayloadCodec(EventBus bus) {
    try {
      bus.registerDefaultCodec(BusPayload.class, new InMemoryCodec(BusPayload.class));
      return true;
    } catch (IllegalStateException ex) {
      log.warn("Could not register bus payload codec", ex);
      return false;
    }
  }
}
