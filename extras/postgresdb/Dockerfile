FROM library/postgres
COPY init.sql /docker-entrypoint-initdb.d/1.sql
COPY main.sql /docker-entrypoint-initdb.d/2.sql
COPY download.sql /docker-entrypoint-initdb.d/3.sql
COPY ebi.sql /docker-entrypoint-initdb.d/4.sql