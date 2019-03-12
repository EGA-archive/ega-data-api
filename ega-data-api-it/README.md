# Integration test for EGA-DATA-API (Rest-Assured Automation)
This is an application of Rest-Assured as a basis for API test framework. 


## Framework

### Structure
This project is your standard Maven Java project with `src` folders and `POM.xml`.

### Properties
`src/main/resources/init.sql` is a simple sql file containing the schema, table & default values. This sql should be dumped in postgres database before running this integration test suite.
All the init.sql data values like fileId, datasetId etc are used in this integration test.


## How to Test
The `ega-data-api` application must be running somewhere before running this integration test suite. Steps to run the integration test:

1. Launch an empty postgres database and export the `src/main/resources/init.sql` in it.
2. Run  ega-data-api(`https://github.com/EGA-archive/ega-data-api`) and the values for database url(spring.datasource.url) in key service and fiedatabase service should point to above postgres db
3. We are ready now to test this integration suite. Run below command 

```
$ mvn test
```

The default value for properties are written is pom.xml inside `<systemPropertyVariables>` for example the key server host is assumed to be running on `http://localhost`. To override this value at run time use below command

```
$ mvn test "-Dkey.url=http://localhost2"
```

## Configurations
### Key server
The key server should have following properties updated in the properties file(keyserver.properties). These below sample files `ega.sec`, `ega.sec.pass`, `ega.shared.pass`, `ega.pub` & `legacy.pass` are provided in `src/test/resources/key` Copy these in your local system and update the path below as per yours.

```
ega.key.path = D:/ebi/config-files/ega.sec
ega.keypass.path = D:/ebi/config-files/ega.sec.pass
ega.sharedpass.path = D:/ebi/config-files/ega.shared.pass
ega.publickey.url = D:/ebi/config-files/ega.pub
ega.legacy.path = D:/ebi/config-files/legacy.pass
```

### Res server
The res server should have following properties updated in the properties file(res2.properties). These below sample files `ega.shared.pass` is provided in `src/test/resources/key` Copy these in your local system and update the path below as per yours.

```
ega.sharedpass.path = D:/ebi/config-files/ega.shared.pass
```

Note: The encrypted file `src/test/resources/key/EGAF00000000014.enc` should be uploaded in your s3/minio server.


### Dataedge
The dataedge server should have following properties updated in the properties file(dataedge.properties)
```
security.oauth2.resource.jwt.key-value: '-----BEGIN PUBLIC KEY-----
MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDdlatRjRjogo3WojgGHFHYLugd
UWAY9iR3fy4arWNA1KoS8kVw33cJibXr8bvwUAUparCwlvdbH6dvEOfou0/gCFQs
HUfQrSDv+MuSUMAe8jzKE4qW+jK+xQU9a03GUnKHkkle+Q0pX/g6jXZ7r1/xAK5D
o2kQ+X5xK9cipRgEKwIDAQAB
-----END PUBLIC KEY-----'
```
