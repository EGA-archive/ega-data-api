package eu.elixir.ega.ebi.commons.shared.service;


import eu.elixir.ega.ebi.commons.shared.dto.File;
import eu.elixir.ega.ebi.commons.shared.dto.FileIndexFile;

public interface FileInfoService {

  File getFileInfo(String filestableId, String sessionId);

  FileIndexFile getFileIndexFile(String fileId);

}
