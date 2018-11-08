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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import eu.elixir.ega.ebi.htsget.config.MyUserAuthenticationConverter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.test.context.TestPropertySource;

/**
 * Test class for {@link MyUserAuthenticationConverter}.
 * 
 * @author amohan
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(MyUserAuthenticationConverter.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class MyUserAuthenticationConverterTest {

    private final String USERNAME = "user_id";
    private final String USERNAME_VAL = "USERNAME";
    private final String DATASET1 = "DATASET1";
    private final String DATASET2 = "DATASET2";
    private final String AUTHORITIES = AccessTokenConverter.AUTHORITIES;
    private UserDetails user;
    private Authentication authentication;

    @SuppressWarnings("rawtypes")
    private Collection authorities;

    @InjectMocks
    private MyUserAuthenticationConverter myUserAuthenticationConverter;

    @Mock
    private UserDetailsService userDetailsService;

    @SuppressWarnings("unchecked")
    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        authentication = mock(Authentication.class);
        user = mock(UserDetails.class);
        when(authentication.getName()).thenReturn(USERNAME_VAL);

        authorities = new ArrayList<GrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority(DATASET1));
        authorities.add(new SimpleGrantedAuthority(DATASET2));

        when(authentication.getAuthorities()).thenReturn(authorities);
        when(user.getAuthorities()).thenReturn(authorities);
        when(userDetailsService.loadUserByUsername(any())).thenReturn(user);

    }

    /**
     * Test class for
     * {@link MyUserAuthenticationConverter#convertUserAuthentication(Authentication)}.
     * Verify the response username and authorities.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertUserAuthentication() {
        final Map<String, Object> output = (Map<String, Object>) myUserAuthenticationConverter
                .convertUserAuthentication(authentication);

        final Set<Object> expectedOutput = new HashSet<Object>();
        expectedOutput.add(DATASET1);
        expectedOutput.add(DATASET2);

        assertThat(output.get(USERNAME), equalTo(USERNAME_VAL));
        assertThat(output.get(AUTHORITIES), equalTo(expectedOutput));
    }

    /**
     * Test class for
     * {@link MyUserAuthenticationConverter#extractAuthentication(Map)}. Verify the
     * response user principal and user authorities.
     */
    @Test
    public void testExtractAuthentication() {
        final Map<String, String> input = new HashMap<String, String>();
        input.put(USERNAME, USERNAME_VAL);

        final UsernamePasswordAuthenticationToken output = (UsernamePasswordAuthenticationToken) myUserAuthenticationConverter
                .extractAuthentication(input);

        assertThat(output.getPrincipal(), equalTo(user));
        assertThat(output.getAuthorities(), equalTo(authorities));
    }

}
