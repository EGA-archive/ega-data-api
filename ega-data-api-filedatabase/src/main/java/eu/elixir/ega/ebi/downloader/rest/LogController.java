/*
 * Copyright 2016 ELIXIR EGA
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
package eu.elixir.ega.ebi.downloader.rest;

import eu.elixir.ega.ebi.downloader.domain.entity.DownloadLog;
import eu.elixir.ega.ebi.downloader.domain.entity.Event;
import eu.elixir.ega.ebi.downloader.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * @author asenf
 */
@RestController
@RequestMapping("/log")
public class LogController {

    @Autowired
    private LogService logService;

    // Loging an 'Event' (Free Form Events)
    @RequestMapping(value = "/event", method = POST)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putEvent(@RequestBody Event input) {
        Event logEvent = logService.logEvent(input);
        // TODO Return 201 with Resource URL
    }

    // Loging a 'Download' (Download Success/Failure)
    @RequestMapping(value = "/download", method = POST)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void putDownload(@RequestBody DownloadLog input) {
        DownloadLog logDownload = logService.logDownload(input);
        // TODO Return 201 with Resource URL
    }
}
