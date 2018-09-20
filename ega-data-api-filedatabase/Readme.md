# FILEDATABASE SERVICE

This is a Database Interaction Server. It (1) provides File/Dataset information and (2) performs basic database logging

This service is an abstraction layer to the local database, which keeps information about files and datasets, as well as some basic logging tables.

It primarily interacts with the databases used by the API. It sits behind the edge services, which enforce authentication and authorization before any call to this service is made. This service performs no further security checks.

### Databases

There are 5 tables necessary for this service:

* File (read-only for this service): This table contains all files held at the current location. Files are identified by the unique EGAF File ID and are placed in Datasets with unique EGAD IDs (permissions are granted on a dataset-level) This table also contains the absolute path/URL to the archived file.

* FileDataset: Necessary to link Datasets to Files (EGA permissions are based on datasets, so it is necessary to know which files are in a dataset)

* FileIndexFile: Links files to potential Index files, if present. This is necessary to provide htsget functionality

* DownloadLog: Logs downloads - both successes and failures

* Event: Generic table to log 'events' of interest