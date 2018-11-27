package eu.elixir.ega.ebi.dataedge.service;

import eu.elixir.ega.ebi.shared.dto.File;

public interface FileInfoService {

  File getFileInfo(String filestableId);

}
