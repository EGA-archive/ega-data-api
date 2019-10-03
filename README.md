# Repository for the EGA DATA project

[![Build Status](https://travis-ci.org/neicnordic/ega-data-api.svg?branch=master)](https://travis-ci.org/neicnordic/ega-data-api)

The EGA Data API is a REST API providing secure and controlled access to the archive. Access is secured by OAuth2 Bearer tokens issued from the the EGA OpenID Connect AAI. The EGA Data API implements the GA4GH Streaming/htsget API as it is specified here http://samtools.github.io/hts-specs/htsget.html. The API supports BAM, CRAM, VCF, BCF files.


# Architecture

Ega Data API is divided into several microservices:

| Services | Role |
| ------------- | ------------- |
| Dataedge  | It enforces user authentication for some endpoints by requiring an EGA Bearer Token for API access |
| Htsget  | Provides htsget server functionality |
| Filedatabase  | This service is an abstraction layer to the local database, which keeps information about files and datasets, as well as some basic logging tables |
| Key  | Handles encryption keys |
| Netflix config  | Stores the configuration files(`application.properties`) for other services |
| Netflix eureka  | Netflix's service discovery system |
| Res(Re-encryption)  | Reads archived data and produce output stream |

# Dependencies
* Maven
* Java8

# Build
```
$ mvn clean install -DskipDockerPush
```

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE.md](LICENSE.md) file for details
