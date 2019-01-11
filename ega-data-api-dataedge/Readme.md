# DATAEDGE SERVICE

This is the Edge Server (DATAEDGE). It enforces user authentication for some endpoints by requiring an EGA Bearer Token for API access. DATAEDGE is the service providing streaming file downloads and provides the back end for a FUSE layer implementation. 

Data downloads can be served as whole-file downloads, or be specifying byte ranges of the underlying file. It provide direct access to EGA archive files, via ega-data-api-res service. This service offers endpoints secured by OAuth2 tokens for direct access to files, and unsecured endpoints for downloading request tickets (which is the main functionality).

Profiles available:

| Profile name | Usage |
| ------------- | ------------- |
|enable-aai  | Enables 2 external identity providers  |
|enable-single-aai Enables 1 external identity provider | 
|external-permissions  | Instead of the default behaviour of checking the permissions against the FILEDATABASE for permissions, this profile uses an external service to do that |
|logger-log  | Logs the downloads and events to the configured log instead of the default restcall to the FILEDATABASE service |
|add-user-ip-headers  | Adds an interceptor so that any outgoing calls made with  RestTemplate will add the user and the ip to the headers of the call |