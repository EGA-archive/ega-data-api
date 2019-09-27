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
package eu.elixir.ega.ebi.dataedge.config;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

/**
 * @author asenf
 */
@Slf4j
@EnableCaching
public class CachingMultipleRemoteTokenService extends RemoteTokenServices {

    /*
     * Additional Code to handle a list of token services
     */
    ArrayList<CachingRemoteTokenService> remoteServices;

    /**
     * Adds a new remote token service to the list of remote services, and
     * creates the remoteServices list if it's not yet initialized.
     *
     * @param service A remote token service to add.
     */
    public void addRemoteTokenService(CachingRemoteTokenService service) {
        if (remoteServices == null)
            remoteServices = new ArrayList<>();

        remoteServices.add(service);
        log.info("-- service " + service.toString());
    }

    /**
     * Tests the given accessToken against the list of remote token services.
     *
     * @param accessToken Token to test against the remote token services.
     * @return The authenticated token, or null if the token could not be
     *     authenticated.
     */
    @Override
    @Cacheable(cacheNames = "tokens", key = "#root.methodName + #accessToken")
    public OAuth2Authentication loadAuthentication(String accessToken)
            throws org.springframework.security.core.AuthenticationException,
            InvalidTokenException {
        log.info("1 -- " + accessToken);

        OAuth2Authentication loadAuthentication = null;
        for (CachingRemoteTokenService remoteService : remoteServices) {
            try {
                loadAuthentication = remoteService.loadAuthentication(accessToken);
            } catch (IllegalStateException ex) {
                log.error(ex.toString());
            }
            if (loadAuthentication != null && loadAuthentication.isAuthenticated()) break;
        }
        return loadAuthentication;
    }

    /**
     * Tests the given accessToken against the list of remote token services to
     * see if the token grants read access.
     *
     * @param accessToken Token to test against the remote token services.
     * @return The read access token, or null if the token could not be
     *     authenticated.
     */
    @Override
    @Cacheable(cacheNames = "tokens", key = "#root.methodName + #accessToken")
    public OAuth2AccessToken readAccessToken(String accessToken) {
        log.info("2 -- " + accessToken);

        OAuth2AccessToken readAccess = null;
        for (int i = 0; i < remoteServices.size(); i++) {
            readAccess = remoteServices.get(i).readAccessToken(accessToken);
            if (!readAccess.isExpired()) break;
        }
        return readAccess;
    }

}
