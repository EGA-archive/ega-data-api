package eu.elixir.ega.ebi.shared.service.internal;

import eu.elixir.ega.ebi.dataedge.config.CustomUsernamePasswordAuthenticationToken;
import eu.elixir.ega.ebi.shared.service.AuthenticationService;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

  @Override
  public Authentication getAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }

  @Override
  public String getName() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null) {
      return authentication.getName();
    }
    return null;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null) {
      return authentication.getAuthorities();
    }
    return null;
  }

    @Override
    public String getDatasetIdByStableId(String stableId) {
        if (getAuthentication() instanceof OAuth2Authentication) {
            OAuth2Authentication authentication = (OAuth2Authentication) getAuthentication();
            if (authentication != null && authentication.getUserAuthentication() instanceof CustomUsernamePasswordAuthenticationToken) {
                CustomUsernamePasswordAuthenticationToken athenticationToken = (CustomUsernamePasswordAuthenticationToken) authentication.getUserAuthentication();
                Map<String, List<String>> datasetFileMapping = athenticationToken.getDatasetFileMapping();
                if (datasetFileMapping != null) {
                    String datasetId = datasetFileMapping.entrySet().parallelStream()
                            .filter(e -> e.getValue().contains(stableId)).map(Map.Entry::getKey).findFirst()
                            .orElse(null);
                    if (datasetId != null) {
                        return datasetId;
                    }
                }
            }
        }
        return null;
    }

}
