package eu.elixir.ega.ebi.shared.service;


import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public interface AuthenticationService {

  Authentication getAuthentication();

  String getName();

  Collection<? extends GrantedAuthority> getAuthorities();
  
  Map<String, List<String>> getDatasetFileMapping();
}