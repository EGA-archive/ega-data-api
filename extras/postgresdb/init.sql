create schema file_key_test;
create schema dev_ega_file;

CREATE SEQUENCE dev_ega_file.download_log_new_download_log_id_seq
INCREMENT BY 1
MINVALUE 1
MAXVALUE 9223372036854775807
START 1;

CREATE SEQUENCE dev_ega_file.event_event_id_seq
INCREMENT BY 1
MINVALUE 1
MAXVALUE 9223372036854775807
START 1;


CREATE TABLE dev_ega_file.file_key (
	file_id  varchar(128) NULL,
	encryption_key_id int8 NULL,
    encryption_algorithm varchar(128) NULL
);

CREATE TABLE file_key_test.encryption_key (
	encryption_key_id bigserial NOT NULL,
	alias varchar(128) NOT NULL,
	encryption_key text NOT NULL
);


CREATE TABLE dev_ega_file.file_index_file (
	file_id varchar(128) NULL,
	index_file_id varchar(128) NULL
);

-- Relation File EGAF <-> Dataset EGAD
CREATE TABLE dev_ega_file.file_dataset (
       id       SERIAL,
       file_id     varchar(256) , 
       dataset_id  TEXT NOT NULL
);

-- Relation File <-> Index File
CREATE TABLE dev_ega_file.index_files (
       id       SERIAL,
       file_id  varchar(256),
       index_file_reference TEXT,   
       index_file_type varchar (12)
);

-- Special view for EBI Data-Out
CREATE TABLE dev_ega_file.file (
	file_id varchar(256),
    file_name varchar(256),
    file_path varchar (256),
    display_file_name varchar (256),	
    file_type varchar (256),
    file_size int8,
    checksum varchar(128) NULL,
    checksum_type varchar(12) NULL,
    unencrypted_checksum varchar (128),
    unencrypted_checksum_type varchar (12),
	file_status varchar(13) NULL,
    header text,
    created_by  timestamp NOT NULL DEFAULT now(),
    last_updated_by  timestamp NOT NULL DEFAULT now(),
    created  timestamp NOT NULL DEFAULT now(),
    last_updated  timestamp NOT NULL DEFAULT now()
);

-- Event
CREATE TABLE dev_ega_file.event (
       event_id  int8 NOT NULL DEFAULT nextval('dev_ega_file.event_event_id_seq'::regclass),
       client_ip varchar(45) NOT NULL,
       event varchar(256) NOT NULL,
       event_type varchar(256) NOT NULL,
       email varchar(256) NOT NULL,
       created timestamp NOT NULL DEFAULT now(),
       CONSTRAINT event_pkey PRIMARY KEY (event_id)
);

-- Download Log
CREATE TABLE dev_ega_file.download_log (
       download_log_id int8 NOT NULL DEFAULT nextval('dev_ega_file.download_log_new_download_log_id_seq'::regclass),
       client_ip varchar(45) NOT NULL,
       api varchar(45) NOT NULL,
       email varchar(256) NOT NULL,
       file_id varchar(15) NOT NULL,
       download_speed float8 NOT NULL DEFAULT '-1'::integer,
       download_status varchar(256) NOT NULL DEFAULT 'success'::character varying,
       encryption_type varchar(256) NOT NULL,
       start_coordinate int8 NOT NULL DEFAULT 0,
       end_coordinate int8 NOT NULL DEFAULT 0,
       bytes int8 NOT NULL DEFAULT 0,
       created timestamp NOT NULL DEFAULT now(),
       token_source varchar(255) NOT NULL
);

INSERT INTO dev_ega_file.file_dataset(file_id, dataset_id) values('EGAF00000000014', 'EGAD00010000919');

INSERT INTO dev_ega_file.file(file_id, file_name, file_path, file_type, file_size, unencrypted_checksum, unencrypted_checksum_type, header) VALUES ('EGAF00000000014', '14.enc', '14.enc' , 'EGAF00000000014.enc', 1287,'c34af3b83f322f7d10e28ad29ba1884c', 'plain', '6372797074346768010000009c020000c1c14c033efbea862334cc7e010ffc0854fbbeb86868ba363562b751ff1a61e2d3a522d672e3d45b9b58ca072c2c76f76198cf90ed654dfd35a6e4e0fb008ba7be397a06c961b5cc775705c28b88443e6812d13276006bd8615f234255fbeee57f6601a50fd89300e8fd1bea7f4d771b69d98c93dc97ae4869652a911c47339297e8b3077740e8418d19bfdaaa92c1bc744810f617aac72591678f6ece1707b64015e3139967117408831e9c9a5b8c72ace8dbd8b1a0f7419b7e253c2b20a83de746886b615268a6bdf3ccb84221c45be9b493a45f15d3d6fb01b28d142d42f748cf4c153fa8b834069899f75bb78882af41e36d2b19c82bb6a1d194cabf6ff64fe05db02460bbda206628466a8ed9ee91c7c9b3094e7936be8917669a1e5644e0cbd9dfd9fc4a5a50ffe3e1d1a95c5834416a187577076d2a29380b65da0be3c837f2296b1692694af8daf46d3bd584bf0d7415d4f1aff62774083166b8435e113d18e49bcf5746a675d5ffa741b1c1e37e7a1ccf97154280135a670240e22467166d05493142b3254c5dcc4f8570152ec0c592eb64770104e7e916bf8f812076be7d08c4ab400025f9f2265029bb1e9ef75e7204b26dd81a4c5b855cdc2e83ba46fddc4ee623af0bbdb08decfbaf6c4dbd5c23798ec9929274666205e236a4df52890c3d903ab0eee690b9e9a34f850f456b1d8110615115845ad8e9bc608a2cb8d0b4d1b671a236dc126a17c8d2d27b01957917699d24f9d80f0faab3f28cdf95cfcc3c767587891c11e0f69295a1e730e6639db6b3ef3e40e01d8881308b42fd01a193f52358ec6893af787cf8095767a1282123308b12ffb70197250de9935cb305f40837c7b54d63b1d5a33e5bb7b1bf9d8e24cb30e80f5b131b08246cd5f3b47b7dbe8d6f39ed0938');

INSERT INTO dev_ega_file.file_key(file_id, encryption_key_id, encryption_algorithm) values ('EGAF00000000014', 1 , 'aes256' );

INSERT INTO file_key_test.encryption_key(encryption_key_id, encryption_key, alias) values (1, 'DfHmQA3Zm0sZW+MzbKcwEBytsPRTgDhJOdwsNmc=', 'Base64');