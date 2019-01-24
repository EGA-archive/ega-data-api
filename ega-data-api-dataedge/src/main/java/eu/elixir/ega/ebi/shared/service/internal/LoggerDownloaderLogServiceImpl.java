package eu.elixir.ega.ebi.shared.service.internal;

import eu.elixir.ega.ebi.shared.service.DownloaderLogService;
import eu.elixir.ega.ebi.shared.service.internal.AbstractDownloaderLogService;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("logger-log")
@Primary
@Service
public class LoggerDownloaderLogServiceImpl extends AbstractDownloaderLogService implements DownloaderLogService {

}
