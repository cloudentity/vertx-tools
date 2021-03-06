#### vault-keycerts

Reads key-certs from versioned secrets engine in Vault.

This store lists all secrets at given `keyCertPath` in Vault (one-level deep) and aggregates values from `key` and `cert` secret attributes.
Outputs object with 2 attributes containing aggregated arrays of keys and certs.
Names of the output attributes are defined in config at `output.keys` and `output.certs`.
Collecting keys or certs can be skipped by not setting `output.keys` or `output.certs`.

Example:
```
  config.enginePath = /v1/secret
  config.keyCertPath = /ssl-keycerts/global
  output.keys = keyValues
  output.certs = certValues

  Vault structure:
    path=/v1/secret/data/ssl-keycerts/global/cloudentity_com
      -key=abcd
      -cert=efgh
    path=/v1/secret/data/ssl-keycerts/global/ce_com
      -key=1234
      -cert=5678

  Output:
  {
    "keyValues": ["abcd", "1234"],
    "certValues": ["efgh", "5678"]
  }
```

Example store configuration:

```json
{
 "type": "vault-keycerts",
 "format": "json",
 "config": {
    "host": "localhost",
    "port": 8200,
    "vaultToken": "$env:VAULT_TOKEN:string",
    "keyCertPath": "/ssl-keycerts/global",
    "http": {
      "ssl": true,
      "pemTrustOptions": {
        "certValues": ["$env:ROOT_CA:string"]
      },
    },
    "output": {
      "keys": "keyValues",
      "certs": "certValues"
    },
    "fallback": {
      "keys": [],
      "certs": []
    }
  }
}
```

Configuration attributes:

* `host` - Vault host, string
* `port` - optional, Vault port, int, default 8200
* `vaultToken` - Vault auth token, string
* `enginePath` - optional, secrets engine path, string, default '/v1/secret'
* `keyCertPath` - root path where key-certs are stored, string
* `http` - optional, HttpClientOptions
* `output.keys` - optional, name of output attribute with keys, string, keys are not loaded if not set
* `output.certs` - optional, name of output attribute with keys, string, certs are not loaded if not set
* `fallback` - optional, output configuration if listing secrets returns 404
