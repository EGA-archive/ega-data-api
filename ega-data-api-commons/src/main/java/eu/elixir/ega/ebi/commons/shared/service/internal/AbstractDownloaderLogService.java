package eu.elixir.ega.ebi.commons.shared.service.internal;

import eu.elixir.ega.ebi.commons.shared.dto.DownloadEntry;
import eu.elixir.ega.ebi.commons.shared.dto.EventEntry;
import eu.elixir.ega.ebi.commons.shared.service.AuthenticationService;
import eu.elixir.ega.ebi.commons.shared.service.DownloaderLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;

@Slf4j
public abstract class AbstractDownloaderLogService implements DownloaderLogService {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private HttpServletRequest request;

    /**
     * Writes a {@link DownloadEntry} string to the log.
     *
     * @param downloadEntry a DownloadEntry to write to the log
     */
    @Override
    public void logDownload(DownloadEntry downloadEntry) {
        log.info(downloadEntry.toString());
    }

    /**
     * Writes an {@link EventEntry} string to the log.
     *
     * @param eventEntry an EventEntry to write to the log
     */
    @Override
    public void logEvent(EventEntry eventEntry) {
        log.info(eventEntry.toString());
    }

    /**
     * Creates a new {@link EventEntry} with the description given in {@code t},
     * client IP set from the current request, email set to the current login
     * email, type set to 'Error' and created set to the current timestamp.
     *
     * @param t Event description text.
     * @param ticket unused.
     *
     * @return An 'Error' type {@link EventEntry}.
     */
    @Override
    public EventEntry createEventEntry(String t, String ticket) {
        EventEntry eev = new EventEntry();
        eev.setEventId("0");
        // CLient IP
        String ipAddress = request.getHeader("client-ip");
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

    /**
     * Creates a new {@link DownloadEntry} with the supplied values, email set to
     * the current login user, created set to the current timestamp and token
     * source set to 'EGA'.
     *
     * @param success {@code true} or {@code false} wheather the download succeeded.
     * @param speed Download speed for the download.
     * @param fileId Stable ID of the downloaded file.
     * @param server Server where the data was downloaded from.
     * @param encryptionType The encryption type for the downloaded data.
     * @param startCoordinate Start coordinate of the downloaded data sequence.
     * @param endCoordinate End coordinate of the downloaded data sequence.
     * @param bytes Number of downloaded bytes.
     *
     * @return A {@link DownloadEntry} with the supplied values.
     */
    @Override
    public DownloadEntry createDownloadEntry(boolean success, double speed, String fileId,
                                             String server, String encryptionType, long startCoordinate, long endCoordinate, long bytes) {
        DownloadEntry dle = new DownloadEntry();
        dle.setDownloadLogId(0L);
        dle.setDownloadSpeed(speed);
        dle.setDownloadStatus(success ? "success" : "failed");
        dle.setFileId(fileId);
        String ipAddress = request.getHeader("client-ip");
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
