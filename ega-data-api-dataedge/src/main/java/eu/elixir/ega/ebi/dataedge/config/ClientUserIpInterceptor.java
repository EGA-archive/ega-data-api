package eu.elixir.ega.ebi.dataedge.config;

import eu.elixir.ega.ebi.dataedge.service.AuthenticationService;
import java.io.IOException;
import java.net.InetAddress;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class ClientUserIpInterceptor implements ClientHttpRequestInterceptor {

  private static final String X_FORWARDED_FOR = "X-FORWARDED-FOR";
  private static final String X_USER_NAME = "X-USER-NAME";

  @Autowired
  private AuthenticationService authenticationService;

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body,
      ClientHttpRequestExecution execution) throws IOException {

    String ipValue = null;
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes != null) {
      HttpServletRequest servletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
      ipValue = servletRequest.getHeader(X_FORWARDED_FOR);
      if (ipValue == null || ipValue.length() == 0) {
        ipValue = servletRequest.getRemoteAddr();
      }
    } else {
      // We are outside a web request
      InetAddress ip = InetAddress.getLocalHost();
      if (ip != null) {
        ipValue = ip.getHostAddress();
      }
    }

    HttpHeaders headers = request.getHeaders();
    if (headers == null) {
      headers = new HttpHeaders();
    }
    headers.add(X_FORWARDED_FOR, ipValue);

    headers.add(X_USER_NAME, authenticationService.getName());

    return execution.execute(request, body);
  }

}