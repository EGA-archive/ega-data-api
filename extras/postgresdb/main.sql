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


INSERT INTO local_ega.main(id, stable_id, archive_file_reference, archive_file_type, archive_file_size, archive_file_checksum, archive_file_checksum_type, header, status, submission_file_path, submission_file_extension, submission_user) VALUES (14, 'EGAF00000000014', '14.enc', 'S3', 1319,'c34af3b83f322f7d10e28ad29ba1884c', 'MD5', '6372797074346768010000009c020000c1c14c033efbea862334cc7e010ffc0854fbbeb86868ba363562b751ff1a61e2d3a522d672e3d45b9b58ca072c2c76f76198cf90ed654dfd35a6e4e0fb008ba7be397a06c961b5cc775705c28b88443e6812d13276006bd8615f234255fbeee57f6601a50fd89300e8fd1bea7f4d771b69d98c93dc97ae4869652a911c47339297e8b3077740e8418d19bfdaaa92c1bc744810f617aac72591678f6ece1707b64015e3139967117408831e9c9a5b8c72ace8dbd8b1a0f7419b7e253c2b20a83de746886b615268a6bdf3ccb84221c45be9b493a45f15d3d6fb01b28d142d42f748cf4c153fa8b834069899f75bb78882af41e36d2b19c82bb6a1d194cabf6ff64fe05db02460bbda206628466a8ed9ee91c7c9b3094e7936be8917669a1e5644e0cbd9dfd9fc4a5a50ffe3e1d1a95c5834416a187577076d2a29380b65da0be3c837f2296b1692694af8daf46d3bd584bf0d7415d4f1aff62774083166b8435e113d18e49bcf5746a675d5ffa741b1c1e37e7a1ccf97154280135a670240e22467166d05493142b3254c5dcc4f8570152ec0c592eb64770104e7e916bf8f812076be7d08c4ab400025f9f2265029bb1e9ef75e7204b26dd81a4c5b855cdc2e83ba46fddc4ee623af0bbdb08decfbaf6c4dbd5c23798ec9929274666205e236a4df52890c3d903ab0eee690b9e9a34f850f456b1d8110615115845ad8e9bc608a2cb8d0b4d1b671a236dc126a17c8d2d27b01957917699d24f9d80f0faab3f28cdf95cfcc3c767587891c11e0f69295a1e730e6639db6b3ef3e40e01d8881308b42fd01a193f52358ec6893af787cf8095767a1282123308b12ffb70197250de9935cb305f40837c7b54d63b1d5a33e5bb7b1bf9d8e24cb30e80f5b131b08246cd5f3b47b7dbe8d6f39ed0938', 'READY', '14.enc', 'ext', 'testuser');

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

