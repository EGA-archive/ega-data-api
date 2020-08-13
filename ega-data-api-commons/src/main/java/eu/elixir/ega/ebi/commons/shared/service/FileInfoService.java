package eu.elixir.ega.ebi.commons.shared.service;


import eu.elixir.ega.ebi.commons.shared.dto.File;
import eu.elixir.ega.ebi.commons.shared.dto.FileIndexFile;

public interface FileInfoService {

  /**
   * Fetches the file information for a given stable ID.
   *
   * @param filestableId stable ID of the requested file
   * @return {@link File} description of the file with the requested
   *     {@code fileId}
   */
  File getFileInfo(String filestableId);

  FileIndexFile getFileIndexFile(String fileId);

}
