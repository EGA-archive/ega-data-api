# CONFIG SERVICE

This is a standalone Spring Configuration server. It is configured to run on port 8888. The correct URL to this service must be specified in all microservices using the configuration server. 

There are no dependencies.

This service provides `application.properties` files to microservices using the configuration server. This allows for centralised configuration management for multiple microservices, and it removes any configuration files from the source code.

The `application.properties` files are configured to be stored in directory `/config` on the server hosting the configuration service. The name for the properties file of a microservice named `{app.name}` is `"{app.name}.properties"`
