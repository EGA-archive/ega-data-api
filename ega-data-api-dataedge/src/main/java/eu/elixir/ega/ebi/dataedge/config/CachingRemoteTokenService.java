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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

/**
 * @author asenf
 */
public class CachingRemoteTokenService extends RemoteTokenServices {

    private static Log log = LogFactory.getLog(CachingRemoteTokenService.class);

    @Override
    @Cacheable(cacheNames = "tokens", key = "#root.methodName + #accessToken")
    public OAuth2Authentication loadAuthentication(String accessToken)
            throws org.springframework.security.core.AuthenticationException,
            InvalidTokenException {
        log.info("loadAuthentication: " + accessToken);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Entering CachingRemoteTokenService auth: " + auth);
        return super.loadAuthentication(accessToken);
    }

    @Override
    @Cacheable(cacheNames = "tokens", key = "#root.methodName + #accessToken")
    public OAuth2AccessToken readAccessToken(String accessToken) {
        log.info("readAccessToken: " + accessToken);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Entering CachingRemoteTokenService auth: " + auth);
        return super.readAccessToken(accessToken);
    }
}