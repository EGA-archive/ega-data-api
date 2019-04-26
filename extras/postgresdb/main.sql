-- \connect lega

-- We use schemas for isolation instead of multiple databases.
-- Look at the grants.sql file, for access user/role rights setup

CREATE SCHEMA local_ega; -- includes the main table, some views and errors

SET search_path TO local_ega;

CREATE TYPE checksum_algorithm AS ENUM ('MD5', 'SHA256', 'SHA384', 'SHA512'); -- md5 is bad. Use sha*!
CREATE TYPE storage AS ENUM ('S3', 'POSIX');
-- Note: This is an enum, because that's what the "provided" database supports
--       If a site has its own database already, let them define their keyword in the ENUM 
--       and use it (Notice that their code must be update to push this value into the table)
--       There is no need to agree on how each site should operate their own database
--       What we need is to document where they need to update and what.


-- ##################################################
--                  FILE STATUS
-- ##################################################
CREATE TABLE local_ega.status (
        id            INTEGER,
	code          VARCHAR(16) NOT NULL,
	description   TEXT,
	-- contraints
	PRIMARY KEY(id), UNIQUE (id), UNIQUE (code)
);

INSERT INTO local_ega.status(id,code,description)
VALUES (10, 'INIT'        , 'Initializing a file ingestion'),
       (20, 'IN_INGESTION', 'Currently under ingestion'),
       (30, 'ARCHIVED'    , 'File moved to Archive'),
       (40, 'COMPLETED'   , 'File verified in Archive'),
       (50, 'READY'       , 'File ingested, ready for download'),
       -- (60, 'IN_INDEXING', 'Currently under index creation'),
       (0, 'ERROR'        , 'An Error occured, check the error table'),
       (1, 'DISABLED'     , 'Used for submissions that are stopped, overwritten, or to be cleaned up')
;

-- ##################################################
--                ENCRYPTION FORMAT
-- ##################################################
CREATE TABLE local_ega.archive_encryption (
       mode          VARCHAR(16) NOT NULL, PRIMARY KEY(mode), UNIQUE (mode),
       description   TEXT
);

INSERT INTO local_ega.archive_encryption(mode,description)
VALUES ('CRYPT4GH'  , 'Crypt4GH encryption (using version)'),
       ('PGP'       , 'OpenPGP encryption (RFC 4880)'),
       ('AES'       , 'AES encryption with passphrase'),
       ('CUSTOM1'   , 'Custom method 1 for local site'),
       ('CUSTOM2'   , 'Custom method 2 for local site')
    -- ...
;

-- ##################################################
--                        FILES
-- ##################################################
-- Main table with looooots of information
CREATE TABLE local_ega.main (
       id                        SERIAL, PRIMARY KEY(id), UNIQUE (id),

       -- EGA file ids
       stable_id                 TEXT,

       -- Status
       status                    VARCHAR NOT NULL REFERENCES local_ega.status (code), -- No "ON DELETE CASCADE":
       				 	     	  	     		      	      -- update to the new status
                                                                            	      -- in case the old one is deleted
       -- Original/Submission file
       submission_file_path                     TEXT NOT NULL,
       submission_file_extension                VARCHAR(260) NOT NULL,
       submission_file_calculated_checksum      VARCHAR(128),
       submission_file_calculated_checksum_type checksum_algorithm,

       submission_file_size                     BIGINT NULL,
       submission_user                          TEXT NOT NULL, -- Elixir ID, or internal user
 
       -- Archive information
       archive_file_reference      TEXT,    -- file path if POSIX, object id if S3
       archive_file_type           storage, -- S3 or POSIX file system
       archive_file_size           BIGINT,
       archive_file_checksum       VARCHAR(128) NULL, -- NOT NULL,
       archive_file_checksum_type  checksum_algorithm,
       
       -- Encryption/Decryption
       encryption_method         VARCHAR REFERENCES local_ega.archive_encryption (mode), -- ON DELETE CASCADE,
       version                   INTEGER , -- DEFAULT 1, -- Crypt4GH version
       header                    TEXT,              -- Crypt4GH header
       session_key_checksum      VARCHAR(128) NULL, -- NOT NULL, -- To check if session key already used
       session_key_checksum_type checksum_algorithm,
       -- Note: We can support multiple encryption formats. See at the end of that file.

       -- Table Audit / Logs
       created_by                NAME DEFAULT CURRENT_USER, -- Postgres users
       last_modified_by          NAME DEFAULT CURRENT_USER, --
       created_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT clock_timestamp(),
       last_modified             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT clock_timestamp()
);
CREATE UNIQUE INDEX file_id_idx ON local_ega.main(id);

-- When there is an updated, remember the timestamp
CREATE FUNCTION main_updated()
RETURNS TRIGGER AS $main_updated$
BEGIN
     NEW.last_modified = clock_timestamp();
		 RETURN NEW;
END;
$main_updated$ LANGUAGE plpgsql;

CREATE TRIGGER main_updated AFTER UPDATE ON local_ega.main FOR EACH ROW EXECUTE PROCEDURE main_updated();

-- ##################################################
--                      ERRORS
-- ##################################################
CREATE TABLE local_ega.main_errors (
        id            SERIAL, PRIMARY KEY(id), UNIQUE (id),
	active        BOOLEAN NOT NULL DEFAULT TRUE,
	file_id       INTEGER NOT NULL REFERENCES local_ega.main(id) ON DELETE CASCADE,
	hostname      TEXT,
	error_type    TEXT NOT NULL,
	msg           TEXT NOT NULL,
	from_user     BOOLEAN DEFAULT FALSE,
	occured_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT clock_timestamp()
);

-- ##################################################
--         Data-In View
-- ##################################################
-- 
CREATE VIEW local_ega.files AS
SELECT id,
       submission_user                          AS elixir_id,
       submission_file_path                     AS inbox_path,
       submission_file_size                     AS inbox_filesize,
       submission_file_calculated_checksum      AS inbox_file_checksum,
       submission_file_calculated_checksum_type AS inbox_file_checksum_type,
       status,
       archive_file_reference                     AS archive_path,
       archive_file_type                          AS archive_type,
       archive_file_size                          AS archive_filesize,
       archive_file_checksum                      AS unencrypted_checksum,
       archive_file_checksum_type                 AS unencrypted_checksum_type,
       stable_id,
       header,  -- Crypt4gh specific
       version,
       session_key_checksum,
       session_key_checksum_type,
       created_at,
       last_modified
FROM local_ega.main;

-- Insert into main
CREATE FUNCTION insert_file(inpath        local_ega.main.submission_file_path%TYPE,
			    eid           local_ega.main.submission_user%TYPE)
RETURNS local_ega.main.id%TYPE AS $insert_file$
    #variable_conflict use_column
    DECLARE
        file_id  local_ega.main.id%TYPE;
        file_ext local_ega.main.submission_file_extension%TYPE;
    BEGIN
        -- Mark old ones as deprecated
        UPDATE local_ega.main SET status = 'DISABLED'
	                      WHERE submission_file_path = inpath AND
	  	                    submission_user = eid AND
				    status <> 'ERROR';
			   	    
        -- Make a new insertion
        file_ext := substring(inpath from '\.([^\.]*)$'); -- extract extension from filename
	INSERT INTO local_ega.main (submission_file_path,
	  	                    submission_user,
			   	    submission_file_extension,
			  	    status,
			  	    encryption_method) -- hard-code the archive_encryption
	VALUES(inpath,eid,file_ext,'INIT','CRYPT4GH') RETURNING local_ega.main.id
	INTO file_id;
	RETURN file_id;
    END;
$insert_file$ LANGUAGE plpgsql;

-- Flag as READY, and mark older ingestion as deprecated (to clean up)
CREATE FUNCTION finalize_file(inpath        local_ega.files.inbox_path%TYPE,
			      eid           local_ega.files.elixir_id%TYPE,
			      checksum      local_ega.files.unencrypted_checksum%TYPE,
			      checksum_type VARCHAR, -- local_ega.files.unencrypted_checksum_type%TYPE,
			      sid           local_ega.files.stable_id%TYPE)
    RETURNS void AS $finalize_file$
    #variable_conflict use_column
    BEGIN
	-- -- Check if in proper state
	-- IF EXISTS(SELECT id
	--    	  FROM local_ega.main
	-- 	  WHERE unencrypted_checksum = checksum AND
	-- 	  	unencrypted_checksum_type = upper(checksum_type)::local_ega.checksum_algorithm AND
	-- 		elixir_id = eid AND
	-- 		inbox_path = inpath AND
	-- 		status <> 'COMPLETED')
	-- THEN
	--    RAISE EXCEPTION 'Archive file not in completed state for stable_id: % ', sid;
	-- END IF;
	-- Go ahead and mark _them_ done
	UPDATE local_ega.files
	SET status = 'READY',
	    stable_id = sid
	WHERE unencrypted_checksum = checksum AND
	      unencrypted_checksum_type = upper(checksum_type)::local_ega.checksum_algorithm AND
	      elixir_id = eid AND
	      inbox_path = inpath AND
	      status = 'COMPLETED';
    END;
$finalize_file$ LANGUAGE plpgsql;

-- If the entry is marked disabled, it says disabled. No data race here.
CREATE FUNCTION is_disabled(fid local_ega.main.id%TYPE)
RETURNS boolean AS $is_disabled$
#variable_conflict use_column
BEGIN
   RETURN EXISTS(SELECT 1 FROM local_ega.files WHERE id = fid AND status = 'DISABLED');
END;
$is_disabled$ LANGUAGE plpgsql;

-- Returns if the session key checksum is already found in the database
CREATE FUNCTION check_session_key_checksum(checksum      local_ega.files.session_key_checksum%TYPE,
       		      			   checksum_type VARCHAR) -- local_ega.files.session_key_checksum_type%TYPE)
    RETURNS boolean AS $check_session_key_checksum$
    #variable_conflict use_column
    BEGIN
	RETURN EXISTS(SELECT 1 FROM local_ega.files
		      WHERE session_key_checksum = checksum AND
		      	    session_key_checksum_type = upper(checksum_type)::local_ega.checksum_algorithm AND
			    (status <> 'ERROR' OR status <> 'DISABLED'));
    END;
$check_session_key_checksum$ LANGUAGE plpgsql;

-- Just showing the current/active errors
CREATE VIEW local_ega.errors AS
SELECT id, file_id, hostname, error_type, msg, from_user, occured_at
FROM local_ega.main_errors
WHERE active = TRUE;

CREATE FUNCTION insert_error(fid        local_ega.errors.file_id%TYPE,
                             h          local_ega.errors.hostname%TYPE,
                             etype      local_ega.errors.error_type%TYPE,
                             msg        local_ega.errors.msg%TYPE,
                             from_user  local_ega.errors.from_user%TYPE)
    RETURNS void AS $insert_error$
    BEGIN
       INSERT INTO local_ega.errors (file_id,hostname,error_type,msg,from_user) VALUES(fid,h,etype,msg,from_user);
       UPDATE local_ega.files SET status = 'ERROR' WHERE id = fid;
    END;
$insert_error$ LANGUAGE plpgsql;


-- When File becomes 'READY', remove all its errors from current errors.
CREATE FUNCTION mark_ready()
RETURNS TRIGGER AS $mark_ready$
BEGIN
     UPDATE local_ega.main_errors SET active = FALSE WHERE file_id = NEW.id;  -- or OLD.id
     RETURN NEW;
END;
$mark_ready$ LANGUAGE plpgsql;

CREATE TRIGGER mark_ready 
    AFTER UPDATE OF status ON local_ega.main -- using the main and not files
                                             -- because "Views cannot have row-level BEFORE or AFTER triggers."
    FOR EACH ROW WHEN (NEW.status = 'READY')
    EXECUTE PROCEDURE mark_ready();




-- ##########################################################################
--           For data-out
-- ##########################################################################

-- View on the archive files
CREATE VIEW local_ega.archive_files AS
SELECT id                        AS file_id
     , stable_id                 AS stable_id
     , archive_file_reference      AS archive_path
     , archive_file_type           AS archive_type
     , archive_file_size           AS archive_filesize
     , archive_file_checksum       AS unencrypted_checksum
     , archive_file_checksum_type  AS unencrypted_checksum_type
     , header                    AS header
     , version                   AS version
FROM local_ega.main
WHERE status = 'READY';


INSERT INTO local_ega.main(id, stable_id, archive_file_reference, archive_file_type, archive_file_size, archive_file_checksum, archive_file_checksum_type, header, status, submission_file_path, submission_file_extension, submission_user) VALUES (14, 'EGAF00000000014', '14.enc', 'S3', 1220,'8705adaec1c7cbb27b27456178c15aae', 'MD5', '6372797074346768010000009b020000c1c14c033efbea862334cc7e011000a2f5606712b87f448a310443584ac85e2dff614955680ee2b30a1d6c6c83de71d2734786d30b4b45d8caaf1fd36de138aaf1acfdddc4a74764df1d1e602fd7ad932d5f1063501593658ec2d9e48d5755815a65673eece627ac6b1df442f4d41175ba5c56c86de02fcf297ede3847e48641b322a0081394a299f2f2e32e944ac86f9a43a841006fa2433b149dcfdd2727bbe0256509283675915f230731d29850f088212fc8d90b1f8fb04dcde93ac0bb96b16b9f16c28972d495d2efa2e1a215e1be2b3cb5b3012aec557be1554e0dd5fe183e8242e62e57a1a4cba1c14220864a48f66bfc9344dc53147453b46b5d75a8e853f146cd68ffc1e5f03c223529b7230004fec43d2e559cb041305cda3fa3599496411e68302c742289f1ae888a2e7353b44ff42ebee17dc98f25da4b78f08cdae787df3bb3251080a1ca3f5f43fdeed0057635a9deec5c95f4c9362f8ed3c2a729ec0eb7cf9ac617dce21909c96cc9da4223caeab9bfec4c006eb61456fd14d09890bc4faa94a3e6fee4be655960cad831f4e75d460b55776d66753c41479f100f17ece8f1f703c09d4a614e7c997c75d6716943228ca23ffcd4eed2f06568d40d503f8f87495b80fb38b8da83bf201578729f5140335e51c83d993d5488419a2afc01a5defd912e10c9a76a9cdd6680765b5dcbc589efcc79c2244e783518495c2612103faf166469e7af200d0bd27a01a5069575acf45b24e5bb37f8cff34f8c8b5fcac827d84f4b8fe222f68de6ab5efc5f25b60b832fec6ed3321ff57648240f6f8d9e0f51b1ad5a4f64b037bdcb64b7a74db00fd77f5e2de764d38e5c7a0641894df5474bced50d255e09f29fb36487dc10423973f63f0af0b61a2ec9bf47890ec2', 'READY', '14.enc', 'ext', 'testuser');

-- ##########################################################################
--                   About the encryption
-- ##########################################################################
-- 
-- 
-- We can support multiple encryption types in the archive
-- (Say, for example, Crypt4GH, PGP and plain AES),
-- in the following manner:
-- 
-- We create a table for each method of encryption.
-- Each table will have its own set of fields, refering to data it needs for decryption
-- 
-- Then we update the main file table with a archive_encryption "keyword".
-- That will tell the main file table to look at another table for that
-- particular file. (Note that this file reference is found in only one
-- encryption table).
--
-- At the moment, we added the whole Crypt4gh-related table inside the main table.
-- That's easily changeable, using views.
-- 
-- The site that wants to support multiple encryption methods must update the data-in and
-- data-out code in order to push/pull the data accordingly.

