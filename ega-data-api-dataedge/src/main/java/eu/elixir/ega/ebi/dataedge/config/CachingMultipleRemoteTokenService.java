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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

import java.util.ArrayList;

/**
 * @author asenf
 */
@EnableCaching
public class CachingMultipleRemoteTokenService extends RemoteTokenServices {

    /*
     * Additional Code to handle a list of token services
     */
    ArrayList<CachingRemoteTokenService> remoteServices;

    public void addRemoteTokenService(CachingRemoteTokenService service) {
        if (remoteServices == null)
            remoteServices = new ArrayList<>();

        remoteServices.add(service);
        System.out.println("-- service " + service.toString());
    }

    /*
     * Code adjusted to handle a list of remote services
     */
    @Override
    @Cacheable(cacheNames = "tokens", key = "#root.methodName + #accessToken")
    public OAuth2Authentication loadAuthentication(String accessToken)
            throws org.springframework.security.core.AuthenticationException,
            InvalidTokenException {
        System.out.println("1 -- " + accessToken);

        OAuth2Authentication loadAuthentication = null;
        for (CachingRemoteTokenService remoteService : remoteServices) {
            try {
                loadAuthentication = remoteService.loadAuthentication(accessToken);
            } catch (IllegalStateException ex) {
                System.out.println(ex.toString());
            }
            if (loadAuthentication != null && loadAuthentication.isAuthenticated()) break;
        }
        return loadAuthentication;

        //return super.loadAuthentication(accessToken);
    }

    @Override
    @Cacheable(cacheNames = "tokens", key = "#root.methodName + #accessToken")
    public OAuth2AccessToken readAccessToken(String accessToken) {
        System.out.println("2 -- " + accessToken);

        OAuth2AccessToken readAccess = null;
        for (int i = 0; i < remoteServices.size(); i++) {
            readAccess = remoteServices.get(i).readAccessToken(accessToken);
            if (!readAccess.isExpired()) break;
        }
        return readAccess;

        //return super.readAccessToken(accessToken);
    }

}