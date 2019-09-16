package com.cloudentity.tools.vertx.conf.ext;

import com.cloudentity.tools.vertx.conf.common.JsonUtils;
import com.cloudentity.tools.vertx.conf.common.YamlUtils;
import com.cloudentity.tools.vertx.json.JsonExtractor;
import io.vertx.config.spi.ConfigStore;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

import java.util.*;

/**
 * Wrapper of a ConfigStore. Extends ConfigStore with additional functionality.
 *
 * `-ext` config store extends underlying store with additional functionality.
 *
 * Configuration provided for this ConfigStore is passed to the underlying ConfigStore.
 * ExtConfigStore uses JsonObject at `ext` key as its actual configuration.
 *
 * ExtConfigStore config attributes:
 *
 * * `sourcePath` - optional, path to the config value
 * * `outputPath` - optional, path at which the config is put
 * * `sourceFormat` - optional, format of the underlying configuration object, supported formats: 'json' (JSON object, default), 'string', `hocon`, `json-array`
 * * `base64Encode` - optional, default false, if true then it base64-encodes the config value
 * * `maskSecrets` - optional, default false, if true then config values are masked when printed in init log
 *
 * ===== Example - wrapping JsonObject configuration attribute:
 * Let's use FileExtConfigStore that wraps FileConfigStore.
 *
 * Given following store configuration (note it is also used by FileConfigStore):
 *
 * .meta-config
 * [source,json]
 * ----
 * {
 *   "path": "path/to/configuration/file.json",
 *   "ext": {
 *     "sourcePath": "key1.key2"
 *     "outputPath": "some-key.some-other-key"
 *   }
 * }
 * ----
 *
 * and following content of `file.json`:
 *
 * [source,json]
 * ----
 * {
 *   "key1": {
 *     "key2": {
 *       "a": "x"
 *     }
 *   }
 * }
 * ----
 *
 * the resulting configuration object is:
 *
 * [source,json]
 * ----
 * {
 *   "some-key": {
 *     "some-other-key": {
 *       "a": "x"
 *     }
 *   }
 * }
 * ----
 *
 * ===== Example - wrapping string configuration attribute:
 * Let's use HttpExtConfigStore that wraps HttpConfigStore
 *
 * Given following store configuration:
 *
 * [source,json]
 * ----
 * {
 *   "host": "localhost",
 *   "port": 8000,
 *   "path": "/pki/ca_chain",
 *   "ext": {
 *     "outputPath": "pki",
 *     "sourceFormat": "string",
 *     "base64Encode": true
 *   }
 * }
 * ----
 *
 * and following response from GET http://localhost:8000/pki/ca_chain:
 *
 * `200 'xyz'`
 *
 * the resulting configuration object is (note `pki` is Base64-encoded):
 *
 * [source,json]
 * ----
 * {
 *   "pki": "eHl6"
 * }
 * ----
 *
 */
public class ExtConfigStore implements ConfigStore {

  private final ConfigStore store;
  private final Optional<String> outputPathOpt;
  private final Optional<String> sourceFormatOpt;
  private final Optional<String> sourcePathOpt;
  private boolean base64Encode;
  private boolean maskSecrets;

  public ExtConfigStore(ConfigStore store, Optional<String> outputPathOpt, Optional<String> sourceFormatOpt, Optional<String> sourcePathOpt, boolean base64Encode, boolean maskSecrets) {
    this.store = store;
    this.outputPathOpt = outputPathOpt;
    this.sourceFormatOpt = sourceFormatOpt;
    this.sourcePathOpt = sourcePathOpt;
    this.base64Encode = base64Encode;
    this.maskSecrets = maskSecrets;
  }

  @Override
  public void get(Handler<AsyncResult<Buffer>> handler) {
    Future<Buffer> fut = Future.future();
    store.get(fut);
    fut.map(this::base64EncodeConfig).map(this::wrapConfig).map(this::maskSecrets).setHandler(handler);
  }

  private Buffer wrapConfig(Buffer buffer) {
    if (sourcePathOpt.isPresent()) {
      Optional<Object> resolvedValue = JsonExtractor.resolveValue(buffer.toJsonObject(), sourcePathOpt.get());
      if(resolvedValue.isPresent()){
        buffer = Buffer.buffer(resolvedValue.get().toString());
      } else {
        throw new IllegalArgumentException("Value 'sourcePath': " + sourcePathOpt.get() + " can't be located in service configuration store");
      }
    }

    if (outputPathOpt.isPresent()) {
      String outputPath = outputPathOpt.get();
      String format = this.sourceFormatOpt.orElse("json");

      if ("string".equals(format)) {
        return JsonUtils.wrapJsonConfig(buffer.toString(), outputPath).toBuffer();
      } else if ("json".equals(format)) {
        return JsonUtils.wrapJsonConfig(buffer.toJsonObject(), outputPath).toBuffer();
      } else if ("json-array".equals(format)) {
        return JsonUtils.wrapJsonConfig(buffer.toJsonArray(), outputPath).toBuffer();
      } else if ("hocon".equals(format)) {
        return Buffer.buffer(outputPath + "{" + buffer.toString() + "}");
      } else if ("yaml".equals(format)) {
        return YamlUtils.wrapYamlConfig(buffer.toString(), outputPath).toBuffer();
      } else {
        throw new IllegalArgumentException("Unsupported 'sourceFormat': " + format);
      }
    } else {
      return buffer;
    }
  }

  private Buffer base64EncodeConfig(Buffer buffer) {
    if (base64Encode) {
      return Buffer.buffer(Base64.getEncoder().encode(buffer.getBytes()));
    } else {
      return buffer;
    }
  }

  private Buffer maskSecrets(Buffer buffer) {
    if (maskSecrets) {
      JsonObject o = buffer.toJsonObject();
      return o.put("_mask", SecretsMask.create(o)).toBuffer();
    } else {
      return buffer;
    }
  }

  @Override
  public void close(Handler<Void> completionHandler) {
    store.close(completionHandler);
  }
}
