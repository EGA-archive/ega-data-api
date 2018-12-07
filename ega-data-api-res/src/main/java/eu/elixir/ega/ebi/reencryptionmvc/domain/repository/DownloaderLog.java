package eu.elixir.ega.ebi.reencryptionmvc.domain.repository;

public interface DownloaderLog {

  String makeRequest(String StableId, String user, String clientIp, long startCoordinate, long endCoordinate);

  void downloadComplete(String requestId, long downloadSize, double speed);

  void setError(String requestId, String hostname, String code, String description);

}
