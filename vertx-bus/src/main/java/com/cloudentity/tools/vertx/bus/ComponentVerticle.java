package com.cloudentity.tools.vertx.bus;

import com.cloudentity.tools.vertx.json.JsonExtractor;
import com.cloudentity.tools.vertx.conf.ConfService;
import com.cloudentity.tools.vertx.conf.ConfVerticle;
import com.cloudentity.tools.vertx.tracing.TracingManager;
import com.cloudentity.tools.vertx.tracing.TracingService;
import com.cloudentity.tools.vertx.tracing.TracingVerticle;
import com.cloudentity.tools.vertx.tracing.internals.JaegerTracing;
import io.vertx.config.ConfigChange;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

/**
 * Base verticle that has access to configuration from ConfVerticle.
 */
public abstract class ComponentVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(ComponentVerticle.class);

  private ConfService confService;
  private JsonObject conf;

  private TracingService tracingService;
  private TracingManager tracing;

  private Map<String, Handler<ConfigChanged>> confChangeListeners = new HashMap<>();

  @Override
  public void start(Future<Void> start) {
    confService = VertxEndpointClient.make(vertx, ConfService.class);
    tracingService = VertxEndpointClient.make(vertx, TracingService.class);

    getConfigAsync()
      .map(this::setConf)
      .compose(x -> initTracing())
      .map(x -> initComponentAndRegisterConfChangeConsumer())
      .compose(x -> initComponentAsync())
      .setHandler(start);
  }

  protected Void initComponentAndRegisterConfChangeConsumer() {
    try {
      start();
      initComponent();
      registerSelfConfChangeConsumer();
      return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Provided listener will be notified when configuration of this verticle is changed.
   * Use ConfigChanged.hasChanged method to check if a specific attribute has changed.
   *
   * The listener execution is thread-safe, i.e. no other message consumed by the ComponentVerticle is handled concurrently with the listener execution.
   *
   * @param listener will be called when configuration changed
   * @return listener id
   */
  protected String registerSelfConfChangeListener(Handler<ConfigChanged> listener) {
    String listenerId = UUID.randomUUID().toString();
    confChangeListeners.put(listenerId, listener);
    return listenerId;
  }

  protected void unregisterSelfConfChangeListener(String listenerId) {
    confChangeListeners.remove(listenerId);
  }

  /**
   * Override this method to execute verticle's initialization logic.
   * It's executed at the end of asynchronous ComponentVerticle.start().
   *
   * Use this method instead of overriding asynchronous AbstractVerticle.start().
   */
  protected Future<Void> initComponentAsync() {
    return Future.succeededFuture();
  }

  /**
   * Override this method to execute verticle's initialization logic.
   * It's executed in sync ServiceVerticle.start() as soon as the verticle's configuration is loaded.
   *
   * Use this method instead of overriding AbstractVerticle.start().
   */
  protected void initComponent() {
  }


  /**
   * Override this method to execute verticle's cleanup logic.
   * It's executed in the asynchronous ComponentVerticle.stop().
   *
   * Use this method instead of overriding asynchronous AbstractVerticle.stop().
   * Implement either `cleanup` or `cleanupAsync`, not both.
   */
  protected Future<Void> cleanupAsync() {
    cleanup();
    return Future.succeededFuture();
  }

  /**
   * Override this method to execute verticle's cleanup logic.
   * It's executed in sync ComponentVerticle.stop().
   *
   * Use this method instead of overriding AbstractVerticle.stop().
   * Implement either `cleanup` or `cleanupAsync`, not both.
   */
  protected void cleanup() {
  }

  @Override
  public void stop(Future<Void> stopFuture) {
    Consumer<Future> stop = (x) -> {
      try {
        super.stop(x);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };

    toFuture(stop).compose(x -> cleanupAsync()).setHandler(stopFuture);
  }

  private JsonObject setConf(JsonObject c) {
    if (c == null) {
      log.debug(verticleId() + ":" + this.getClass().getName() + " has no configuration in ConfVerticle");
    }
    return conf = c;
  }

  private void registerSelfConfChangeConsumer() {
    registerConfChangeConsumer(change -> {
      if (configPath() != null) {
        Optional<JsonObject> newConf = JsonExtractor.resolve(change.getNewConfiguration(), configPath());
        if (newConf.isPresent() && !newConf.get().equals(conf)) {
          log.debug("Updating configuration. Old={}, new={}", conf, newConf);
          ConfigChanged configChanged = new ConfigChanged(Optional.ofNullable(conf), newConf.get());
          conf = newConf.get();

          notifyConfChangeListeners(configChanged);
        }
      }
    });
  }

  private void notifyConfChangeListeners(ConfigChanged configChanged) {
    for (Handler<ConfigChanged> listener : confChangeListeners.values()) {
      listener.handle(configChanged);
    }
  }

  /**
   * Get TracingManager via TracingService or use dummy tracing manager in case of error.
   */
  protected Future<TracingManager> initTracing() {
      if (!tracingEnabled()) {
        this.tracing = JaegerTracing.noTracing;
        return Future.succeededFuture(tracing);
      }

      return tracingService.getTracingManager().map(tracing -> {
        this.tracing = tracing;
        return tracing;
      }).recover(t -> {
        log.warn("Failed to initialize tracing. Using noTracing");
        log.debug("Failed to initialize tracing", t);
        this.tracing = JaegerTracing.noTracing;
        return Future.succeededFuture(tracing);
      });
  }

  /**
   * Overwrite this method to disable tracing initialization
   */
  protected boolean tracingEnabled() {
    return TracingVerticle.isTracingEnabled(vertx);
  }

  public TracingManager getTracing() {
    return tracing;
  }

  public ConfService getConfService() {
    return confService;
  }

  /**
   * Registers for global configuration change.
   */
  protected void registerConfChangeConsumer(Consumer<ConfigChange> handler) {
    VertxBus.consumePublished(vertx.eventBus(), ConfVerticle.CONFIG_CHANGE_ADDRESS, ConfigChange.class, handler);
  }

  /**
   * It can be used to differentiate between instances of the same Verticle class, but with different configuration.
   * Can be used in implementation of {@link ServiceVerticle#vertxServiceAddressPrefix()}.
   *
   * When RegistryVerticle deploys verticle it passes 'verticleId' to config().
   */
  public String verticleId() {
    return config().getString("verticleId");
  }

  /**
   * Returns configuration path that is used to retrieve configuration of this verticle from {@link ConfVerticle}.
   *
   * This implementation reads value from {@link AbstractVerticle#config()} or uses {@link ComponentVerticle#verticleId()} as fallback.
   */
  public String configPath() {
    String configPath = config().getString("configPath");
    return configPath != null ? configPath : verticleId();
  }

  /**
   * This method returns configuration of this verticle retrieved from {@link ConfVerticle}.
   * This method should be used instead of {@link AbstractVerticle#config()}.
   *
   * In the {@link ComponentVerticle#start(Future)} method ServiceVerticle calls ConfVerticle to retrieve
   * its configuration and saves it in local variable. ComponentVerticle has getConfig method that returns
   * the configuration set in start. This method is synchronous and non-blocking.
   *
   * ComponentVerticle consumes on an address where ConfVerticle publishes changed configuration.
   * The consume handler updates the configuration. This way there is no need to get configuration asynchronously
   * in the classes extending ServiceVerticle.
   *
   * However, when we deploy ComponentVerticle we should pass `verticleId` value in the config() that will be later used
   * by ConfVerticle to resolve proper configuration object. RegistryVerticle passes verticleId to the config()
   * of managed verticles, so we need to take care of it when deploy verticles on our own.
   * @return
   */
  public JsonObject getConfig() {
    return conf;
  }

  /**
   * Use {@link ComponentVerticle#getConfig()} instead.
   */
  @Deprecated
  @Override
  public JsonObject config() {
    return super.config();
  }

  protected Future<JsonObject> getConfigAsync() {
    if (configPath() != null) {
      return confService.getConf(configPath());
    } else {
      log.debug("No configuration path defined for verticle={}, class={}, could not retrieve configuration from ConfVerticle", verticleId(), this.getClass().getName());
      return Future.succeededFuture(new JsonObject());
    }
  }

  protected <T> Future<T> toFuture(Consumer<Future> start) {
    Future promise = Future.future();
    start.accept(promise);
    return promise;
  }

  public <T> T createClient(Class<T> clazz) {
    return VertxEndpointClient.makeWithTracing(vertx, tracing, clazz);
  }

  public <T> T createClient(Class<T> clazz, Optional<String> addressPrefixOpt) {
    return VertxEndpointClient.makeWithTracing(vertx, tracing, clazz, addressPrefixOpt);
  }

  public <T> T createClient(Class<T> clazz, String addressPrefix) {
    return VertxEndpointClient.makeWithTracing(vertx, tracing, clazz, Optional.ofNullable(addressPrefix));
  }

  public <T> T createClient(Class<T> clazz, DeliveryOptions opts) {
    return VertxEndpointClient.makeWithTracing(vertx, tracing, clazz, Optional.empty(), opts);
  }

  public <T> T createClient(Class<T> clazz, Optional<String> addressPrefixOpt, DeliveryOptions opts) {
    return VertxEndpointClient.makeWithTracing(vertx, tracing, clazz, addressPrefixOpt, opts);
  }

  public <T> T createClient(Class<T> clazz, String addressPrefix, DeliveryOptions opts) {
    return VertxEndpointClient.makeWithTracing(vertx, tracing, clazz, Optional.ofNullable(addressPrefix), opts);
  }

  public static class ConfigChanged {
    private Optional<JsonObject> _previousConfig;
    private JsonObject _newConfig;

    public ConfigChanged(Optional<JsonObject> previousConfig, JsonObject newConfig) {
      _previousConfig = previousConfig;
      _newConfig = newConfig;
    }

    public ConfigChanged(JsonObject previousConfig, JsonObject newConfig) {
      _previousConfig = Optional.ofNullable(previousConfig);
      _newConfig = newConfig;
    }

    /**
     * Returns true if attribute value at given path is different in previous and new configuration.
     * If previous config was not set then always return true.
     */
    public boolean hasChanged(String path) {
      if (_previousConfig.isPresent()) {
        return !JsonExtractor.resolveValue(_previousConfig.get(), path).equals(JsonExtractor.resolveValue(_newConfig, path));
      } else return true;
    }

    public Optional<JsonObject> previousConfig() {
      return _previousConfig;
    }

    public JsonObject nextConfig() {
      return _newConfig;
    }
  }
}
