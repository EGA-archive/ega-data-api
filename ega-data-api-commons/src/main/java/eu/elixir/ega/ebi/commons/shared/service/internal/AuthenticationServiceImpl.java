package eu.elixir.ega.ebi.commons.shared.service.internal;

import eu.elixir.ega.ebi.commons.config.CustomUsernamePasswordAuthenticationToken;
import eu.elixir.ega.ebi.commons.shared.service.AuthenticationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

  /**
   * Returns the authentication information from the current security context.
   *
   * @return The current authentication or null.
   */
  @Override
  public Authentication getAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }

  /**
   * Returns the login name from the current security context.
   *
   * @return The login name or null.
   */
  @Override
  public String getName() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null) {
      return authentication.getName();
    }
    return null;
  }

  /**
   * Returns the authentication authorities of the current security context.
   *
   * @return The collection of authorities, or null.
   */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null) {
      return authentication.getAuthorities();
    }
    return null;
  }

  /**
   * Returns a map of datasets and included files for the current authentication
   * context.
   *
   * @return A map of datasets and included files, or null.
   */
  @Override
  public Map<String, List<String>> getDatasetFileMapping() {
    if (getAuthentication() instanceof OAuth2Authentication) {
      OAuth2Authentication authentication = (OAuth2Authentication) getAuthentication();
      if (authentication != null
          && authentication.getUserAuthentication() instanceof CustomUsernamePasswordAuthenticationToken) {
        CustomUsernamePasswordAuthenticationToken authenticationToken = (CustomUsernamePasswordAuthenticationToken) authentication
            .getUserAuthentication();
        return authenticationToken.getDatasetFileMapping();
      }
    }
    return null;
  }

}
