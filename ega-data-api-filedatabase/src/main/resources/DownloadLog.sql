/* 
 * Copyright 2017 ELIXIR EGA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Author:  asenf
 * Created: 17-Feb-2017
 */

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
)
WITH (
	OIDS=FALSE
);
