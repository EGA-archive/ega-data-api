package eu.elixir.ega.ebi.shared.service;

import eu.elixir.ega.ebi.shared.dto.File;

public interface FileInfoService {

  /**
   * Fetches the file information for a given stable ID.
   *
   * @param filestableId stable ID of the requested file
   * @return {@link File} description of the file with the requested
   *     {@code fileId}
   */
  File getFileInfo(String filestableId);

}
