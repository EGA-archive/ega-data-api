package eu.elixir.ega.ebi.dataedge.service;

import eu.elixir.ega.ebi.shared.dto.File;
import javax.servlet.http.HttpServletRequest;

public interface PermissionsService {

  File getReqFile(String fileId, HttpServletRequest request);

}
