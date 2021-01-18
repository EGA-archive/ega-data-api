/*
 *
 * Copyright 2021 EMBL - European Bioinformatics Institute
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
 *
 */
package eu.elixir.ega.ebi.commons.shared.service.internal;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import eu.elixir.ega.ebi.commons.shared.service.JWTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

public class JWTServiceImpl implements JWTService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JWTServiceImpl.class);

    private final JWKSet jwkSetPublicKey;
    private final String keyId;

    public JWTServiceImpl(final JWKSet jwkSetPublicKey,
                          final String keyId) {
        this.jwkSetPublicKey = jwkSetPublicKey;
        this.keyId = keyId;
    }

    @Override
    public Optional<SignedJWT> parse(final String signedJWTString) {
        try {
            return of(SignedJWT.parse(signedJWTString));
        } catch (ParseException e) {
            LOGGER.error("Error while parsing jwt string=" + e.getMessage(), e);
        }
        return empty();
    }

    @Override
    public boolean isValidSignature(final SignedJWT signedJWT) {
        try {
            final JWK jwk = jwkSetPublicKey.getKeyByKeyId(keyId);
            final RSASSAVerifier verifier = new RSASSAVerifier((RSAKey) jwk);
            return signedJWT.verify(verifier);
        } catch (JOSEException e) {
            LOGGER.error("Error while validating jwt signature=" + e.getMessage(), e);
        }
        return false;
    }
}
