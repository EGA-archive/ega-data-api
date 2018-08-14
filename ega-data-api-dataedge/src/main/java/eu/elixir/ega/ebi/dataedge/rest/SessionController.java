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
package eu.elixir.ega.ebi.dataedge.rest;

import eu.elixir.ega.ebi.dataedge.config.NotFoundException;
import eu.elixir.ega.ebi.dataedge.domain.entity.Transfer;
import eu.elixir.ega.ebi.dataedge.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * @author asenf
 */
@RestController
@RequestMapping("/session")
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @RequestMapping(value = "/{sessionId}", method = GET)
    @ResponseBody
    public Transfer get(@PathVariable String sessionId) {
        Transfer session = sessionService.getSessionStats(sessionId);
        if (session == null) {
            throw new NotFoundException("Session not found for UUID", sessionId);
        }
        return session;
    }

}
