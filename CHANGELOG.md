## [Unreleased]
### Added
- module instance support
- smart http client body streaming and addHeader method
- 'enabled' flag in verticle registry
- plugins to build and upload BOM files
- ext config stores can control 'ssl' flag of underlying store with 'scheme' attribute

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
