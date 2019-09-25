#### consul-json

Reads configuration JsonObject from Consul value.

The store configuration is used to create an instance of ConsulClient.
Check the documentation of the Vert.x Consul Client for further details.

Parameters specific to the Consul Configuration Store:

* `path`: Consul key at which JsonObject is stored

Example store configuration:

```json
{
  "type": "consul-json",
  "format": "json",
  "config": {
    "host": "localhost",
    "port": 8500,
    "path": "cloudentity/config/authz",
    "fallback": {
      "key1": "value1",
      "key2": "value2"
    }
  }
}
```

Optionally to `path` we can set `prefix` and `key` that concatenated are used as `path`.
`fallback` is optional, defaults to {} - used when the value at `path` is not set