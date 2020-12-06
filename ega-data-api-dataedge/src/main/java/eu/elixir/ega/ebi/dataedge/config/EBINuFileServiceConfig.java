/*
 *
 * Copyright 2020 EMBL - European Bioinformatics Institute
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
package eu.elixir.ega.ebi.dataedge.config;

import eu.elixir.ega.ebi.dataedge.service.FileMetaService;
import eu.elixir.ega.ebi.dataedge.service.KeyService;
import eu.elixir.ega.ebi.dataedge.service.NuFileService;
import eu.elixir.ega.ebi.dataedge.service.internal.EBINuFileService;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.ega.fire.properties.HttpClientProperties;
import uk.ac.ebi.ega.fire.service.FireService;
import uk.ac.ebi.ega.fire.service.IFireService;

import java.util.Base64;
import java.util.Collections;

@Configuration
public class EBINuFileServiceConfig {

    @Value("${fire.user:testUser}")
    private String fireUsername;
    @Value("${fire.password:testPass}")
    private String firePassword;
    @Value("${fire.url:testUrl}")
    private String fireURL;

    @Bean
    public NuFileService nuFileService(KeyService keyService,
                                       FileMetaService fileDatabase,
                                       IFireService fireService) {
        return new EBINuFileService(keyService, fileDatabase, fireService);
    }

    @ConfigurationProperties(prefix = "httpclient.connection")
    @Bean
    public HttpClientProperties initHttpClientProperties() {
        return new HttpClientProperties();
    }

    @Bean
    public CloseableHttpClient initHttpClient(final HttpClientProperties httpClientProperties) {

        final ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setBufferSize(httpClientProperties.getBufferSize())
                .build();

        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(httpClientProperties.getMaxTotal());
        connectionManager.setDefaultMaxPerRoute(httpClientProperties.getDefaultMaxPerRoute());
        connectionManager.setValidateAfterInactivity(5000);

        final RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(httpClientProperties.getTimeout() * 1000)
                .setConnectionRequestTimeout(httpClientProperties.getTimeout() * 1000)
                .setSocketTimeout(0)
                .build();

        return HttpClients.custom()
                .setDefaultConnectionConfig(connectionConfig)
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setDefaultHeaders(Collections.singleton(new BasicHeader("Authorization", "Basic ".concat(base64EncodedCredentials()))))
                .build();
    }

    @Bean
    public IFireService initFireService(final CloseableHttpClient httpClient) {
        return new FireService(httpClient, fireURL, 3);
    }

    private String base64EncodedCredentials() {
        return Base64.getEncoder().encodeToString((fireUsername + ":" + firePassword).getBytes());
    }
}
