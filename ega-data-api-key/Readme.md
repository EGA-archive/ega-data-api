# KEY SERVICE

This is a standalone Encryption Key server. This is a service to abstract encryption key management: this service returns a key for a file ID. The purpose of this server is to abstract customised key management solutions, so that RES service can make a standardized REST call to obtain the encryption key for an archived file, while allowing local installations to choose their own key management strategies. It is available via EUREKA using the service name "KEY".

This service provides a very basic abstraction to handle encryption keys. Each installation will have to assess the security needs for this service. It should run in a private area shielded from outside access. Only RES should access this service.

This service uses a separate configuration XML to describe the keys used.

This service can be used (developed into) either as a proxy to existing key management systems, or into its own key management solution.


