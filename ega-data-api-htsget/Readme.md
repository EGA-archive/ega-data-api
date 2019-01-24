# Htsget SERVICE

This is the Htsget  Server (Htsget). It enforces user authentication for some endpoints by requiring an EGA Bearer Token for API access. Provides the htsget api http://samtools.github.io/hts-specs/htsget.html 


Profiles available:

| Profile name | Usage |
| ------------- | ------------- |
|enable-aai  | Enables 2 external identity providers  |
|enable-single-aai  | Enables 1 external identity provider | 
|external-permissions  | Instead of the default behaviour of checking the permissions against the FILEDATABASE for permissions, this profile uses an external service to do that |
|logger-log  | Logs the downloads and events to the configured log instead of the default restcall to the FILEDATABASE service |
|add-user-ip-headers  | Adds an interceptor so that any outgoing calls made with  RestTemplate will add the user and the ip to the headers of the call |
|no-oss | disables eureka client |