# DATAEDGE SERVICE

This is the Edge Server (DATAEDGE). It enforces user authentication for some endpoints by requiring an EGA Bearer Token for API access. DATAEDGE is the service providing streaming file downloads and provides the back end for a FUSE layer implementation. 

Data downloads can be served as whole-file downloads, or be specifying byte ranges of the underlying file. It provide direct access to EGA archive files, via ega-data-api-res service. This service offers endpoints secured by OAuth2 tokens for direct access to files, and unsecured endpoints for downloading request tickets (which is the main functionality).
