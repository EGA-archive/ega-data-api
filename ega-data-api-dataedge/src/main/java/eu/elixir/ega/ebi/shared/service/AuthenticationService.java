package eu.elixir.ega.ebi.shared.service;


import java.util.Collection;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public interface AuthenticationService {

  Authentication getAuthentication();

  String getName();

  Collection<? extends GrantedAuthority> getAuthorities();
  
  String getDatasetIdByStableId(String stableId);

}