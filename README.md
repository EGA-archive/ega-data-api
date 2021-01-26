# Repository for the EGA DATA project

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
$ mvn clean install
```

To build and push the docker images enable the profile `production` or `debug` respectively
```
$ mvn clean install -P production
```
If you wish to build the images but not push them, add the parameter `-DskipDockerPush`

## License

This project is licensed under the Apache 2.0 License - see the [LICENSE.md](LICENSE.md) file for details
