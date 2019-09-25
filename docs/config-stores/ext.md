#### {config-store}-ext

`-ext` config store extends underlying store with additional functionality.

Extended config stores:

* http-ext
* file-ext
* consul-ext
* vault-ext
* vault-keycerts-ext

Configuration provided for this ConfigStore is passed to the underlying ConfigStore.
ExtConfigStore uses JsonObject at `ext` key as its actual configuration.

ExtConfigStore config attributes:

* `sourcePath` - optional, path to the config value
* `outputPath` - optional, path at which the config is put
* `sourceFormat` - optional, format of the underlying configuration object, supported formats: 'json' (JSON object, default), 'string', `hocon`, `json-array`
* `base64Encode` - optional, default false, if true then it base64-encodes the config value
* `maskSecrets` - optional, default false, if true then config values are masked when printed in init log

##### Example - wrapping JsonObject configuration attribute:
Let's use FileExtConfigStore that wraps FileConfigStore.

Given following store configuration (note it is also used by FileConfigStore):

```json
{
  "path": "path/to/configuration/file.json",
  "ext": {
    "outputPath": "some-key.some-other-key"
  }
}
```

and following content of `file.json`:

```json
{
  "a": "x"
}
```

the resulting configuration object is:

```json
{
  "some-key": {
    "some-other-key": {
      "a": "x"
    }
  }
}
```

##### Example - wrapping string configuration attribute:
Let's use HttpExtConfigStore that wraps HttpConfigStore

Given following store configuration:

```json
{
  "host": "localhost",
  "port": 8000,
  "path": "/pki/ca_chain",
  "ext": {
    "outputPath": "pki",
    "sourceFormat": "string",
    "base64Encode": true
  }
}
```

and following response from GET http://localhost:8000/pki/ca_chain:

`200 'xyz'`

the resulting configuration object is (note `pki` is Base64-encoded):

```json
{
  "pki": "eHl6"
}
```