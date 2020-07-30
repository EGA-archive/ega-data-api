/*
 * Copyright 2016 ELIXIR EBI
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
package eu.elixir.ega.ebi.htsget.config;

import eu.elixir.ega.ebi.commons.config.CachingMultipleRemoteTokenService;
import eu.elixir.ega.ebi.commons.config.CachingRemoteTokenService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Test class for {@link CachingMultipleRemoteTokenService}.
 *
 * @author amohan
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(CachingMultipleRemoteTokenService.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class CachingMultipleRemoteTokenServiceTest {

    private CachingRemoteTokenService cachingRemoteTokenService;
    private OAuth2Authentication oAuth2Authentication;
    private OAuth2AccessToken oAuth2AccessToken;

    @InjectMocks
    private CachingMultipleRemoteTokenService cachingMultipleRemoteTokenService;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        oAuth2Authentication = mock(OAuth2Authentication.class);
        oAuth2AccessToken = mock(OAuth2AccessToken.class);
        cachingRemoteTokenService = mock(CachingRemoteTokenService.class);
        when(cachingRemoteTokenService.loadAuthentication(any())).thenReturn(oAuth2Authentication);
        when(cachingRemoteTokenService.readAccessToken(any())).thenReturn(oAuth2AccessToken);
        cachingRemoteTokenService.setTokenName("tokenName");
        cachingMultipleRemoteTokenService.addRemoteTokenService(cachingRemoteTokenService);
    }

    /**
     * Test class for
     * {@link CachingMultipleRemoteTokenService#loadAuthentication(String)}. Verify
     * the response OAuth2Authentication.
     */
    @Test
    public void testLoadAuthenticationd() {
        assertThat(cachingMultipleRemoteTokenService.loadAuthentication("accessToken"), equalTo(oAuth2Authentication));
    }

    /**
     * Test class for
     * {@link CachingMultipleRemoteTokenService#readAccessToken(String)}. Verify the
     * read access token.
     */
    @Test
    public void testReadAccessToken() {
        assertThat(cachingMultipleRemoteTokenService.readAccessToken("accessToken"), equalTo(oAuth2AccessToken));
    }
}
