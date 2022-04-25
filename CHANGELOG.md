## [1.11.0] - 2022-04-25
### Removed
- Removed Cassandra references in README

### Security
- [CVE-2020-13949](https://nvd.nist.gov/vuln/detail/CVE-2020-13949)
  - Solved by upgrading jaeger-client 1.2.0 -> 1.8.0 (transitively libthrift 0.13.0 -> 0.14.1)
  - Avoiding importing tomcat-embed-core 8.5.46, with its new vulnerabilities, by setting overriding libthrift -> 0.16.0
  - Upgraded okhttp mockwebserver version from 3.14.9 to 4.9.0 to match that imported by jaeger 1.8.0
- [CVE-2020-29582](https://nvd.nist.gov/vuln/detail/CVE-2020-29582)
  - Solved by upgrading kotlin-stdlib and kotlin-stdlib-common from 1.3.50 -> 1.4.32
  - Note that kotlin-stdlib 1.4.10 and kotlin-stdlib-common 1.4.0 would have been imported by okhttp 4.9.0 above
- [CVE-2022-21653](https://nvd.nist.gov/vuln/detail/CVE-2022-21653)
  - Upgrading circe 0.11.1 to 0.14.1 to move keep transient scala-reflect vulnerabilities in test scope - moves jawn-parser 0.14.1 -> 1.1.2
  - Upgrading jawn-parser -> 1.3.2 to solve vulnerability
- [CVE-2018-12541](https://nvd.nist.gov/vuln/detail/CVE-2018-12541) - Solved by upgrading vertx 3.9.5 -> 3.9.12 (transitively netty-transport 4.1.49 -> 4.1.72)
- [CVE-2021-21290](https://nvd.nist.gov/vuln/detail/CVE-2021-21290) - Solved by upgrading vertx 3.9.5 -> 3.9.12 (transitively netty-transport 4.1.49 -> 4.1.72)
- [CVE-2021-21295](https://nvd.nist.gov/vuln/detail/CVE-2021-21295) - Solved by upgrading vertx 3.9.5 -> 3.9.12 (transitively netty-transport 4.1.49 -> 4.1.72)
- [CVE-2021-21409](https://nvd.nist.gov/vuln/detail/CVE-2021-21409) - Solved by upgrading vertx 3.9.5 -> 3.9.12 (transitively netty-transport 4.1.49 -> 4.1.72)
- [CVE-2021-37136](https://nvd.nist.gov/vuln/detail/CVE-2021-37136) - Solved by upgrading vertx 3.9.5 -> 3.9.12 (transitively netty-transport 4.1.49 -> 4.1.72)
- [CVE-2021-37137](https://nvd.nist.gov/vuln/detail/CVE-2021-37137) - Solved by upgrading vertx 3.9.5 -> 3.9.12 (transitively netty-transport 4.1.49 -> 4.1.72)
- [CVE-2021-43797](https://nvd.nist.gov/vuln/detail/CVE-2021-43797) - Solved by upgrading vertx 3.9.5 -> 3.9.12 (transitively netty-transport 4.1.49 -> 4.1.72)
- [CVE-2021-29425](https://nvd.nist.gov/vuln/detail/CVE-2021-29425) - Solved by upgrading commons-io 2.5 -> 2.11.0
- [CVE-2020-17521](https://nvd.nist.gov/vuln/detail/CVE-2020-17521) - Solved by upgrading rest-assured 3.3.0 -> 4.5.1 (transitively groovy 2.4.15 -> 3.0.9, httpclient 4.5.3 -> 4.5.13)
- [CVE-2020-13956](https://nvd.nist.gov/vuln/detail/CVE-2020-13956) - Solved by upgrading rest-assured 3.3.0 -> 4.5.1 (transitively groovy 2.4.15 -> 3.0.9, httpclient 4.5.3 -> 4.5.13)

## [1.10.0] - 2021-11-17
### Changed
- Activated httpClient logs - will show up in debug level 

## [1.9.0] - 2021-06-11
### Added
- spring-like references can use environment variables from 'env' config fallback

## [1.8.0] - 2021-05-07
### Fixed
- Deploy registries sequentially for VertxModuleTest

## [1.7.0] - 2021-04-12
### Added
- Support base64 encoding and decoding of referenced values

### Changed
- On deployment failure exit with status 15
- Switched off running OWASP plugin by default

## [1.6.0] - 2021-02-26
### Added
- ConsulSdRegistrar supports dynamic port
- ConsulSdRegistrar module deploys 'system-ready' registry
- Ability to override name of the service config for vertx service tests

### Changed
- Deploying 1 instance of ApiServer instead of 2*CPUs (vertx-server) - improves performance + allows using dynamic port
- VertxBootstrap starts 'system-init' registry before server start and 'system-ready' after server start (vertx-server)
- SmartHttpClientImpl body stream resetting order changed in case of exception

## [1.5.0] - 2021-02-04
### Added
- GetConfigRoute implementation returning masked global config

### Breaking changes
- ComponentVerticle.registerSelfConfChangeListener accepts Handler<ConfigChanged> instead of Handler<JsonObject>

### Changed
- Version upgrade (vertx 3.9.4 -> 3.9.5)

### Security
- [CVE-2019-17640](https://nvd.nist.gov/vuln/detail/CVE-2019-17640) - Fixed by upgrading vertx to 3.9.5

## [1.4.0] - 2020-12-30
### Added
- Support for escape character \\ for dots in path in json extractor

## [1.3.0] - 2020-12-01
### Added
- capability to enable micrometrics configs for jmx, influx and prometheus
- spring-like conf reference
- optional casting in configuration reference
- For VertxModuleTest, added new deployment method which allows supplying custom set of config stores
- consul-sd-registrar support IP/hostname discovery

### Fixed
- sd-provider/consul and sd-provider/static modules deployment race condition

### Changed
- default for SD registration params
- switch to unordered blocking execution
- Version upgrade (vertx 3.9.1 -> 3.9.4) and other supporting libs
- switched to generic project suppression file and updated dependency check plugin version

### Security
- [CVE-2017-18640](https://nvd.nist.gov/vuln/detail/CVE-2017-18640) - Fixed by upgrading vertx to 3.9.4
- [CVE-2018-20200](https://nvd.nist.gov/vuln/detail/CVE-2018-20200) - Fixed by upgrading okhttp3 version to 3.14.9

## [1.2.0] - 2020-06-09
### Added
- ext config stores can control 'ssl' flag of underlying store with 'scheme' attribute
- env fallback reference
- VertxModuleTest
- registry deploys verticles applying 'dependsOn' attribute
- VertxExecutionContext extends Executor
- shared-local-map config store

### Changed
- ServiceClientFactory deprecated by VertxEndpointClient
- ComponentVerticle does not print stacktrace when TracingVerticle not available
- vertx upgraded to 3.9.1
- jackson upgraded to 2.10.2 (jackson-databind exclusions removed)

## [1.1.0] - 2020-04-20
### Added
- module instance support
- smart http client body streaming and addHeader method
- 'enabled' flag in verticle registry
- plugins to build and upload BOM files

### Changed
- registry logger name contains registry type

### Fixed
- registry configuration change listener handles missing config object gracefully
- smart http client supports transfer-encoding chunked
- smart http client copies all header values per key

### Security
- [CVE-2019-14540](https://nvd.nist.gov/vuln/detail/CVE-2019-14540) - Fixed by upgrading jackson-databind 2.9.9.3 -> 2.9.10.3
- [CVE-2019-14892](https://nvd.nist.gov/vuln/detail/CVE-2019-14892) - Fixed by upgrading jackson-databind 2.9.9.3 -> 2.9.10.3
- [CVE-2019-14893](https://nvd.nist.gov/vuln/detail/CVE-2019-14893) - Fixed by upgrading jackson-databind 2.9.9.3 -> 2.9.10.3
- [CVE-2019-16335](https://nvd.nist.gov/vuln/detail/CVE-2019-16335) - Fixed by upgrading jackson-databind 2.9.9.3 -> 2.9.10.3
- [CVE-2019-16942](https://nvd.nist.gov/vuln/detail/CVE-2019-16942) - Fixed by upgrading jackson-databind 2.9.9.3 -> 2.9.10.3
- [CVE-2019-16943](https://nvd.nist.gov/vuln/detail/CVE-2019-16943) - Fixed by upgrading jackson-databind 2.9.9.3 -> 2.9.10.3
- [CVE-2019-17267](https://nvd.nist.gov/vuln/detail/CVE-2019-17267) - Fixed by upgrading jackson-databind 2.9.9.3 -> 2.9.10.3
- [CVE-2019-17531](https://nvd.nist.gov/vuln/detail/CVE-2019-17531) - Fixed by upgrading jackson-databind 2.9.9.3 -> 2.9.10.3
- [CVE-2019-20330](https://nvd.nist.gov/vuln/detail/CVE-2019-20330) - Fixed by upgrading jackson-databind 2.9.9.3 -> 2.9.10.3
- [CVE-2020-8840](https://nvd.nist.gov/vuln/detail/CVE-2020-8840) - Fixed by upgrading jackson-databind 2.9.9.3 -> 2.9.10.3
- [CVE-2019-0205](https://nvd.nist.gov/vuln/detail/CVE-2019-0205) - Fixed by upgrading jaeger 0.35.5 -> 1.2.0
- [CVE-2019-0210](https://nvd.nist.gov/vuln/detail/CVE-2019-0210) - Fixed by upgrading jaeger 0.35.5 -> 1.2.0

## [1.0.0] - 2019-11-27
### Added
- Initial version
