package eu.elixir.ega.ebi.reencryptionmvc.domain.repository;

import java.sql.Types;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

@Profile("db-repo-logger")
@Repository
public class DbDownloaderLogRepository implements DownloaderLog {

  private SimpleJdbcCall makeRequest;
  private SimpleJdbcCall downloadComplete;
  private SimpleJdbcCall setError;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    this.makeRequest = new SimpleJdbcCall(dataSource)
        .withSchemaName("local_ega_download")
        .withProcedureName("make_request")
        .declareParameters(
            //In
            new SqlParameter("sid", Types.VARCHAR),
            new SqlParameter("uinfo", Types.VARCHAR),
            new SqlParameter("cip", Types.VARCHAR),
            new SqlParameter("scoord", Types.BIGINT),
            new SqlParameter("ecoord", Types.BIGINT),
            //Out
            new SqlOutParameter("req_id", Types.INTEGER),
            new SqlOutParameter("header", Types.VARCHAR),
            new SqlOutParameter("archive_path", Types.VARCHAR),
            new SqlOutParameter("archive_type", Types.OTHER),
            new SqlOutParameter("file_size", Types.INTEGER),
            new SqlOutParameter("unencrypted_checksum", Types.VARCHAR),
            new SqlOutParameter("unencrypted_checksum_type", Types.OTHER)
        );
    this.downloadComplete = new SimpleJdbcCall(dataSource)
        .withSchemaName("local_ega_download")
        .withProcedureName("download_complete");
    this.setError = new SimpleJdbcCall(dataSource)
        .withSchemaName("local_ega_download")
        .withProcedureName("insert_error");

  }

  @Override
  public String makeRequest(String stableId, String user, String clientIp, long startCoordinate,
      long endCoordinate) {

    if (StringUtils.isBlank(stableId)) {
      throw new RuntimeException("Need a stable id to log the request");
    }

    SqlParameterSource inParams = new MapSqlParameterSource()
        .addValue("sid", stableId)
        .addValue("uinfo", user)
        .addValue("cip", clientIp)
        .addValue("scoord", startCoordinate)
        .addValue("ecoord", endCoordinate);
    Map<String, Object> result = makeRequest.execute(inParams);
    return result.get("req_id").toString();
  }

  @Override
  public void downloadComplete(String requestId, long downloadSize, double speed) {
    if (StringUtils.isBlank(requestId)) {
      throw new RuntimeException("Need a requestId to complete the request");
    }
    SqlParameterSource inParams = new MapSqlParameterSource()
        .addValue("rid", requestId)
        .addValue("dlsize", downloadSize)
        .addValue("s", speed);
    downloadComplete.execute(inParams);
  }

  @Override
  public void setError(String requestId, String hostname, String code, String description) {
    if (StringUtils.isBlank(requestId)) {
      throw new RuntimeException("Need a requestId to log the error");
    }
    SqlParameterSource inParams = new MapSqlParameterSource()
        .addValue("rid", requestId)
        .addValue("h", hostname)
        .addValue("etype", code)
        .addValue("msg", description);
    setError.execute(inParams);
  }

}
