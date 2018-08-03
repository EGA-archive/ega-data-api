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
package eu.elixir.ega.ebi.downloader.service.internal;

import eu.elixir.ega.ebi.downloader.domain.entity.DownloadLog;
import eu.elixir.ega.ebi.downloader.domain.entity.Event;
import eu.elixir.ega.ebi.downloader.domain.repository.DownloadLogRepository;
import eu.elixir.ega.ebi.downloader.domain.repository.EventRepository;
import eu.elixir.ega.ebi.downloader.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

/**
 * @author asenf
 */
@Service
@Transactional
public class LogServiceImpl implements LogService {

    @Autowired
    private DownloadLogRepository logRepository;

    @Autowired
    private EventRepository eventRepository;

    @Override
    public Event logEvent(Event event) {
        return eventRepository.save(event);
    }

    @Override
    public DownloadLog logDownload(DownloadLog downloadLog) {
        return logRepository.save(downloadLog);
    }

}
