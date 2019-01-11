# RES(RE-ENCRYPTION) SERVICE

RES incorporates all cryptographic services required by EGA and can read input streams (and produce output streams) encrypted using:

* GPG Public Key	{“semi-random-access”}
* GPG Symmetric Key 	{“semi-random-access”}
* AES 128-Bit
* AES 256-Bit
* Plain (Unencrypted)

Plain and AES-encrypted input streams can be accessed at a byte-level. Currently two input stream sources are supported: file path and http, to support files stored in file systems as well as object stores (Cleversafe). GPG-encrypted input streams can still be “sliced” - the mechanism allowing this form of access has to read/decrypt a file from start until the specified start coordinate, and then the specified number of bytes are read and returned. Bytes can be ‘skipped’ there is no true ‘seek’ functionality. This is slow, but may still be beneficial in saving data transfer volume.

RES uses FILEDATABASE to translate an EGAF file ID into an access URL so that archive files can be accessed directly by ID rather than requiring an absolute file locator.

Each request returns a special header field “X-Session” which contains a UUID and identifies this file transfer. Clients can request server statistics (e.g. MD5s, sizes) from the server after the transfer is completed using the /session endpoint with the session UUID. Further in the future this should allow websocket connections for higher-performance interactions with archived files. This functionality is currently not used.

Profiles available:

| Profile name (Interface) | Usage |
| ------------- | ------------- |
| enable-filesystem-based-archive | Gets file information from FILEDATABASE and key information from KEYSERVICE . The default is to get it from Cleversafe |
| LocalEGA (ArchiveService) | Gets file information from FILEDATABASE and key information from KEYSERVICE (The difference is how the key for the file is obtained get the master key from KEYSERVER and then decrypt draft Crypt4gh header) |
| LocalEGA  (ResService) |  Transfers the file stored in draft Crypt4gh file format |
| log-transfer | Enables log functionality of the transfers. Currently has to be used with repo-logger db-repo-logger |
| repo-logger | Logs the transfer information to a repository |
| db-repo-logger  | Logs the transfer information to the database |