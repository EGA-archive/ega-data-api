package eu.elixir.ega.ebi.reencryptionmvc.service.internal;

import eu.elixir.ega.ebi.reencryptionmvc.domain.repository.DownloaderLog;
import eu.elixir.ega.ebi.reencryptionmvc.service.DownloaderLogService;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Profile("repo-logger")
@Primary
@Transactional
@Service
public class RepoDownloaderLogServiceImpl implements DownloaderLogService {

  @Autowired
  private DownloaderLog downloaderLogRepository;

  @Autowired
  private HttpServletRequest request;

  @Override
  public String logStart(String stableId, long startCoordinate, long endCoordinate) {
    String clientIp = request.getHeader("X-FORWARDED-FOR");
    String user = request.getHeader("X-USER-NAME");
    return downloaderLogRepository
        .makeRequest(stableId, user, clientIp, startCoordinate, endCoordinate);
  }

  @Override
  public void logCompleted(String requestId, long downloadSize, double speed) {
    downloaderLogRepository.downloadComplete(requestId, downloadSize, speed);
  }

  @Override
  public void logError(String requestId, String code, String cause) {
    String hostname = "unknown";
    try {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      //Ignore
    }
    downloaderLogRepository.setError(requestId, hostname, code, cause);
  }
}
