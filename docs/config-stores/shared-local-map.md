#### shared-local-map

Reads configuration `JsonObject` from shared-data LocalMap.

In order to programmatically set the configuration execute:

```java
vertx.sharedData().getLocalMap(name).put(key, config)
```

where `name`, `key` are String values and `config` is JsonObject configuration.

```json
{
  "type": "shared-local-map",
  "format": "json",
  "config": {
    "name": "module-test",
    "key": "config"
  }
}
```