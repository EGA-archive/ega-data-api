/*
 * Copyright 2016 ELIXIR EGA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.elixir.ega.ebi.dataedge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.security.oauth2.provider.authentication.TokenExtractor;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author asenf
 */
@Order(1)
@Configuration
@EnableCaching
@EnableResourceServer
public class OAuth2ResourceConfig extends ResourceServerConfigurerAdapter {

    private TokenExtractor tokenExtractor = new BearerTokenExtractor();

    /**
     * Configures the given http security object with the proper security settings,
     * and the following endpoints:
     * <ul>
     * <li>/files/**
     * <li>/metadata/**
     * <li>/demo/**
     * <li>/download/file/**
     * </ul>
     *
     * OPTIONS requests will be permitted on all enpoints, and CSRF tokens will
     * be disabled.
     *
     * @param http
     * @return
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.addFilterAfter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                // We don't want to allow access to a resource with no token so clear
                // the security context in case it is actually an OAuth2Authentication
                if (tokenExtractor.extract(request) == null) {
                    SecurityContextHolder.clearContext();
                }
                filterChain.doFilter(request, response);
            }
        }, AbstractPreAuthenticatedProcessingFilter.class);
        http
                .cors()
                .and()
                .requestMatchers()
                .antMatchers("/files/**")
                .antMatchers("/metadata/**")
                .antMatchers("/demo/**")
                .antMatchers("/download/file/**")
                .and()
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .csrf().disable();
    }

    /**
     * This is a bit of a Hack! MitreID doesn't return 'user_name' but 'user_id',
     * The customized User Authentication Converter simply changes the field name
     * for extraction.
     *
     * @return The converted access token
     */
    @Bean
    public AccessTokenConverter accessTokenConverter() {
        MyAccessTokenConverter myAccessTokenConverter = new MyAccessTokenConverter();
        myAccessTokenConverter.setUserTokenConverter(new MyUserAuthenticationConverter());
        return myAccessTokenConverter;
    }

    /**
     * Creates a combined token service including both EGA AAI and Elixir AAI
     * authentication.
     *
     * @param request Unused
     * @param checkTokenUrl EGA AAI token endpoint url
     * @param clientId Client id for the EGA AAI
     * @param clientSecret Client secret for the EGA AAI
     * @param zuulCheckTokenUrl Elixir token endpoint url
     * @param zuulClientId Elixir AAI ID
     * @param zuulClientSecret Elixir AAI client
     * @return A combined authentication token service
     */
    @Profile("enable-aai")
    @Primary
    @Bean
    public RemoteTokenServices remoteTokenServices(HttpServletRequest request,
                                                   final @Value("${auth.server.url}") String checkTokenUrl,
                                                   final @Value("${auth.server.clientId}") String clientId,
                                                   final @Value("${auth.server.clientsecret}") String clientSecret,
                                                   final @Value("${auth.zuul.server.url}") String zuulCheckTokenUrl,
                                                   final @Value("${auth.zuul.server.clientId}") String zuulClientId,
                                                   final @Value("${auth.zuul.server.clientsecret}") String zuulClientSecret) {

        final CachingMultipleRemoteTokenService remoteTokenServices = new CachingMultipleRemoteTokenService();

        // EGA AAI
        CachingRemoteTokenService b = new CachingRemoteTokenService();
        b.setCheckTokenEndpointUrl(checkTokenUrl);
        b.setClientId(clientId);
        b.setClientSecret(clientSecret);
        b.setAccessTokenConverter(accessTokenConverter());
        remoteTokenServices.addRemoteTokenService(b);

        // ELIXIR AAI
        CachingRemoteTokenService a = new CachingRemoteTokenService();
        a.setCheckTokenEndpointUrl(zuulCheckTokenUrl);
        a.setClientId(zuulClientId);
        a.setClientSecret(zuulClientSecret);
        remoteTokenServices.addRemoteTokenService(a);

        return remoteTokenServices;
    }

    /**
     * Sets CORS headers to allow all methods and all hosts.
     *
     * @return CORSfilter
     */
    @Bean
    @Order(0)
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

}
