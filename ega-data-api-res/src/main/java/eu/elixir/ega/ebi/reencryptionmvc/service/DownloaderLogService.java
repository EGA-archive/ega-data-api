package eu.elixir.ega.ebi.reencryptionmvc.service;

public interface DownloaderLogService {

  String logStart(String stableId, long startCoordinate, long endCoordinate);

  void logCompleted(String requestId, long downloadSize, double speed);

  void logError(String requestId, String code, String cause);

}
