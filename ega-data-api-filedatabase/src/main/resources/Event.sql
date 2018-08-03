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

CREATE TABLE dev_ega_file.event (
	event_id int8 NOT NULL DEFAULT nextval('dev_ega_file.event_event_id_seq'::regclass),
	client_ip varchar(45) NOT NULL,
	event varchar(256) NOT NULL,
	event_type varchar(256) NOT NULL,
	email varchar(256) NOT NULL,
	created timestamp NOT NULL DEFAULT now(),
	CONSTRAINT event_pkey PRIMARY KEY (event_id)
)
WITH (
	OIDS=FALSE
);