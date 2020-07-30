package eu.elixir.ega.ebi.commons.shared.service;

public interface PermissionsService {

  /**
   * Returns the dataset that got the user permission for a given stableId
   *
   * @param stableId the stable ID of a system resource.
   * @return Dataset name.
   */
  String getFilePermissionsEntity(String stableId);

}
