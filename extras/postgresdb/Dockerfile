FROM library/postgres
COPY extras/postgresdb/init.sql /docker-entrypoint-initdb.d/1.sql
COPY extras/postgresdb/main.sql /docker-entrypoint-initdb.d/2.sql
COPY extras/postgresdb/download.sql /docker-entrypoint-initdb.d/3.sql
COPY extras/postgresdb/ebi.sql /docker-entrypoint-initdb.d/4.sql