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
package eu.elixir.ega.ebi.keyproviderservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author asenf
 */
@Configuration
public class MyConfiguration {

    @Value("${ega.key.path}")
    private String[] cipherKeyPath;
    @Value("${ega.keypass.path}")
    private String[] cipherKeyPassPath;
    @Value("${ega.sharedpass.path}")
    private String sharedKeyPath;
    @Value("${ega.publickey.url}")
    private String publicKeyUrl;
    @Value("${ega.legacy.path}")
    private String egaLegacyPath;

    @Bean
    public MyCipherConfig MyCipherConfig() {
        return new MyCipherConfig(cipherKeyPath, 
                                  cipherKeyPassPath,
                                  sharedKeyPath,
                                  publicKeyUrl,
                                  egaLegacyPath);
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
