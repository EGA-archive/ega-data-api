package eu.elixir.ega.ebi.dataedge.service.internal;


import eu.elixir.ega.ebi.dataedge.config.GeneralStreamingException;
import eu.elixir.ega.ebi.dataedge.config.PermissionDeniedException;
import eu.elixir.ega.ebi.dataedge.service.AuthenticationService;
import eu.elixir.ega.ebi.dataedge.service.PermissionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Profile("external-permissions")
@Primary
@Service
public class ExternalPermissionsServiceImpl implements PermissionsService {

  //Todo log denied requests?

  @Autowired
  RestTemplate restTemplate;

  AuthenticationService authenticationService;

  @Value("${localega.permissions.url}")
  private String url;

  @Autowired
  public ExternalPermissionsServiceImpl(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @Override
  public String getFilePermissionsEntity(String stableId) {

    if (stableId == null || stableId.isEmpty()) {
      throw new GeneralStreamingException("Need a valid stableId to proceed.", 0);
    }

    Authentication authentication = authenticationService.getAuthentication();
    String token = null;
    if (authentication instanceof OAuth2Authentication) {
      Object authenticationDetails = authentication.getDetails();
      if (authenticationDetails != null) {
        if (authenticationDetails instanceof OAuth2AuthenticationDetails) {
          String tokenType = ((OAuth2AuthenticationDetails) authenticationDetails).getTokenType();
          String tokenValue = ((OAuth2AuthenticationDetails) authenticationDetails).getTokenValue();
          if ("bearer".equalsIgnoreCase(tokenType) && tokenValue != null && !tokenValue.isEmpty()) {
            token = tokenValue;
          }
        }
      }
    }

    if (token == null || token.isEmpty()) {
      throw new PermissionDeniedException("Don't have correct authorization to do this");
    }

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token );
    HttpEntity<String> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response = restTemplate.exchange(url + "/files/" + stableId, HttpMethod.GET, entity, String.class);
    if (response == null || response.getStatusCode()!= HttpStatus.OK) {
      throw new PermissionDeniedException(HttpStatus.UNAUTHORIZED.toString());
    }
    return response.getBody();
  }
}
