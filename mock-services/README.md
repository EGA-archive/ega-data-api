REST API Simulator
==================

The simulator uses the imposter "scriptable, multipurpose mock server" (https://github.com/outofcoffee/imposter/) to create mock services from OpenAPI specifications. To allow the services to be run on different ports (and not just different REST enpoints) a separate imposter instance is required for each service.
Currently the services are defined by an OpenAPI 2.0 (swagger) specification, along with an imposter json configuration file on the format:
```
{
  "plugin": "com.gatehill.imposter.plugin.openapi.OpenApiPluginImpl",
  "specFile": "<openapi>.yaml",
  "response": {
    "scriptFile": "<script-name>.groovy"
  }
}
```
More options can be added to the imposter configuration, as described at https://github.com/outofcoffee/imposter/blob/master/docs/configuration.md. To start the mock server run `./mock.sh start all`, and then navigate a web browser to http://localhost:<port>/_spec/, where port is given in the OpenAPI specifications (and will be listed when you start the mock services, or run `./mock.sh list`).
