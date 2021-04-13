/*
 * Copyright 2017 ELIXIR EGA
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
package eu.elixir.ega.ebi.commons.config;

import com.nimbusds.jwt.SignedJWT;
import eu.elixir.ega.ebi.commons.shared.service.FileDatasetService;
import eu.elixir.ega.ebi.commons.shared.service.Ga4ghService;
import eu.elixir.ega.ebi.commons.shared.service.JWTService;
import eu.elixir.ega.ebi.commons.shared.service.UserDetailsService;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;
import org.springframework.security.oauth2.provider.token.UserAuthenticationConverter;

import java.net.URI;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toMap;

/**
 * @author asenf
 */
public class MyAccessTokenConverter implements AccessTokenConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyAccessTokenConverter.class);

    private UserAuthenticationConverter userTokenConverter;
    private boolean includeGrantType;
    private final JWTService jwtService;
    private final Ga4ghService ga4ghService;
    private final FileDatasetService fileDatasetService;
    private final UserDetailsService userDetailsService;

    public MyAccessTokenConverter(final JWTService jwtService,
                                  final Ga4ghService ga4ghService,
                                  final FileDatasetService fileDatasetService,
                                  final UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.ga4ghService = ga4ghService;
        this.fileDatasetService = fileDatasetService;
        this.userTokenConverter = new DefaultUserAuthenticationConverter();
        this.userDetailsService = userDetailsService;
    }

    public MyAccessTokenConverter(final JWTService jwtService,
                                  final Ga4ghService ga4ghService,
                                  final FileDatasetService fileDatasetService,
                                  final UserAuthenticationConverter userTokenConverter,
                                  final UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.ga4ghService = ga4ghService;
        this.fileDatasetService = fileDatasetService;
        this.userTokenConverter = userTokenConverter;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Converter for the part of the data in the token representing a user.
     *
     * @param userTokenConverter the userTokenConverter to set
     */
    public void setUserTokenConverter(UserAuthenticationConverter userTokenConverter) {
        this.userTokenConverter = userTokenConverter;
    }

    /**
     * Flag to indicate the the grant type should be included in the converted token.
     *
     * @param includeGrantType the flag value (default false)
     */
    public void setIncludeGrantType(boolean includeGrantType) {
        this.includeGrantType = includeGrantType;
    }

    /**
     * Extracts authentication information from the authentication object as
     * well as the the given authentication token, and returns a formatted,
     * combined authentication object.
     *
     * @param token The authentication token to convert
     * @param authentication Authentication object to extract information from.
     *
     * @return A {@link HashMap} of authentication tokens
     */
    public Map<String, ?> convertAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        OAuth2Request clientToken = authentication.getOAuth2Request();

        if (!authentication.isClientOnly()) {
            response.putAll(userTokenConverter.convertUserAuthentication(authentication.getUserAuthentication()));
        } else {
            if (clientToken.getAuthorities() != null && !clientToken.getAuthorities().isEmpty()) {
                response.put(UserAuthenticationConverter.AUTHORITIES,
                        AuthorityUtils.authorityListToSet(clientToken.getAuthorities()));
            }
        }

        if (token.getScope() != null) {
            response.put(SCOPE, token.getScope());
        }
        if (token.getAdditionalInformation().containsKey(JTI)) {
            response.put(JTI, token.getAdditionalInformation().get(JTI));
        }

        if (token.getExpiration() != null) {
            response.put(EXP, token.getExpiration().getTime() / 1000);
        }

        if (includeGrantType && authentication.getOAuth2Request().getGrantType() != null) {
            response.put(GRANT_TYPE, authentication.getOAuth2Request().getGrantType());
        }

        response.putAll(token.getAdditionalInformation());

        response.put(CLIENT_ID, clientToken.getClientId());
        if (clientToken.getResourceIds() != null && !clientToken.getResourceIds().isEmpty()) {
            response.put(AUD, clientToken.getResourceIds());
        }
        return response;
    }

    /**
     * Creates an authentication token with the value given in 'value', and the
     * information from 'map'.
     *
     * @param value Value to set in the return authentication token
     * @param map An authentication map, such as returned from
     *         {@link AccessTokenConverter}
     *
     * @return Access token containing the combined information
     */
    public OAuth2AccessToken extractAccessToken(String value, Map<String, ?> map) {
        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(value);
        Map<String, Object> info = new HashMap<>(map);
        info.remove(EXP);
        info.remove(AUD);
        info.remove(CLIENT_ID);
        info.remove(SCOPE);
        if (map.containsKey(EXP)) {
            token.setExpiration(new Date((Long) map.get(EXP) * 1000L));
        }
        if (map.containsKey(JTI)) {
            info.put(JTI, map.get(JTI));
        }
        token.setScope(extractScope(map));
        token.setAdditionalInformation(info);
        return token;
    }

    /**
     * Uses the information in the given authentication map to create an
     * authentication request.
     *
     * @param map An authentication map, such as returned from
     *         {@link AccessTokenConverter}
     *
     * @return An OAuth2 authentication object
     */
    @Override
    public OAuth2Authentication extractAuthentication(Map<String, ?> map) {

        final List<String> ga4ghDatasets;
        final String egaAccountId = getEGAAccountId(map);

        if (egaAccountId != null) {
            ga4ghDatasets = ga4ghService.getDatasets(egaAccountId);
        } else {
            ga4ghDatasets = emptyList();
        }

        final Map<String, List<String>> datasetFileMap = getDatasetFileMap(ga4ghDatasets);

        final Map<String, Object> info = new HashMap<>(map);
        info.put(AUTHORITIES, datasetFileMap);

        Map<String, String> parameters = new HashMap<>();
        Set<String> scope = extractScope(map);
        Authentication user = userTokenConverter.extractAuthentication(info); // {map} Use 'enhanced' Map with Permissions
        String clientId = (String) map.get(CLIENT_ID);
        parameters.put(CLIENT_ID, clientId);
        if (includeGrantType && map.containsKey(GRANT_TYPE)) {
            parameters.put(GRANT_TYPE, (String) map.get(GRANT_TYPE));
        }
        Set<String> resourceIds = new LinkedHashSet<>(map.containsKey(AUD) ? getAudience(map) : Collections.emptySet());

        Collection<? extends GrantedAuthority> authorities = null;
        if (user == null && map.containsKey(AUTHORITIES)) {
            @SuppressWarnings("unchecked")
            String[] roles = ((Collection<String>) map.get(AUTHORITIES)).toArray(new String[0]);
            authorities = AuthorityUtils.createAuthorityList(roles);
        }
        OAuth2Request request = new OAuth2Request(parameters, clientId, authorities, true, scope, resourceIds, null, null,
                null);
        return new OAuth2Authentication(request, user);
    }

    private String getEGAAccountId(final Map<String, ?> map) {
        return userDetailsService
                .getEGAAccountId((String) map.get("user_id"))
                .orElseGet(() -> userDetailsService
                        .getEGAAccountIdForElixirId((String) map.get("sub"))
                        .orElse(null));
    }

    private Map<String, List<String>> getDatasetFileMap(final List<String> ga4ghDatasets) {
        return ga4ghDatasets
                .parallelStream()
                .map(jwtService::parse)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(jwtService::isValidSignature)
                .filter(this::isValidToken)
                .map(this::getDatasetId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toMap(datasetId -> datasetId, fileDatasetService::getFileIds, (d1, d2) -> d2));
    }

    private boolean isValidToken(final SignedJWT signedJWT) {
        try {
            return new Date(currentTimeMillis())
                    .before(signedJWT.getJWTClaimsSet().getExpirationTime());
        } catch (ParseException e) {
            LOGGER.error("Unable to get JWT token expiration time from=" + e.getMessage(), e);
        }
        return false;
    }

    private Optional<String> getDatasetId(final SignedJWT signedJWT) {
        try {
            final JSONObject jsonObject = signedJWT.getJWTClaimsSet().toJSONObject();
            final JSONObject jsonObjectGa4ghVisa = (JSONObject) jsonObject.get("ga4gh_visa_v1");
            final String type = jsonObjectGa4ghVisa.getAsString("type");
            if ("ControlledAccessGrants".equals(type)) {
                final String uri = URI.create(jsonObjectGa4ghVisa.getAsString("value")).getPath();
                return of(uri.substring(uri.lastIndexOf("/") + 1));
            }
        } catch (ParseException e) {
            LOGGER.error("Unable to get JWT claims as JSON object=" + e.getMessage(), e);
        }
        return empty();
    }

    /**
     * Extracts the authentication audience (held in the 'AUD' key) from the
     * given authentication map.
     *
     * @param map An authentication map, such as returned from
     *         {@link AccessTokenConverter}
     *
     * @return The extracted authentication audience
     */
    private Collection<String> getAudience(Map<String, ?> map) {
        Object auds = map.get(AUD);
        if (auds instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<String> result = (Collection<String>) auds;
            return result;
        }
        return Collections.singleton((String) auds);
    }

    /**
     * Extracts the authentication scope (held in the 'SCOPE' key) from the
     * given authentication map.
     *
     * @param map An authentication map, such as returned from
     *         {@link AccessTokenConverter}
     *
     * @return The extracted authentication scope
     */
    private Set<String> extractScope(Map<String, ?> map) {
        Set<String> scope = Collections.emptySet();
        if (map.containsKey(SCOPE)) {
            Object scopeObj = map.get(SCOPE);
            if (scopeObj instanceof String) {
                scope = new LinkedHashSet<>(Arrays.asList(((String) scopeObj).split(" ")));
            } else if (Collection.class.isAssignableFrom(scopeObj.getClass())) {
                @SuppressWarnings("unchecked")
                Collection<String> scopeColl = (Collection<String>) scopeObj;
                scope = new LinkedHashSet<>(scopeColl);    // Preserve ordering
            }
        }
        return scope;
    }
}
