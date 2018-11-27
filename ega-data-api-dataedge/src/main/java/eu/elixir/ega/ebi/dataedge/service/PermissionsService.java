package eu.elixir.ega.ebi.dataedge.service;

public interface PermissionsService {

  //returns the dataset that got the user permission for this stableId
  String getFilePermissionsEntity(String stableId);

}
