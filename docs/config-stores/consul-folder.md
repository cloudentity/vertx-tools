#### consul-folder

Reads sub-tree of configuration from Consul at configured path.
Expects Json array or Json object as a leaf in the tree.

The store configuration is used to create an instance of ConsulClient.
Check the documentation of the Vert.x Consul Client for further details.

Parameters specific to the Consul Configuration Store:

* `path`: Consul key at which our configuration is stored
* `exclude`: optional, an array of regular expressions, if path of value in Consul (relative to `path`) matches at least one of the regular expressions then it's ignored

Example store configuration:

```json
{
  "type": "classpath",
  "format": "json",
  "config": {
    "host": "localhost",
    "port": 8500,
    "path": "prod/api-gateway",
    "exclude": ["hazelcast/.*"]
  }
}
```

This implementation covers 3 cases:

* path points to node with value - value must be a valid Json object,
* path points to configuration sub tree - value must be a valid Json object or array,
* path points to configuration sub tree with value as top level leaf

Let's say we have following key in Consul `prod/api-gateway/rules/sla` with
value `{ "key": "value"}`. ConsulFolderConfigStore looks in Consul for given path,
removes path element from configuration key and creates JSON object from
data fetched from Consul.

Configuration output for the above example:

```json
{
 "rules": {
   "SLA": {
     "key": "value"
   }
 }
}
```

Let's say we have another value in our directory, but this value is assigned to
root element (prod/api-gateway) of tree. For example when we have another value
`{"topKey": "topValue"}` then the output will look like:

```json
{
 "rules": {
   "SLA": {
     "key": "value"
   }
 },
 "topKey": "topValue"
}
```

Consider situation when tree's leaf is an array. In this case value looks like
`["former", "latter"]`. After all transformations the output will look like:

```json
{
 "rules": {
   "SLA": [
     "former",
     "latter"
   ]
 }
}
```

When Consul leaf value is empty then the value of corresponding attribute in output is null.

When prefix path matches entire directory path then the value must be a valid Json object.
For example, when directory path looks like `prod/api-gateway`
and value is equal to `{"firstKey": "firstValue", "secKey", "secValue"}`, then the output will look like:

```json
{
  "firstKey": "firstValue",
  "secKey": "secValue"
}
```

All matched subtrees are deep merged into JSON object representation of Consul
directory configuration.

When it's impossible to parse JSON, then IllegalArgumentException will be thrown.
