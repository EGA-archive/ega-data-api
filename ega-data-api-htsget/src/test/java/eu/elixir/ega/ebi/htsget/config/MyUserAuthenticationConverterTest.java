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

import eu.elixir.ega.ebi.commons.config.CustomUsernamePasswordAuthenticationToken;
import eu.elixir.ega.ebi.commons.config.MyUserAuthenticationConverter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link MyUserAuthenticationConverter}.
 *
 * @author amohan
 */
@RunWith(MockitoJUnitRunner.class)
@PrepareForTest(MyUserAuthenticationConverter.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class MyUserAuthenticationConverterTest {
    private final String USERNAME = "user_id";
    private final String USERNAME_VAL = "USERNAME";
    private final String DATASET1 = "EGAD00000000001";
    private final String DATASET2 = "EGAD00000000002";
    private final String AUTHORITIES = AccessTokenConverter.AUTHORITIES;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private MyUserAuthenticationConverter myUserAuthenticationConverter;

    /**
     * Test class for
     * {@link MyUserAuthenticationConverter#convertUserAuthentication(Authentication)}.
     * Verify the response username and authorities.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertUserAuthentication() {
        when(authentication.getName()).thenReturn(USERNAME_VAL);

        final Map<String, Object> output = (Map<String, Object>) myUserAuthenticationConverter
                .convertUserAuthentication(authentication);

        assertThat(output.get(USERNAME)).isEqualTo(USERNAME_VAL);
    }

    /**
     * Test class for
     * {@link MyUserAuthenticationConverter#extractAuthentication(Map)}. Verify the
     * response user principal and user authorities.
     */
    @Test
    public void testExtractAuthentication() {
        final Map<String, List<String>> authorities = new HashMap<>();
        final List<String> fileIds = asList(
                "EGAF00000000001",
                "EGAF00000000002",
                "EGAF00000000003",
                "EGAF00000000004"
        );
        authorities.put(DATASET1, fileIds);
        authorities.put(DATASET2, fileIds);

        final Map<String, Object> input = new HashMap<>();
        input.put(USERNAME, USERNAME_VAL);
        input.put(AUTHORITIES, authorities);

        final CustomUsernamePasswordAuthenticationToken output = (CustomUsernamePasswordAuthenticationToken) myUserAuthenticationConverter
                .extractAuthentication(input);

        assertThat(output.getAuthorities())
                .containsAll(asList(
                        new SimpleGrantedAuthority(DATASET1),
                        new SimpleGrantedAuthority(DATASET2)
                ));
        assertThat(output.getDatasetFileMapping().get(DATASET1))
                .isEqualTo(fileIds);
    }
}
