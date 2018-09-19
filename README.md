# Repository for the EGA DATA project

[![Build Status](https://travis-ci.org/EGA-archive/ega-data-api.svg?branch=master)](https://travis-ci.org/EGA-archive/ega-data-api)


The EGA Data API is a REST API providing secure and controlled access to the archive. Access is secured by OAuth2 Bearer tokens issued from the the EGA OpenID Connect AAI. In the future (or upon request) EGA users may link their ELIXIR and EGA identities, to also allow access with OAuth2 tokens issued by the ELIXIR AAI.

The EGA Data API implements the GA4GH Streaming/htsget API as it is specified here http://samtools.github.io/hts-specs/htsget.html. The API supports BAM, CRAM, VCF, BCF files.


NOTE: The URL in the property source name is the git repository, not the config server URL.

| First Header  | Second Header |
| ------------- | ------------- |
| Content Cell  | Content Cell  |
| Content Cell  | Content Cell  |


