package eu.elixir.ega.ebi.shared.service;

import eu.elixir.ega.ebi.shared.dto.File;

public interface FileInfoService {

  File getFileInfo(Integer filestableId);

}
