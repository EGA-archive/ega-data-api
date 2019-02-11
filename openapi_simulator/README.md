REST API Simulator
==================

The simulator uses the imposter "scriptable, multipurpose mock server" (https://github.com/outofcoffee/imposter/) to create mock services from OpenAPI specifications. To allow the services to be run on different ports (and not just different REST enpoints) a separate imposter instance is required for each service.
Currently the services are defined by an OpenAPI 2.0 (swagger) specification, along with an imposter json configuration file on the format:
```
{
  "plugin": "com.gatehill.imposter.plugin.openapi.OpenApiPluginImpl",
  "specFile": "<openapi>.yaml"
}
```
More options can be added to the imposter configuration to control things such as mock replies on REST enpoints, as described at https://github.com/outofcoffee/imposter/blob/master/docs/configuration.md.
