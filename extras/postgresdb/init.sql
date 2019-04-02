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

INSERT INTO dev_ega_file.file(file_id, file_name, file_path, file_type, file_size, unencrypted_checksum, unencrypted_checksum_type, header) VALUES ('EGAF00000000014', 'EGAF00000000014', 'EGAF00000000014.enc' , 'EGAF00000000014.enc', 1220,'8705adaec1c7cbb27b27456178c15aae', 'plain', '6372797074346768010000009b020000c1c14c033efbea862334cc7e011000a2f5606712b87f448a310443584ac85e2dff614955680ee2b30a1d6c6c83de71d2734786d30b4b45d8caaf1fd36de138aaf1acfdddc4a74764df1d1e602fd7ad932d5f1063501593658ec2d9e48d5755815a65673eece627ac6b1df442f4d41175ba5c56c86de02fcf297ede3847e48641b322a0081394a299f2f2e32e944ac86f9a43a841006fa2433b149dcfdd2727bbe0256509283675915f230731d29850f088212fc8d90b1f8fb04dcde93ac0bb96b16b9f16c28972d495d2efa2e1a215e1be2b3cb5b3012aec557be1554e0dd5fe183e8242e62e57a1a4cba1c14220864a48f66bfc9344dc53147453b46b5d75a8e853f146cd68ffc1e5f03c223529b7230004fec43d2e559cb041305cda3fa3599496411e68302c742289f1ae888a2e7353b44ff42ebee17dc98f25da4b78f08cdae787df3bb3251080a1ca3f5f43fdeed0057635a9deec5c95f4c9362f8ed3c2a729ec0eb7cf9ac617dce21909c96cc9da4223caeab9bfec4c006eb61456fd14d09890bc4faa94a3e6fee4be655960cad831f4e75d460b55776d66753c41479f100f17ece8f1f703c09d4a614e7c997c75d6716943228ca23ffcd4eed2f06568d40d503f8f87495b80fb38b8da83bf201578729f5140335e51c83d993d5488419a2afc01a5defd912e10c9a76a9cdd6680765b5dcbc589efcc79c2244e783518495c2612103faf166469e7af200d0bd27a01a5069575acf45b24e5bb37f8cff34f8c8b5fcac827d84f4b8fe222f68de6ab5efc5f25b60b832fec6ed3321ff57648240f6f8d9e0f51b1ad5a4f64b037bdcb64b7a74db00fd77f5e2de764d38e5c7a0641894df5474bced50d255e09f29fb36487dc10423973f63f0af0b61a2ec9bf47890ec2');

INSERT INTO dev_ega_file.file_key(file_id, encryption_key_id, encryption_algorithm) values ('EGAF00000000014', 1 , 'aes256' );

INSERT INTO file_key_test.encryption_key(encryption_key_id, encryption_key, alias) values (1, 'DfHmQA3Zm0sZW+MzbKcwEBytsPRTgDhJOdwsNmc=', 'Base64');