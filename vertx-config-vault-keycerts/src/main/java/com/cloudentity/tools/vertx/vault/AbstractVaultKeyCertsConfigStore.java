package com.cloudentity.tools.vertx.vault;

import com.cloudentity.tools.vertx.futures.FutureUtils;
import com.cloudentity.tools.vertx.json.JsonExtractor;
import io.vavr.control.Either;
import io.vertx.config.spi.ConfigStore;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Abstract implementation of key-certs store in Vault.
 * Can be extended to support versioned and un-versioned secrets engine.
 */
public abstract class AbstractVaultKeyCertsConfigStore implements ConfigStore {
  private Logger log = LoggerFactory.getLogger(this.getClass());

  private final WebClient client;

  private final Optional<String> vaultTokenOpt;
  private final String secretPath;
  private final String listPath;

  private final String enginePath;
  private final String keyCertPath;

  private final Optional<String> keysOutputKeyOpt;
  private final Optional<String> certsOutputKeyOpt;

  private final Optional<JsonObject> fallback;

  private static final String VAULT_TOKEN_HEADER = "X-Vault-Token";
  private static final String VAULT_KEY_KEY = "key";
  private static final String VAULT_CERT_KEY = "cert";


  public AbstractVaultKeyCertsConfigStore(Vertx vertx, JsonObject config) {
    String host = config.getString("host");
    Integer port = config.getInteger("port", 8200);

    WebClientOptions clientOptions =
      new WebClientOptions(config.getJsonObject("http", new JsonObject()))
        .setDefaultPort(port)
        .setDefaultHost(host);

    client = WebClient.create(vertx, clientOptions);

    vaultTokenOpt = Optional.ofNullable(config.getString("vaultToken"));

    enginePath = config.getString("enginePath", "/v1/secret");
    keyCertPath = config.getString("keyCertPath");

    keysOutputKeyOpt = Optional.ofNullable(config.getJsonObject("output").getString("keys"));
    certsOutputKeyOpt = Optional.ofNullable(config.getJsonObject("output").getString("certs"));

    fallback = Optional.ofNullable(config.getJsonObject("fallback"));

    if (!keysOutputKeyOpt.isPresent() && !certsOutputKeyOpt.isPresent()) {
      throw new RuntimeException("'output.keys' or 'output.certs' need to be set");
    }

    secretPath = secretPath(enginePath, keyCertPath);
    listPath = listPath(enginePath, keyCertPath);
  }

  public static class KeyCerts {
    public final List<String> keys;
    public final List<String> certs;

    public KeyCerts(List<String> keys, List<String> certs) {
      this.keys = keys;
      this.certs = certs;
    }

    public KeyCerts() {
      this.keys = new ArrayList<>();
      this.certs = new ArrayList<>();
    }

    public KeyCerts merge(KeyCerts other) {
      ArrayList<String> keys = new ArrayList<>(this.keys);
      keys.addAll(other.keys);

      ArrayList<String> certs = new ArrayList<>(this.certs);
      certs.addAll(other.certs);

      return new KeyCerts(keys, certs);
    }
  }

  protected abstract String listPath(String enginePath, String secretPath);
  protected abstract String secretPath(String enginePath, String secretPath);

  @Override
  public void get(Handler<AsyncResult<Buffer>> completionHandler) {
    listSecrets().compose(secretNamesOpt -> {
      if (secretNamesOpt.isPresent()) {
        List<Future<Either<Throwable, JsonObject>>> futures = fetchSecrets(secretNamesOpt.get());

        return FutureUtils.sequence(futures).compose(asyncErrorOrBodies -> {
          KeyCerts keyCerts = readKeyCertsIgnoringErrors(asyncErrorOrBodies);
          return Future.succeededFuture(buildOutput(keyCerts).toBuffer());
        });
      } else if (fallback.isPresent()){
        return Future.succeededFuture(fallback.get().toBuffer());
      } else {
        return Future.failedFuture(new RuntimeException("Received 404 secrets-list response from Vault. path=" + secretPath));
      }
    }).setHandler(completionHandler);
  }

  private Future<Optional<List<String>>> listSecrets() {
    HttpRequest<Buffer> request = client.get(listPath + "?list=true");
    vaultTokenOpt.map(vaultToken -> request.putHeader(VAULT_TOKEN_HEADER, vaultToken));
    return FutureUtils.<HttpResponse<Buffer>>asFuture(h -> request.send(h)).compose( response -> {
      log.debug("Received secrets-list response from Vault. path=" + secretPath + ", code=" + response.statusCode() + ", body=" + response.bodyAsString());

      JsonObject listBody = response.bodyAsJsonObject();
      Optional<JsonArray> secretNames = JsonExtractor.resolve(listBody, "data").map(x -> x.getJsonArray("keys"));

      if (response.statusCode() == 200 && secretNames.isPresent()) {
        List<String> names = secretNames.get().stream().map(x -> x.toString()).collect(Collectors.toList());
        return Future.succeededFuture(Optional.of(names));
      } else if (response.statusCode() == 404){
        return Future.succeededFuture(Optional.empty());
      } else {
        return Future.failedFuture("Invalid response from Vault on listing keycerts. code=" + response.statusCode() + ", body=" + response.bodyAsString());
      }
    });
  }

  private KeyCerts readKeyCertsIgnoringErrors(List<Either<Throwable, JsonObject>> errorOrBodies) {
    List<Throwable> fetchSecretErrors = filterLeft(errorOrBodies);
    List<JsonObject> bodies           = filterRight(errorOrBodies);

    logFetchSecretErrors(fetchSecretErrors);
    return aggregateKeyCerts(extractKeyCerts(bodies));
  }

  private void logFetchSecretErrors(List<Throwable> errors) {
    errors.forEach(x -> log.error("Loading single keycerts from Vault failed", x));
  }

  private KeyCerts aggregateKeyCerts(List<KeyCerts> collectedKeyCerts) {
    return io.vavr.collection.List.ofAll(collectedKeyCerts).foldLeft(new KeyCerts(), (acc, kc) -> acc.merge(kc));
  }

  private <A, B> List<A> filterLeft(List<Either<A, B>> xs) {
    return xs.stream().filter(x -> x.isLeft()).map(x -> x.getLeft()).collect(Collectors.toList());
  }

  private <A, B> List<B> filterRight(List<Either<A, B>> xs) {
    return xs.stream().filter(x -> x.isRight()).map(x -> x.get()).collect(Collectors.toList());
  }

  private JsonObject buildOutput(KeyCerts keyCerts) {
    JsonObject output = new JsonObject();
    keysOutputKeyOpt.map(x -> output.put(x, keyCerts.keys));
    certsOutputKeyOpt.map(x -> output.put(x, keyCerts.certs));
    return output;
  }

  protected abstract Optional<JsonObject> readDataFromBody(JsonObject body);

  private List<KeyCerts> extractKeyCerts(List<JsonObject> bodies) {
    return
      bodies.stream().map(body -> {
        Optional<JsonObject> dataOpt = readDataFromBody(body);

        List<String> keys = new ArrayList<>();
        List<String> certs = new ArrayList<>();

        if (!dataOpt.isPresent()) {
          log.error("Invalid 'data' attribute in Vault keycert response. body=" + body.toString());
          return new KeyCerts();
        }

        JsonObject data = dataOpt.get();
        if (keysOutputKeyOpt.isPresent()) {
          String key = data.getString(VAULT_KEY_KEY);
          if (key == null) {
            log.error("Missing 'data." + VAULT_KEY_KEY + "' attribute in Vault keycert response. body=" + body.toString());
          } else {
            keys.add(key);
          }
        }

        if (certsOutputKeyOpt.isPresent()) {
          String cert = data.getString(VAULT_CERT_KEY);
          if (cert == null) {
            log.error("Missing 'data." + VAULT_CERT_KEY + "' attribute in Vault keycert response. body=" + body.toString());
          } else {
            certs.add(cert);
          }
        }

        return new KeyCerts(keys, certs);
      }).collect(Collectors.toList());
  }

  private List<Future<Either<Throwable, JsonObject>>> fetchSecrets(List<String> secretNames) {
    return
      secretNames.stream()
        .map(key -> {
          String keyCertPath = secretPath + "/" + key;
          return FutureUtils.<HttpResponse<Buffer>>asFuture(h -> {
            HttpRequest<Buffer> request = client.get(keyCertPath);
            vaultTokenOpt.map(vaultToken -> request.putHeader(VAULT_TOKEN_HEADER, vaultToken));
            request.send(h);
          })
            .compose(response -> {
              log.debug("Received secrets-get response from Vault. path=" + keyCertPath + ", code=" + response.statusCode() + ", body=" + response.bodyAsString());
              if (response.statusCode() == 200) {
                return Future.succeededFuture(response.bodyAsJsonObject());
              } else {
                return Future.failedFuture("Invalid response from Vault on getting keycerts. path=" + secretPath + "/" + key + ", code=" + response.statusCode() + ", body=" + response.bodyAsString());
              }
            })
            .map(resp -> Either.<Throwable, JsonObject>right(resp))
            .recover((Throwable ex) -> Future.succeededFuture(Either.left(new RuntimeException("Could not read keycerts from Vault at path=" + keyCertPath, ex))));
        }).collect(Collectors.toList());
  }
}
