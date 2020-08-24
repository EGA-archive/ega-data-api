/* 
 * Copyright 2020 ELIXIR EGA
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

CREATE TABLE dev_ega_file.dataset (
	dataset_id varchar(128) NOT NULL,
	description text NULL,
	dac_stable_id varchar(128) NULL,
	double_signature varchar(3) NULL
)
WITH (
	OIDS=FALSE
);
CREATE UNIQUE INDEX dataset_id_idx ON dev_ega_file.dataset (dataset_id);
