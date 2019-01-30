package eu.elixir.ega.ebi.shared.service.internal;

import eu.elixir.ega.ebi.shared.dto.DownloadEntry;
import eu.elixir.ega.ebi.shared.dto.EventEntry;
import eu.elixir.ega.ebi.shared.service.AuthenticationService;
import eu.elixir.ega.ebi.shared.service.DownloaderLogService;
import java.util.Calendar;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class AbstractDownloaderLogService implements DownloaderLogService {

  @Autowired
  private AuthenticationService authenticationService;

  @Autowired
  private HttpServletRequest request;

  @Override
  public void logDownload(DownloadEntry downloadEntry) {
    log.info(downloadEntry.toString());
  }

  @Override
  public void logEvent(EventEntry eventEntry) {
    log.info(eventEntry.toString());
  }

  @Override
  public EventEntry createEventEntry(String t, String ticket) {
    EventEntry eev = new EventEntry();
    eev.setEventId("0");
    // CLient IP
    String ipAddress = request.getHeader("X-FORWARDED-FOR");
    if (ipAddress == null) {
      ipAddress = request.getRemoteAddr();
    }
    eev.setClientIp(ipAddress);
    eev.setEvent(t);
    eev.setEventType("Error");
    eev.setEmail(authenticationService.getName());
    eev.setCreated(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));

    return eev;
  }

  @Override
  public DownloadEntry createDownloadEntry(boolean success, double speed, Integer fileId,
      String server, String encryptionType, long startCoordinate, long endCoordinate, long bytes) {
    DownloadEntry dle = new DownloadEntry();
    dle.setDownloadLogId(0L);
    dle.setDownloadSpeed(speed);
    dle.setDownloadStatus(success ? "success" : "failed");
    dle.setFileId(fileId);
    String ipAddress = request.getHeader("X-FORWARDED-FOR");
    if (ipAddress == null) {
      ipAddress = request.getRemoteAddr();
    }
    dle.setClientIp(ipAddress);
    dle.setEmail(authenticationService.getName());
    dle.setApi(server);
    dle.setEncryptionType(encryptionType);
    dle.setStartCoordinate(startCoordinate);
    dle.setEndCoordinate(endCoordinate);
    dle.setBytes(bytes);
    dle.setCreated(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
    dle.setTokenSource("EGA");

    return dle;
  }
}
