package eu.elixir.ega.ebi.shared.service;

public interface PermissionsService {

  //returns the dataset that got the user permission for this stableId
  String getFilePermissionsEntity(Integer stableId);

}
