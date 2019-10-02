# Cloudentity vertx-tools

This repo contains tools helping to develop a Vertx application in Java and Scala.

The key element is `ServiceVerticle`, a `io.vertx.core.Verticle` implementation that provides `Future` abstraction for event-bus communication (instead of callbacks) and robust config injection mechanism.

| Module                          | Description                                                                                       |
|---------------------------------|---------------------------------------------------------------------------------------------------|
| vertx-bus                       | Verticles with Future event-bus abstraction and configuration injection                           |
| vertx-bus-scala                 | Scala extensions for vertx-bus                                                                    |
| vertx-registry                  | Dependency injection and component management                                                     |
| vertx-server                    | HTTP server framework                                                                             |
| vertx-client                    | Future-oriented wrapper of Vertx HTTP client with service-discovery, load-balancing and retries   |
| vertx-sd                        | Service-discovery                                                                                 |
| vertx-sd-consul                 | Consul service-discovery provider                                                                 |
| vertx-config-classpath          | Vertx config-stores reading from classpath                                                        |
| vertx-config-consul-json        | Vertx config-stores reading from Consul                                                           |
| vertx-config-vault-keycerts     | Vertx config-store reading keys and certificates from Vault                                       |
| vertx-config-ext                | Wrappers for Vertx config-stores providing extra functionality                                    |
| vertx-test                      | Testing tools for vertx-bus                                                                       |
| vertx-test-scala                | Testing tools for vertx-bus-scala                                                                 |
| vertx-server-test               | Testing tools for vertx-server                                                                    |

## Contents

* [How to start](#how-to-start)
* [Configuration](#config)
  * [Configuration and verticles](#config-verticle)
  * [Where verticle's configuration is read from](#config-where)
  * [Configuration references](#config-references)
  * [Nullifying object attributes](#config-nullify)
  * [ComponentVerticle initialization](#config-init)
* [Meta configuration](#meta)
  * [Configuration management](#meta-management)
  * [Disabling config store](#meta-disable)
  * [Configuring VertxOptions](#meta-vertx-options)
  * [Integration with Vault](#meta-vault)
  * [Custom configuration stores](#meta-custom-stores)
    * [classpath](docs/config-stores/classpath.md)
    * [classpath-folder](docs/config-stores/classpath-folder.md)
    * [consul-folder](docs/config-stores/consul-folder.md)
    * [consul-json](docs/config-stores/consul-json.md)
    * [vault-keycerts](docs/config-stores/vault-keycerts.md)
    * [ext](docs/config-stores/ext.md)
* [Modules configuration](#modules-config)
  * [How modules configuration is resolved](#modules-how)
  * [Modules and deploying verticles](#modules-verticles)
  * [Modules configuration and Docker](#modules-docker)
  * [Shared modules dependency](#modules-shared)
  * [Required modules](#modules-required)
* [Event bus communication](#bus)
  * [Define service interface](#bus-define)
  * [Implement ServiceVerticle](#bus-implement)
  * [Consuming published messages](#bus-publish)
  * [Call ServiceVerticle](#bus-call)
  * [Service client timeout](#bus-timeout)
  * [ServiceVerticle initialization](#bus-verticle-init)
  * [Verticles cleanup](#bus-verticle-cleanup)
* [Dependency injection](#di)
  * [Defining deployment strategy](#di-strategy)
  * [Defining configuration path of verticle](#di-config-path)
  * [Injecting configuration to verticle](#di-config-inject)
  * [Deploying multiple implementations of VertxEndpoint](#di-impls)
  * [Defining custom DeploymentOptions](#di-deployment-opts)
  * [Disabling verticle](#di-disable)
* [HTTP server](#server)
  * [Routes configuration](#server-routes)
  * [HTTP filters](#server-filters)
  * [HTTP server configuration](#server-config)
  * [Deploying multiple servers](#server-multiple)
* [Examples](#examples)
  * [Create RouteVerticle with configuration access](#examples-route)
  * [Create RouteVerticle passing request param to ServiceVerticle](#example-route-service)
  * [Create RouteVerticle dispatching request to one of multiple VertxEndpoint implementations](#example-route-service-multiple)

<a id="how-to-start"></a>
## How to start

This section describes how to use `vertx-server` module. If you are interested in `vertx-bus` functionality then go to [configuration](#config) or [event bus communication](#event-bus).

#### Add vertx-server dependency

Add following entry in your `pom.xml` or its equivalent if you are not using Maven:

```xml
<dependency>
  <groupId>com.cloudentity.tools.vertx</groupId>
  <artifactId>vertx-server</artifactId>
  <version>${vertx-tools.version}</version>
</dependency>
```

Note: vertx-tools are not available in public Maven repository. Build them first using `mvn clean install command` with JDK 8.

#### Create meta-config.json

`meta-config.json` defines how to get the app's configuration. See [more details](#meta-configuration).
The best place to put it is `src/main/resources`. Let's assume we want to load configuration from a local file.
The `meta-config.json` should contain following code:

```json
{
  "scanPeriod": 5000,
  "stores": [
    {
      "type": "file",
      "format": "json",
      "config": {
        "path": "src/main/resources/config.json"
      }
    }
  ]
}
```

#### Create config.json

In previous step we configured the app to read configuration from `src/main/resources/config.json` file. The minimal configuration looks like this:


```json
{
  "apiServer": {
    "http": {
      "port": 8080
    },
    "routes": [
      {
        "id": "hello-world-route",
        "method": "GET",
        "urlPath": "/hello"
      }
    ]
  },
  "registry:routes": {
    "hello-world-route": { "main": "example.app.HelloWorldRoute" }
  }
}
```

This configuration makes the HTTP server to start on port 8080 and expose one route `GET /hello` that is handled by `example.app.HelloWorldRoute` verticle.

#### Create app bootstrap and route handler

Next step is to create a bootstrap class that looks like this:

```java
package example.app;

import com.cloudentity.tools.vertx.launchers.OrchisCommandLauncher;
import com.cloudentity.tools.vertx.server.VertxBootstrap;
import io.vertx.core.Future;

public class App extends VertxBootstrap {
  /**
   * Your custom app initialization logic.
   */
  @Override
  protected Future beforeServerStart() {
    return Future.succeededFuture();
  }
}
```

And finally we need to implement route handler that returns 200 response with 'Hello world!' string body:

```java
package example.app;

import com.cloudentity.tools.vertx.server.api.routes.RouteService;
import com.cloudentity.tools.vertx.server.api.routes.RouteVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

public class HelloWorldRoute extends RouteVerticle {

  @Override
  public void handle(RoutingContext ctx) {
    ctx.response().end("Hello world!");
  }
}
```
#### Build the app
Make sure the following plugin is used to build the app

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <version>2.3</version>
  <executions>
    <execution>
      <phase>package</phase>
      <goals>
        <goal>shade</goal>
      </goals>
      <configuration>
        <transformers>
          <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
            <manifestEntries>
              <Main-Class>example.app.App</Main-Class>
            </manifestEntries>
          </transformer>
          <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
        </transformers>
        <artifactSet/>
      </configuration>
    </execution>
  </executions>
</plugin>
```

Note that the Main-Class references the bootstrap class created in previous section.


#### Run the app
When you build a fat jar then you should execute following command to run the app:

`java -jar your-app.jar run example.app.App -conf src/main/resources/meta-config.json`

#### Run the app in dry mode
If you are interested how the application config looks like you can run it in dry mode with following command:

`java -jar your-app.jar print-config example.app.App -conf src/main/resources/meta-config.json`

It prints available modules, configuration (with and without resolved modules and references)
and environment variables referenced by root configuration and each module.

<a id="config"></a>
## Configuration

<a id="config-verticle"></a>
### Configuration and verticles

`vertx-bus` project provides solution for configuration management. It implements `com.cloudentity.tools.vertx.conf.ConfVerticle`
singleton verticle that reads `meta-config.json` file in and exposes configuration to other verticles.
The easiest way to have access to configuration from `ConfVerticle` is to extend `ComponentVerticle` (`ServiceVerticle` extends `ComponentVerticle`).
`ComponentVerticle` implements `getConfig()` method that returns `JsonObject` with configuration associated with the instance of `ComponentVerticle`.

<a id="config-where"></a>
### Where verticle's configuration is read from

`ComponentVerticle` has `configPath()` method that returns comma-separated path to the verticle's configuration JsonObject.

Let's assume the global configuration JsonObject is as follows:

```json
{
  "apiServer": {
    "http": {
      "port": 8080
    },
    "routes": []
  },
  "components": {
    "my-component": {
      "message": "Hello world!"
    }
  }
}
```

If `configPath()` returns "components.my-component" then the verticle's configuration is resolved to `{ "message": "Hello world!" }`.

#### Default implementation of configPath()
Let's have a look at the default implementation of `configPath()` and verticle:

```java
public String configPath() {
  String configPath = config().getString("configPath");
  return configPath != null ? configPath : verticleId();
}

public String verticleId() {
  return config().getString("verticleId");
}
```

`configPath()` reads information passed to the verticle at the deployment time in `DeploymentOptions.config.configPath`
and falls back to `verticleId` attribute. You can pass it on your own or use `RegistryVerticle` that does it for you.

Note: if you are using Registry to deploy verticles, you can put the configuration with deployment options. See [Injecting configuration to verticle](#di-config-inject)

<a id="config-references"></a>
### Configuration references
It is possible to make a reference to configuration value. This way it is easy to share common configuration.
The reference is a JSON string with following format `"$ref:{configuration-path}"`. The configuration-path in
the reference string is a path at which the value that should be injected is.

For example, the following configuration:

```json
{
  "verticle-a": {
    "cassandra-port": "$ref:cassandra.port",
    "cassandra-host": ["$ref:cassandra.host1", "$ref:cassandra.host2"]
  },
  "cassandra": {
    "port": 9000,
    "host1": "localhost",
    "host2": "127.0.0.1"
  }
}
```

after reference resolution looks like this:

```json
{
  "verticle-a": {
    "cassandra-port": 9000,
    "cassandra-host": ["localhost", "127.0.0.1"]
  },
  "cassandra": {
    "port": 9000,
    "host1": "localhost",
    "host2": "127.0.0.1"
  }
}
```

When we call `getConfig()` from `ComponentVerticle` or `ServiceVerticle` we get the configuration with references resolved.
When the path from reference is invalid the attribute is set to null. It is up to consumer to validate the configuration and fail the verticle start.

#### Default configuration reference
If we want to define default configuration reference we should provide it in the following format: `"$ref:{reference-path}:{default-value-type}:{default-value}"`.
When the value cannot be resolved at `{reference-path}` then default-value is cast to `default-value-type` and used instead.

E.g. following config resolves server.port to 80:

```json
{
  "server": {
    "port": "$ref:port:int:8080"
  },
  "port": 80
}
```

E.g. following config resolves server.port to 8080:

```json
{
  "server": {
    "port": "$ref:port:int:8080"
  }
}
```

#### System and environment property reference
In similar manner we can make reference to system or environment property. The reference for system property has following format
`"$sys:{property-name}:{property-type}:{default-value}"` and for environment property `"$env:{property-name}:{property-type}:{default-value}"`.
System and environment properties are resolved using `System.getProperty` and `System.getenv` methods respectively.
`property-type` defines what type the property value should be cast to. It is one of "string", "int", "double" or "boolean" (for array and object support see <<Array or object property reference>>).
`default-value` is optional. It is used when the property value is missing. When the reference is invalid it is resolved to `null` value.

Example, following configuration:

```json
{
  "cassandra-port": "$env:CASSANDRA_PORT:int:9000"
}
```

is resolved to this one (provided CASSANDRA_PORT environment property is set to 8000):

```json
{
  "cassandra-port": 8000
}
```

or to this one (provided CASSANDRA_PORT is not set):

```json
{
  "cassandra-port": 9000
}
```
Default value is optional, so we can have following configuration:

```json
{
  "cassandra-port": "$env:CASSANDRA_PORT:int"
}
```

For unit testing it might be cumbersome to provide environment or system variables. To facilitate testing, instead of setting those variables
you can define a map in the root configuration at `env` or `sys` attribute and provide values for the variables.
If environment or system variable is not set then there is an attempt to read it from corresponding attribute in root configuration.

For example let's have following reference to `CASSANDRA_PORT` variable that was not set as environment variable:

```json
{
  "cassandra-port": "$env:CASSANDRA_PORT:int",
  "env": {
    "CASSANDRA_PORT": 9000
  }
}
```

After resolution, we end up with the following configuration:

```json
{
  "cassandra-port": 9000,
  "env": {
    "CASSANDRA_PORT": 9000
  }
}
```

##### Property value expression

Property value can be used as a part of value expression.
To do so, property name should be wrapped in curly braces `{` and `}`.

For example, given `KEY=user` and following configuration:

```json
{
  "path": "$env:/apis/{KEY}:string"
}
```

the value of `path` is `/apis/user`.

We can also use default value:

```json
{
  "path": "$env:/apis/{KEY}:string:session"
}
```

if `KEY` is not set then the value of `path` is `/apis/session`.

#### Array or object property reference

You can use env/sys references to set array or object configuration attribute.
The `property_type` in the reference string is `array` and `object` respectively.
The env/sys value should be string representation of JSON array/object.

As an example, let's have KAFKA_TOPICS env variable set to `["value1", "value2"]`.

The following configuration:

```json
{
  "topics": "$env:KAFKA_TOPICS:array"
}
```

is resolved to:

```json
{
  "topics": ["value1", "value2"]
}
```

You can use default value as well:

```json
{
  "topics": "$env:KAFKA_TOPICS:array:[\"value1\", \"value1\"]"
}
```

#### Optional reference

If a reference could not be resolved and had no default value then warning is logged.
If configuration attribute is optional you can prepend `?` to its reference path to silence the warning.

```json
{
  "consul": {
    "tags": "$env:?CONSUL_TAGS:array"
  }
}
```


#### Escaping ':' reference separator

Reference path and default value can contain escaped colon '\\:'.

E.g. Given `$ref:path:string:localhost\\:8080` the default value is `localhost:8080`.

<a id="config-nullify"></a>
### Nullifying object attributes

Let's consider scenario where we need to configure object with one-of alternative via environment variables.

As an example let's configure `HttpServerOptions.trustOptions`. To do so you choose one of: PemTrustOptions, JksTrustOptions or PfxTrustOptions.
If you choose PemTrustOptions, then `pemTrustOptions` configuration attribute should be set and `jksTrustOptions` and `pfxTrustOptions` should be `null`.

```json
{
  "pemTrustOptions": {
    "certPaths": ["/etc/ssl/cert.pem"]
  }
}
```

Let's configure `trustOptions` using environment variables:

```json
{
  "pemTrustOptions": {
    "certPaths": "$env:PEM_CERT_PATHS:array"
  },
  "jksTrustOptions": {
    "value": "$env:JKS_VALUE:string"
  },
  "pfxTrustOptions": {
    "value": "$env:PFX_VALUE:string"
  }
}
```

The problem with above configuration is that even if we set only `PEM_CERT_PATHS` both `jksTrustOptions` and `pfxTrustOptions`
have value of empty JSON object. We need `jksTrustOptions` and `pfxTrustOptions` to be set to `null`.

In order to do so we need to set `_nullify` attribute to `true` in all config objects:

```json
{
  "pemTrustOptions": {
    "_nullify": true,
    "certPaths": "$env:PEM_CERT_PATHS:array"
  },
  "jksTrustOptions": {
    "_nullify": true,
    "value": "$env:JKS_VALUE:string"
  },
  "pfxTrustOptions": {
    "_nullify": true,
    "value": "$env:PFX_VALUE:string"
  }
}
```

If `_nullify` attribute is set and all other attributes in JsonObject are `null` then entire JsonObject is replaced with `null`.

In our example, if `PEM_CERT_PATHS` is only variable set then we end up with following final configuration:

```json
{
  "pemTrustOptions": {
    "certPaths": ["/etc/ssl/cert.pem"]
  }
}
```
<a id="config-init"></a>
### ComponentVerticle initialization
See section on [ServiceVerticle initialization](#bus-verticle-init).

<a id="meta"></a>
## Meta configuration

When we start `vertx-server` application we need to pass path to `meta-config.json` in the command line argument `-conf`.
Sample `meta-config.json` looks like this:

```json
{
  "scanPeriod": 5000,
  "stores": [
    {
      "type": "file",
      "format": "json",
      "config": {
        "path": "src/main/resources/config.json"
      }
    }
  ],
  "vertx": {
    "options": {
      "addressResolverOptions": {
        "servers": ["127.0.0.11"]
    }
  }
}
```

<a id="meta-management"></a>
### Configuration management

The advantage of using `meta-config.json` is ease of changing the way configuration is distributed.
Imagine all applications in your system read configuration from local files, but we wanted to make them read it from Consul.
What we need to do is just modify the `meta-config.json` providing Consul access configuration and restart the servers.
Another benefit is possibility to split one giant configuration file into several smaller ones that are easier to maintain.
We would just need to list them in the stores section in `meta-config`.
The underlying mechanism uses [vertx-config](http://vertx.io/docs/vertx-config/java/) project.

| Attribute  | Description                                                                            |
|------------|----------------------------------------------------------------------------------------|
| stores     | holds an array of JSON objects that are parsed to `io.vertx.config.ConfigStoreOptions` |
| scanPeriod | defines configuration refresh period in milliseconds                                   |

Every time you call `ComponentVerticle.getConfig()` you retrieve version of the configuration that is not older than scan period.
You can register `ComponentVerticle` to receive information whenever global configuration changes. To do so you need to call
`ComponentVerticle.registerConfChangeConsumer` method passing consumer of `io.vertx.config.ConfigChange` object.

<a id="meta-disable"></a>
### Disabling config store

You can use `enabled` flag to control whether config store should be used. It's set to `true` by default:

```json
{
  "scanPeriod": 5000,
  "stores": [
    {
      "type": "file",
      "format": "json",
      "enabled": false,
      "config": {
        "path": "src/main/resources/config.json"
      }
    }
  ]
}
```

If `enabled` is `false` then the entry is filtered out from the `stores` array. If you reference environment variable
(see [Configuration references](#config-references)) then you can control what config stores are used without changing content of meta config file.

```json
{
 "scanPeriod": 5000,
 "stores": [
   {
     "type": "file",
     "format": "json",
     "enabled": "$env:CONF_FILE_ENABLED:boolean:true",
     "config": {
       "path": "src/main/resources/config.json"
     }
   }
 ]
}
```

<a id="meta-vertx-options"></a>
### Configuring VertxOptions

You can define `io.vertx.core.VertxOptions` that will be used to initialize Vertx instance at the application startup.
You need to provide JSON object at "vertx.options" path that is decoded to VertxOptions.

<a id="meta-vault"></a>
### Integration with Vault - how to store passwords/secrets

Add Vault store in `meta-config.json`:

```json
{
  "type": "vault",
  "format": "json",
  "config": {
    "host": "$env:VAULT_HOST:string:localhost",
    "port": "$env:VAULT_PORT:int:8200",
    "auth-backend": "token",
    "token": "$env:VAULT_TOKEN:string",
    "path": "secret/{YOUR_APP_NAME}"
  }
}
```

VAULT_HOST, VAULT_PORT and VAULT_TOKEN should be set as ENV variables.
In path attribute, replace {YOUR_APP_NAME} with your app name.

Store passwords/secrets in Vault at secret/{YOUR_APP_NAME}
You can store only string values in Vault.
Reference values stored in Vault in your configuration.
The values from Vault are set as top-level configuration attributes.

E.g. data stored in Vault with this command:

`vault write secret/my_app x=a y=b`

is retrieved by Vertx in a form of JSON object:

`{ "x": "a", "y": "b" }`

#### Integration with Vault - example

Let's assume we store two passwords in Vault:

`vault write secret/my_app pass1=!@#$ pass2=*&^%`

and we have following `meta-config.json`:

```json
{
  "scanPeriod": 5000,
  "stores": [
    {
      "type": "file",
      "format": "json",
      "config": {
        "path": "config.json"
      }
    },
    {
      "type": "vault",
      "format": "json",
      "config": {
        "host": "$env:VAULT_HOST:string:localhost",
        "port": "$env:VAULT_PORT:int:8200",
        "auth-backend": "token",
        "token": "$env:VAULT_TOKEN:string",
        "path": "secret/my_app"
      }
    }
  ]
}
```

Following config.json:

```json
{
  "my-service-verticle": {
    "password1": "$ref:pass1",
    "password1": "$ref:pass2",
  }
}
```

The final configuration object looks like this:

```json
{
  "my-service-verticle": {
    "password1": "$ref:pass1",
    "password1": "$ref:pass2",
  },
  "pass1": "!@#$",
  "pass2": "*&^%"
}
```


When configuration references have been resolved then "my-service-verticle" stores passwords from Vault:

```json
{
  "my-service-verticle": {
    "password1": "!@#$",
    "password1": "*&^%",
  },
  "pass1": "!@#$",
  "pass2": "*&^%"
}

```

<a id="meta-custom-stores"></a>
### Custom configuration stores

* [classpath](docs/config-stores/classpath.md)
* [classpath-folder](docs/config-stores/classpath-folder.md)
* [consul-folder](docs/config-stores/consul-folder.md)
* [consul-json](docs/config-stores/consul-json.md)
* [vault-keycerts](docs/config-stores/vault-keycerts.md)
* [ext](docs/config-stores/ext.md)

<a id="modules-config"></a>
## Modules configuration

When we develop an application we usually want to split it into modules.
Some modules implement the same functionality and we choose one of them to run (e.g. modules implementing storage).
Other are optional (e.g. module registering app for service-discovery).

In order to make it easier to configure application there is a special configuration attribute `modules` introduced.
`ConfVerticle` takes that attribute and reads configuration objects from classpath and merges them with root configuration.
Module configuration should reference environment variable for those attributes we want to set for different environments
(urls, hosts, ports, header names, etc.)

<a id="modules-how"></a>
### How modules configuration is resolved

Let’s say our root configuration looks like this:

```json
{
  "apiServer": {
    "http": {
      "port": 9090
    }
  },
  "modules": [
    "policy-storage/cassandra",
    "sd-registrar/consul"
  ]
}
```

`ConfVerticle` tries to read `policy-storage/cassandra` and `sd-registrar/consul` modules configuration from classpath.
The modules configuration should be stored on classpath in `modules` folder. This means that `ConfVerticle` searches for
`modules/policy-storage/cassandra.json` and `modules/sd-registrar/consul.json` files.

Let `modules/policy-storage/cassandra.json` has following content:

```json
{
  "cassandra": {
    "host": "localhost",
    "port": 9042
  }
}
```

and `modules/sd-registrar/consul.json` following:

```json
{
  "consul": {
    "host": "localhost",
    "port": 8500
   }
}
```

`ConfVerticle` merges module configuration objects sequentially and then the root configuration is merged last.

The global configuration looks as follows:

```json
{
  "apiServer": {
    "http": {
      "port": 9090
    }
  },

  "cassandra": {
    "host": "localhost",
    "port": 9042
  },

  "consul": {
    "host": "localhost",
    "port": 8500
  },

  "modules": [
    "policy-storage/cassandra",
    "sd-registrar/consul"
  ]
}
```

When the modules are merged with root configuration then `ConfVerticle` resolves configuration references. See [Configuration references](#config-references).

<a id="modules-verticles"></a>
### Modules and deploying verticles

What actually makes configuration modules useful is the possibility to control what verticles are deployed. In [Dependency Injection](#di)
you learn how to deploy them. The application must start a verticle registry that the modules will use to deploy their verticles.

The simplest root configuration looks like this:

```json
{
  "registry:components": {
  },
  "modules": ["module-a", "module-b"]
}
```

`registry:components` is a placeholder for modules' verticles. Make sure that this registry is programmatically deployed by the application.

The modules configuration can look like this:

```json
{
  "registry:components": {
    "module-a-verticle": {
      "main": "com.example.moduleA.Verticle",
      "verticleConfig": {
        "someAVerticleAttribute": true
      }
    }
  }
}
```

and

```json
{
  "registry:components": {
    "module-b-verticle": {
      "main": "com.example.moduleB.Verticle"
    }
  }
}
```

When modules configuration and root configuration is merged we end up with following configuration:

```json
{
  "registry:components": {
    "module-a-verticle": {
      "main": "com.example.moduleA.Verticle",
      "verticleConfig": {
        "someAVerticleAttribute": true
      }
    },

    "module-b-verticle": {
      "main": "com.example.moduleB.Verticle"
    }
  },

  "modules": ["module-a", "module-b"]
}
```

When the application starts the `registry:components` is deployed that in turn deploys `module-a-verticle` and `module-b-verticle`.

<a id="modules-docker"></a>
### Modules configuration and Docker

If you build your application using modules with environment variable references (see [Configuration references](#config-references) it's easy to run it in Docker container.

Your `modules` configuration attribute should have following value:

```json
{
  "modules": "$env:MODULES:array"
}
```

This means that you can control what modules are used by setting `MODULES` environment variable.

From the perspective of someone who wants to run a docker with your application the process looks as follows:

* set meta-config environment variables (if any) that control where the configuration is coming from

* decide what modules you want to run (e.g. `policy-storage/cassandra` and `sd-registrar/consul`) and set `MODULES` environment variable (e.g. `policy-storage/cassandra,sd-registrar/consul`)

* set required application environment variables

* run docker

As an extra feature, `ConfVerticle` prints out what env variables are used and what values they have.

#### Default configuration modules

You can specify a set of default modules that will be used if `modules` attribute is not set.

```json
{
  "modules": "$env:MODULES:array",
  "defaultModules": ["module-a", "module-b"]
}
```

<a id="modules-shared"></a>
### Shared modules dependency

As a rule of thumb the module configuration file should be complete, i.e. it should not depend on other configuration
attributes to be present in the root config. However it still can reference attributes from root configuration - e.g. secrets,
but it should be limited to minimum and well documented. In particular, it should deploy all verticles the module requires.

The only exception for deploying verticles should be when the application itself uses a verticle the modules depends on.
In this case the verticle is configured in the root configuration so we can be sure it is deployed.

If we have two modules that require the same verticle to be deployed (not used by application itself), then its configuration should be the same in both modules.
After modules configuration merge, there is only one instance of it.

Example:

Let's have two modules that require open-api client verticle. The configuration of module A should like like this:

```json
{
  "registry:components": {
    "sevice-x-client": {
      "main": "x.y.z.ServiceAClient",
      "verticleConfig": {
        "serviceLocation": {
          "host": "$env:SERVICE_A_HOST:string:localhost",
          "port": "$env:SERVICE_A_PORT:int:8010",
          "ssl": "$env:SERVICE_A_SSL:boolean:false"
        }
      }
    },

    # other verticles of module A
    ...
  },

  # other attributes of module A
}
```

similarly module B:

```json
{
  "registry:components": {
    "sevice-x-client": {
      "main": "x.y.z.ServiceAClient",
      "verticleConfig": {
        "serviceLocation": {
          "host": "$env:SERVICE_A_HOST:string:localhost",
          "port": "$env:SERVICE_A_PORT:int:8010",
          "ssl": "$env:SERVICE_A_SSL:boolean:false"
        }
      }
    },

    # other verticles of module B
    ...
  },

  # other attributes of module B
}
```

finally we get the following config:

```json
{
  "registry:components": {
    "sevice-x-client": {
      "main": "x.y.z.ServiceAClient",
      "verticleConfig": {
        "serviceLocation": {
          "host": "$env:SERVICE_A_HOST:string:localhost",
          "port": "$env:SERVICE_A_PORT:int:8010",
          "ssl": "$env:SERVICE_A_SSL:boolean:false"
        }
      }
    },

    # other verticles of module A
    ...

    # other verticles of module B
    ...
  },

  # other verticles of module A
  ...

  # other attributes of module B
}
```

<a id="modules-required"></a>
### Required modules

If we want to load some modules regardless the deployment (e.g. to split classpath configuration for easier maintenance) we can define them in `requiredModules` attribute.

```json
{
  "requiredModules": ["module-x"]
}
```

First `requiredModules` are loaded and then all the other modules.

<a id="bus"></a>
## Event bus communication and ServiceVerticle

Using Vertx' event bus is quite cumbersome. You need to make sure you send messages on proper address and of proper type.
It's easy to make a mistake that is difficult to discover. To fix this you can use `ServiceVerticle` that works like regular Java class,
but in fact you are passing messages via event bus. Note that `ServiceVerticle` extends `ComponentVerticle`, so you still have access to configuration.

Let's imagine we have a simple verticle that wants to send a string to `UpperCaseVerticle` and receive that string in upper-case. We need to do following steps:

<a id="bus-define"></a>
### Define service interface

```java
import io.vertx.core.Future;
import com.cloudentity.tools.vertx.bus.VertxEndpoint;

public interface UpperCaseService {
  @VertxEndpoint(address = "to-uppercase") // address is optional - it is defaulted to full method name
  Future<String> toUpperCase(String s);
}
```

<a id="bus-implement"></a>
### Implement ServiceVerticle

```java
import io.vertx.core.Future;
import com.cloudentity.tools.vertx.bus.ServiceVerticle;

public class UpperCaseVerticle extends ServiceVerticle implements UpperCaseService {
  @Override
  public Future<String> toUpperCase(String s) {
    return Future.succeededFuture(s.toUpperCase());
  }
}
```

When you deploy `UpperCaseVerticle` it's gonna register event-bus consumer on the address configured in the VertxEndpoint
annotation of `UpperCaseService.toUpperCase()`. If the address is not set it is defaulted to full name of the method in
the follwoing format: `{class-name}.{method-name}({comma-separated-parameter-types})`.

When the message is received on that address the body of the message is unpacked and passed to the implementation of `toUpperCase` method in `UpperCaseVerticle`.
The value returned by toUpperCase is sent back to the message sender.

Note that all the methods in `UpperCaseService` interface return a `Future` even though the implementation in `UpperCaseVerticle.toUpperCase` might
have been synchronous (i.e. `s.toUpperCase()`). It is so due to the fact that the interface is used also by the client,
so there will be asynchronous operations to send and receive messages over event bus.

<a id="bus-publish"></a>
### Consuming published messages

If there is no need to return any value then the method in the service interface should return `void`.
Under the hood the message will not be sent but published.

```java
import io.vertx.core.Future;
import com.cloudentity.tools.vertx.bus.VertxEndpoint;

public interface NotifierService {
  @VertxEndpoint
  void notify(String event);
}
```

```java
import io.vertx.core.Future;
import com.cloudentity.tools.vertx.bus.ServiceVerticle;

public class NotifierVerticle extends ServiceVerticle implements NotifierService {
  @Override
  void notify(String event) {
    // do something with `event`
  }
}
```

<a id="bus-call"></a>
### Call ServiceVerticle

```java
import io.vertx.core.Future;
import com.cloudentity.tools.vertx.bus.VertxBus;
import com.cloudentity.tools.vertx.bus.ServiceClientFactory;

public class ClientVerticle extends AbstractVerticle {
  public void start() {
    VertxBus.registerPayloadCodec(vertx.eventBus()); // you don't need this line if you use VertxBootstrap in your project
    UpperCaseService client = ServiceClientFactory.make(vertx.eventBus(), UpperCaseService.class); // when extending ComponentVerticle use: createClient(UpperCaseService.class);

    Future<String> response = client.toUpperCase("hello world!");
    response.setHandler(async -> {
      if (async.succeeded()) {
        System.out.println("hello world to upper-case is " + async.result());
      }
    });
  }
}
```

`ServiceClientFactory.make()` builds a proxy object using reflection. The proxy uses event-bus to send messages on addresses defined
in the `VertxEndpoint` annotation on `UpperCaseService` interface. If you are extending `ServiceVerticle` or `ComponentVerticle` then instead of using
`ServiceClientFactory` use `ComponentVerticle.createClient` method.

```java
import io.vertx.core.Future;
import com.cloudentity.tools.vertx.bus.VertxBus;
import com.cloudentity.tools.vertx.bus.ServiceClientFactory;

public class ClientVerticle extends ComponentVerticle {
  @Override
  protected void initComponent() {
    UpperCaseService client = createClient(UpperCaseService.class);

    Future<String> response = client.toUpperCase("hello world!");
    response.setHandler(async -> {
      if (async.succeeded()) {
        System.out.println("hello world to upper-case is " + async.result());
      }
    });
  }
}
```

<a id="bus-timeout"></a>
### Service client timeout

When you create a client using `ServiceClientFactory` or `createClient` then by default all the calls timeout after 30 seconds.
You can change that timeout by setting `VERTX_SERVICE_CLIENT_TIMEOUT` system or environment variable (in milliseconds).
It applies to all clients unless they are created using `ServiceClientFactory` and `DeliveryOptions` as argument.
`DeliveryOptions` should have `sendTimeout` property set.

<a id="bus-verticle-init"></a>
### ServiceVerticle initialization

In vertx-server we are using hierarchy of verticles: ComponentVerticle - ServiceVerticle - RouteVerticle.
Using vanilla Vertx you would override `AbstractVerticle.start()` and `AbstractVerticle.start(Future)` methods to initialize your verticles.
We could do the same when extending base verticles from vertx-server, but it would be quite tricky due to the need to call
`super.start(Future)`. Moreover, it's easy to forget about calling super. Instead of overriding start methods you should override
`initService`, `initServiceAsync` or `initComponent`, `initComponentAsync`.

Let's follow Initialization sequence of `ServiceVerticle`. It covers initialization of `ComponentVerticle`, since one extends the other.

* ServiceVerticle.start(Future)
  * call ComponentVerticle.start(Future)
    * load verticle's configuration
    * call AbstractVerticle.start()
    * call ComponentVerticle.initComponent
    * call ComponentVerticle.initComponentAsync
  * register event bus consumers based on VertxEndpoint annotations
  * call ServiceVerticle.initService
  * call ServiceVerticle.initServiceAsync

<a id="bus-verticle-cleanup"></a>
### Verticles cleanup

If your `ServiceVerticle` or `ComponentVerticle` needs to do some cleanup when the verticle is stopped, e.g. close connection pool when closing application, then implement one of `cleanup` or `cleanupAsync`.
These methods are called when Vertx executes `AbstractVerticle.stop` method.

```java
public class ResourceVerticle extends ComponentVerticle {
  SomeResource resource;
  @Override
  protected void initComponent() {
    resource = createResource();
  }

  ...

  @Override
  protected void cleanup() {
    if (resource != null) {
      resource.close();
    }
  }
}
```

```java
public class ResourceVerticle extends ComponentVerticle {
  SomeResource resource;
  @Override
  protected void initComponent() {
    resource = createResource();
  }

  ...

  @Override
  protected Future cleanupAsync() {
    Handler<Future> action = fut -> {
      if (resource != null) {
        resource.close();
        fut.complete();
      }
    };

    Future promise = Future.future();
    vertx.executeBlocking(action, promise);
    return promise;
  }
}
```

<a id="di"></a>
## Dependency Injection and RegistryVerticle

`RegistryVerticle` and `ServiceVerticle` provides Dependency Injection capabilities. `ServiceVerticle` gives you the way to define interface
with `VertxEndpoint` annotations. `RegistryVerticle` allows to define what verticles should be deployed.

In order to use `RegistryVerticle` you need to come up with its identifier. Let's our id be "components".
Following code snippet deploys "components" `RegistryVerticle` at the application start:

```java
import com.cloudentity.tools.vertx.registry.RegistryVerticle;
import com.cloudentity.tools.vertx.verticles.VertxDeploy;

public class App extends VertxBootstrap {
  @Override
  protected Future beforeServerStart() {
    return VertxDeploy.deploy(vertx, new RegistryVerticle(new RegistryType("components")));
  }
}
```

The "components" `RegistryVerticle` gets its configuration at "registry:components" key and deploys defined verticles.
The minimal registry configuration has following structure:

```json
{
  "registry:components": {
    "verticle-a-id": {
      "main": "com.example.VerticleA"
    },
    "verticle-b-id": {
      "main": "com.example.VerticleB"
    }
  }
}
```

`RegistryVerticle` reads verticle ids ("verticle-a-id", "verticle-b-id") and deploys corresponding verticles defined under "main" key.
The value of "main" is full name of verticle's class. The deployment order is undefined. When at least one verticle fails to start
it means that `RegistryVerticle` deployment fails as well.

The verticle's id can be accessed from verticle's code with `AbstractVerticle.config().getString("verticleId")` method call.

IMPORTANT: `config` is reserved key in the registry configuration object, it can't be used as verticle id.

<a id="di-strategy"></a>
### Defining deployment strategy

Deployment strategy controls how many instances of a verticle is deployed.

#### Simple deployment strategy

By default `simple` strategy is used, which uses `options.instances` attribute from verticle descriptor.

The following configuration deploys 5 instances of `com.example.Verticle``:

```json
{
  "registry:components": {
    "verticle-id": {
      "main": "com.example.Verticle",
      "options": {
        "instances": 5
      }
    }
  }
}
```

Default value of `options.instances` is 1.

#### CPU deployment strategy

CPU deployment strategy deploys one verticle instance per available CPU.
To use it set `deploymentStrategy` to `cpu` in verticle descriptor:

```json
{
  "registry:components": {
    "verticle-id": {
      "main": "com.example.Verticle",
      "deploymentStrategy": "cpu"
    }
  }
}
```

If you want to deploy 2 times number of CPUs then use `cpux2` deployment strategy.

#### Default deployment strategy

You can define default deployment strategy for all verticles in the registry. If deployment strategy is not defined in the verticle descriptor, then default one is used. To do so set `config.defaultDeploymentStrategy`:

.default deployment strategy
```json
{
  "registry:components": {
    "config": {
      "defaultDeploymentStrategy": "cpu"
    },
    "verticle-id": {
      "main": "com.example.Verticle"
    }
  }
}
```

<a id="di-config-path"></a>
### Defining configuration path of ComponentVerticle or ServiceVerticle

When you are deploying `ComponentVerticle` or `ServiceVerticle` you can override its default configuration path TODO (see default implementation of `configPath()` in [Configuration and verticles](#config-verticles).
To do so add "configPath" string at the level of "main" key:

```json
{
  "registry:components": {
    "verticle-a-id": {
      "main": "com.example.ServiceVerticleA",
      "configPath": "components.verticle-a"
    }
  },
  "components": {
    "verticle-a": {
      ...
    }
  }
}
```

In result, "verticle-a-id" verticle will get it's configuration from "components.verticle-a" object.

```json
{
  "registry:components": {
    "verticle-a-id": {
      "main": "com.example.ServiceVerticleA",
      "configPath": "components.verticle-a"
    }
  },
  "components": {
    "verticle-a": {
      "someGlobalConfig": "$ref:globalConfig",
      "anotherConfig" : "$ref:components.aConfig",
      "anotherConfigKey" : "$ref:components.aConfig.anotherKey",
      ....
    },
    "aConfig" : {
     "aKey" : "aValue",
     "anotherKey": "anotherValue"
    }
  },
  "globalConfig" : {
    "a" : "value",
    "b": "value1"
  }
}
```

In result, "verticle-a-id" verticle will get it's configuration from "components.verticle-a" object and resolved json path references.

<a id="di-config-inject"></a>
### Injecting configuration to ComponentVerticle or ServiceVerticle

You can keep verticle's configuration next to its deployment options.

```json
{
  "registry:components": {
    "verticle-a-id": {
      "main": "com.example.ServiceVerticleA",
      "verticleConfig": {
        "ttl": 1000
      }
    }
  }
}
```

In result, "verticle-a-id" verticle is configured with `{ "ttl": 1000 }`.

<a id="di-impls"></a>
### Deploying multiple implementations of VertxEndpoint

`ServiceVerticle.vertxServiceAddressPrefix()` method allows to deploy multiple verticles implementing the same `@VertxEndpoint` interface.
Instead of setting the address value programmatically we can use verticle's id defined in `Registry` configuration.
To do so set `prefix` attribute to true:

```json
{
  "registry:components": {
    "verticle-a-id": {
      "main": "com.example.ServiceVerticleA",
      "prefix": true
    }
  }
}
```

In this case `vertxServiceAddressPrefix()` returns `verticle-a-id`.

Alternatively, we can set `prefix` to custom address:

```json
{
  "registry:components": {
    "verticle-a-id": {
      "main": "com.example.ServiceVerticleA",
      "prefix": "address-prefix"
    }
  }
}
```

<a id="di-deployment-opts"></a>
### Defining custom DeploymentOptions
You can make the RegistryVerticle to deploy verticle using custom io.vertx.core.DeploymentOptions defined in configuration file. To do so add JSON object "options" key at the level of "main" key.

For example, let's deploy 4 instances ServiceVerticleA:

```json
{
  "registry:components": {
    "verticle-a-id": {
      "main": "com.example.ServiceVerticleA",
      "options": {
        "instances": 4
      }
    }
  }
}
```

<a id="di-disable"></a>
### Disabling verticle
You can skip deployment of a verticle defined in registry. To do so, set `disabled` flag to true.

E.g.
```json
{
  "registry:components": {
    "verticle-a-id": {
      "main": "com.example.ServiceVerticleA",
      "disabled": true
    }
  }
}
```

<a id="server"></a>
## Serving HTTP requests and ApiServer

`vertx-server` gives you easy way to define HTTP APIs. You've already seen in TODO <<Step by step setup>> section how to configure
and implement simple HTTP route. Once you created and deployed `RouteVerticle` you need to implement `handle(RoutingContext)` method.
The [vertx-web docs](http://vertx.io/docs/vertx-web/java) will guide you how to do it (you may want to cut
to the [chase](http://vertx.io/docs/vertx-web/java/#_handling_requests_and_calling_the_next_handler)).
Let's focus on routes configuration now.

<a id="server-routes"></a>
### Routes configuration

```json
{
  "apiServer": {
    ...
    "routes": [
      {
        "id": "route-id",
        "handler": "route-handler", // optional, defaults to value in 'id' attribute
        "method": "GET",
        "urlPath": "/hello",
        "skipBodyHandler": false // optional, default value 'false'
      }
    ]
  },
  "registry:routes": {
    "route-handler": { "main": ... }
  },
  ...
}
```

Route configuration has following fields:

* id - route identifier
* handler - defines what RouteVerticle defined in "registry:routes" handles the route, optional, defaults to value in 'id' attribute
* method - HTTP method of the route
* urlPath - path of the route
* skipBodyHandler - defines whether `io.vertx.ext.web.handler.BodyHandler` should be registered on the route, optional, default value `false`

Route configuration is used to register `io.vertx.ext.web.Route` using `io.vertx.ext.web.Router.route(HttpMethod, String)`.
If method attribute in configuration is missing then `Router.route(String)` method is used instead (effectively the Route matches all requests with given urlPath regardless HTTP method).

#### Base path

You can set server's base path using 'basePath' attribute, e.g.:

```json
{
  "apiServer": {
    ...
    "basePath": "/api"
    "routes": [
      {
        "id": "route-id",
        "method": "GET",
        "urlPath": "/hello"
      },
      {
        "id": "other-route-id",
        "method": "GET",
        "urlPath": "/hi"
      }
    ]
  },
  ...
}
```

The above configuration defines two routes that will be exposed at `/api/hello` and `/api/hi` paths.

#### Adding/disabling routes with `classpath` vertx-store

If you are using `classpath` vertx-store then you might need to modify the default routes configuration.
To avoid overriding entire `routes` array you can use `disabledRoutes` and `appendRoutes` or `prependRoutes` attributes.

The reason for having separate `appendRoutes` and `prependRoutes` is that Vertx routes are being matched sequentially,
so you may want to execute some routes before or after others. The final list of routes consists of `prependRoutes`, `routes` and `appendRoutes`.

E.g. let's disable "route-id" route:

```json
{
  "apiServer": {
    ...
    "routes": [
      {
        "id": "route-id",
        ...
      }
    ]
  },
  "disabledRoutes": [ "route-id" ]
  ...
}
```

E.g. let's append extra route:

```json
{
  "apiServer": {
    ...
    "routes": [
      ...
    ]
  },
  "appendRoutes": [
    {
      "id": "extra-route-id",
      "method": "GET",
      "urlPath": "/extra/hello"
    }
  ]
  ...
}
```

<a id="server-filters"></a>
### HTTP filters

If you want to apply HTTP filters to your route you need to add filter configuration in `filters` attribute, e.g.:

```json
{
  "apiServer": {
    ...
    "routes": [
      {
        "id": "route-id",
        "method": "GET",
        "urlPath": "/hello",
        "filters": [ "my-filter" ]
      }
    ]
  }
  ...
}
```

Make sure that `registry:filters` contains all the filters you need, e.g.:

```json
{
  "registry:filters": {
    "my-filter": { "main": "com.example.MyFilter" }
  }
  ...
}
```

If the filter you use has some configuration you can pass it in instead of filter name in `filters` attribute following way:

```json
{
  "apiServer": {
    ...
    "routes": [
      {
        "id": "route-id",
        "method": "GET",
        "urlPath": "/hello",
        "filters": [
          {
            "name": "my-filter",
            "conf": { "param": "value" }
          }
        ]
      }
    ]
  }
  ...
}
```

#### Implementing HTTP filters (low-level)

HTTP filter is a ServiceVerticle that implements `com.cloudentity.tools.vertx.server.api.filters.RouteFilter` interface.

RouteFilter has two methods:

```java
public interface RouteFilter {
  @VertxEndpoint
  Future applyFilter(RoutingContext ctx, String rawJsonConf);

  @VertxEndpoint
  Future<RouteFilterConfigValidation> validateConfig(String rawJsonConf);
}
```

In `applyFilter` implement you filtering logic. Remember to call `RoutingContext.next()` to pass the context to next filter or `RouteVerticle` for handling.
`validateConfig` is invoked at app startup to validate configuration of all applications of the filter.
If some configurations are invalid then the app fails to start.

The configuration is sent as string-representation of JSON.

E.g. `rawJsonConf` contains `null` with following configuration:

```json
...
"routes": [
 {
   ...
   "filters": ["my-filter"]
 }
]
...
```

E.g. `rawJsonConf` contains `{ "param": "value" }`  with following configuration:

```json
...
"routes": [
  {
    ...
    "filters": [
      {
        "name": "my-filter",
        "conf": { "param": "value" }
      }
    ]
  }
]
...
```

E.g. `rawJsonConf` contains `"param"` with following configuration:

```json
...
"routes": [
  {
    ...
    "filters": [
      {
        "name": "my-filter",
        "conf": "param"
      }
    ]
  }
]
...
```

#### Implementing HTTP filters (Scala)

You can use `ScalaRouteFilterVerticle` as a base for your filter.
Let's implement a filter that returns 401 if "role" header doesn't contain configured value.

Configuration looks like this:

```json
...
"routes": [
  {
    ...
    "filters": [
      {
        "name": "role-security",
        "conf": {
          "role": "admin"
        }
      }
    ]
  }
]
...
```

Now we can implement our `RoleSecurityFilter`:

```java
import io.circe.generic.semiauto._

case class RoleSecurityFilter(role: String)

class RoleSecurityFilter extends ScalaRouteFilterVerticle[RoleSecurityFilter] with RouteFilter {
  override def confDecoder: Decoder[RoleSecurityFilter] = deriveDecoder[RoleSecurityFilter]

  override def filter(ctx: RoutingContext, conf: RoleSecurityFilter): Unit =
    if (conf.role == ctx.request().getHeader("role")) {
      ctx.next()
    } else {
      ctx.response().setStatusCode(401).end()
    }

  override def checkConfigValid(conf: RoleSecurityFilter): RouteFilterConfigValidation =
    if (conf == "admin" || conf == "user") RouteFilterConfigValidation.success()
    else                                   RouteFilterConfigValidation.failure(s"Invalid role '${conf.role}'")
}
```

`ScalaRouteFilterVerticle` is generic with regard to type of configuration.
We need to define configuration decoder in `confDecoder` and implement `filter` and `checkConfigValid` methods using decoded configuration.

If your filter does not accept configuration use `Unit` as type of configuration and `io.circe.Decoder.decodeUnit` as `confDecoder`.

Note: `ScalaRouteFilterVerticle` caches decoded configuration.

<a id="server-config"></a>
### HTTP server configuration
vertx-server uses `io.vertx.core.http.HttpServer` as underlying implementation.
You can provide its `io.vertx.core.http.HttpServerOptions` in "apiServer.http" configuration.

For example, let's define that the HTTP server starts on port 8081 and binds to localhost:

```json
{
  "apiServer": {
    "http": {
      "port": 8081,
      "host": "localhost"
    },
    "routes": [
      {
      ...
    ]
  },
  ...
}
```

<a id="server-multiple"></a>
### Deploying multiple servers

By default configuration of API server and its routes and filters are at `apiServer`, `registry:routes` and `registry:filters` configuration paths.
If you want to deploy another API Server you can do that programmatically using `com.cloudentity.tools.vertx.server.api.ApiServerDeployer.deploy(Vertx, String)` method.
The second argument is verticle id of the API server.

```java
static void deployApiServer(Vertx vertx) {
  ApiServerDeployer.deploy(vertx, "anotherApiServer");
}
```

the configuration should look like this:

```json
{
  "anotherApiServer": {
    "routesRegistry": "another-routes",
    "filtersRegistry": "another-filters",
    "http": {
      "port": 8082,
      "host": "localhost"
    },
    "routes": [
      {
      ...
    ]
  },

  ...

  "registry:another-routes": {
    ...
  },
  "registry:another-filters": {
    ...
  }
}
```

`anotherApiServer.routesRegistry` and `anotherApiServer.filtersRegistry` attributes define names of the corresponding registries.

<a id="examples"></a>
## Examples

<a id="examples-route"></a>
### Create RouteVerticle with configuration access
We gonna create RouteVerticle that serves static content read from configuration.

Add route definition at "apiServer.routes" in config.json:

```json
{
  "id": "accessing-configuration-route",
  "method": "GET",
  "urlPath": "/config"
}
```

1. Set any JSON object as route configuration at "accessing-configuration-route" in config.json:

```json
...
"accessing-configuration-route": {
  "content": "Hello world!"
}
...
```

2. Create AccessingConfigurationRoute verticle:

```java
package examples.app.routes;
...
public class AccessingConfigurationRoute extends RouteVerticle {
  @Override
  public void handle(RoutingContext ctx) {
    String content = getConfig().getString("content");
    ctx.response().setStatusCode(200).end(content);
  }
}
```

3. Add AccessingConfigurationRoute to "registry:routes" in config.json:

```json
...
"registry:routes": {
  "accessing-configuration-route": { "main": "examples.app.routes.AccessingConfigurationRoute" }
}
...
```
4. RouteVerticle injection

Final config.json:

```json
{
  "apiServer": {
    "http": {
      "port": 8081
    },
    "routes": [
      {
        "id": "accessing-configuration-route",
        "method": "GET",
        "urlPath": "/config"
      }
    ]
  },
  "registry:routes": {
    "accessing-configuration-route": { "main": "examples.app.routes.AccessingConfigurationRoute" }
  },
  "accessing-configuration-route": {
    "content": "Hello world!"
  }
}
```

<a id="examples-route-service"></a>
### Create RouteVerticle passing request param to ServiceVerticle
We gonna create a RandomGenerator service that generates random integer smaller than value given as method argument. Next, we create a RouteVerticle that reads request path parameter and passes it to RandomGenerator to generate value.

1. Add route definition at "apiServer.routes" in config.json:

```json
{
  "id": "calling-singleton-route",
  "method": "GET",
  "urlPath": "/random/:max"
}
```

2. Add CallingSingletonServiceRoute to "registry:routes" in config.json:

```json
...
"registry:routes": {
  "calling-singleton-route": { "main": "examples.app.routes.CallingSingletonServiceRoute" }
}
...
```

3. Create RandomGenerator service:

```java
package examples.app.components;
...
public interface RandomGeneratorService {
  @VertxEndpoint
  Future<Integer> generate(int max);
}
...
public class RandomGenerator extends ServiceVerticle implements RandomGeneratorService {
  @Override
  public Future<Integer> generate(int max) {
    return Future.succeededFuture(new Random().nextInt(max));
  }
}
```

4. You can deploy RandomGenerator manually or use RegistryVerticle - the latter is preferred.
4.1 Deploy RandomGenerator in bootstrap class:

```java
public class App extends VertxBootstrap {
  @Override
  protected Future beforeServerStart() {
   return VertxDeploy.deploy(vertx, new RandomGenerator());
  }
}
```

4.2 Deploy RandomGenerator using RegistryVerticle:

4.2.1 Add "registry:components" entry in config.json and add ServiceVerticle:

```json
...
"components:registry": {
  "random-generator": { "main": "examples.app.components.RandomGenerator" }
}
...
```

4.2.2 Deploy "components:registry" in bootstrap class:

```java
public class App extends VertxBootstrap {
  @Override
  protected Future beforeServerStart() {
   return VertxDeploy.deploy(vertx, new RegistryVerticle(new RegistryType("components")));
  }
}
```

5. Create CallingSingletonServiceRoute verticle:

```java
package examples.app.routes;

public class CallingSingletonServiceRoute extends RouteVerticle {
  private static final Logger log = LoggerFactory.getLogger(CallingSingletonServiceRoute.class);

  private RandomGeneratorService client;

  @Override
  protected void initService() {
    client = createClient(RandomGeneratorService.class);
  }

  @Override
  public void handle(RoutingContext ctx) {
    int max = Integer.valueOf(ctx.request().getParam("max"));

    client.generate(max).setHandler(async -> {
      if (async.succeeded()) {
       ctx.response().setStatusCode(200).end(async.result().toString());
      } else {
       log.error("Could not generate random value", async.cause());
       ctx.response().setStatusCode(500).end();
      }
    });
  }
}
```

Final config.json:

```json
{
  "apiServer": {
   "http": {
    "port": 8081
   },
   "routes": [
    {
      "id": "calling-singleton-route",
      "method": "GET",
      "urlPath": "/random/:max"
    }
   ]
  },
  "registry:routes": {
   "calling-singleton-route": { "main": "examples.app.routes.CallingSingletonServiceRoute" }
  },
  "registry:components": {
   "random-generator": { "main": "examples.app.components.RandomGenerator" }
  }
}
```

<a id="examples-route-service-multiple"></a>
### Create RouteVerticle dispatching request to one of multiple VertxEndpoint implementations

We gonna create a DateTimeGenerator service that returns string representation of current date-time. It reads the timezone from configuration. There will be two instances of DateTimeGenerator: GMT and Europe/Warsaw. The RouteVerticle will decide what time generator should be used based on the request parameter.

1. Add route definition at "apiServer.routes" in config.json:

```json
{
  "id": "calling-non-singleton-route",
  "method": "GET",
  "urlPath": "/date-time/:timer"
}
```

2. Add CallingNonSingletonServiceRoute to "registry:routes" in config.json:

```json
...
"registry:routes": {
  "calling-non-singleton-route": { "main": "examples.app.routes.CallingNonSingletonServiceRoute" }
}
...
```

3. Create DateTimeGeneratorVerticle. To be able to differentiate between different instances of the same ServiceVerticle we need to return event-bus address prefix in ServiceVerticle.vertxServiceAddressPrefix():

```java
package examples.app.components;

...
public interface DateTimeGeneratorService {
  @VertxEndpoint
  Future<String> generate();
}
...
public class DateTimeGeneratorVerticle extends ServiceVerticle implements DateTimeGeneratorService {
   private ZoneId zone;

   @Override
   public Future<String> generate() {
     ZonedDateTime now = ZonedDateTime.now(zone);
     return Future.succeededFuture(now.toString());
   }

   @Override
   protected void initService() {
     String zoneIdString = getConfig().getString("zoneId");
     zone = ZoneId.of(zoneIdString);
   }

   @Override
   protected Optional<String> vertxServiceAddressPrefix() {
     return Optional.ofNullable(verticleId());
   }
 }
```

4. Add "registry:timers" entry in config.json:

```json
...
"components:timers": {
   "gmt-timer": { "main": "examples.app.components.DateTimeGeneratorVerticle" },
   "cest-timer": { "main": "examples.app.components.DateTimeGeneratorVerticle" }
 }
...
```

5. Deploy "components:timers" in bootstrap class:

```java
public class App extends VertxBootstrap {
   @Override
   protected Future beforeServerStart() {
     return VertxDeploy.deploy(vertx, new RegistryVerticle(new RegistryType("timers")));
   }
 }
```

6. Add configuration for "gmt-timer" and "cest-timer" verticles:

```json
{
  "gmt-timer": {
    "zoneId": "GMT"
  },
  "cest-timer": {
    "zoneId": "Europe/Warsaw"
  }
}
```

7. Create CallingNonSingletonServiceRoute verticle:

```java
package examples.app.routes;

public class CallingNonSingletonServiceRoute extends RouteVerticle {
   private static final Logger log = LoggerFactory.getLogger(CallingSingletonServiceRoute.class);

   private ServiceClientsRepository<DateTimeGeneratorService> clientRepo;

   @Override
   protected Future initServiceAsync() {
     return ServiceClientsFactory.build(vertx.eventBus(), "timers", DateTimeGeneratorService.class)
       .map((repo) -> clientRepo = repo);
   }

   @Override
   public void handle(RoutingContext ctx) {
     String timer = ctx.request().getParam("timer");
     DateTimeGeneratorService client = clientRepo.get(timer);
     if (client != null) {
       client.generate()
         .setHandler(async -> {
           if (async.succeeded()) {
             ctx.response().setStatusCode(200).end(async.result());
           } else {
             log.error("Could not generate date-time value", async.cause());
             ctx.response().setStatusCode(500).end();
           }
         });
     } else {
         ctx.response().setStatusCode(400).end("Time generator not found");
     }
   }
 }
```

Final config.json:

```json
{
  "apiServer": {
    "http": {
      "port": 8081
    },
    "routes": [
      {
        "id": "calling-non-singleton-route",
        "method": "GET",
        "urlPath": "/date-time/:timer"
      }
    ]
  },
  "registry:routes": {
    "calling-non-singleton-route": { "main": "examples.app.routes.CallingNonSingletonServiceRoute" }
  },
  "registry:timers": {
    "gmt-timer": { "main": "examples.app.components.DateTimeGeneratorVerticle" },
    "cest-timer": { "main": "examples.app.components.DateTimeGeneratorVerticle" }
  },
  "gmt-timer": {
   "zoneId": "GMT"
 },
 "cest-timer": {
   "zoneId": "Europe/Warsaw"
 }
}
```