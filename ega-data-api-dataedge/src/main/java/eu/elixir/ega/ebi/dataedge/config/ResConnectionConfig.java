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

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class ResConnectionConfig {
    @Value("${res.connection.pool.size}")
    private int connectionPoolSize;

    @Value("${res.connection.pool.keepalive.ms}")
    private int connectionPoolKeepAlive;

    @Value("${res.connection.timeout.connect.ms}")
    private int resConnectionConnectTimeout;

    @Value("${res.connection.timeout.read.ms}")
    private int resConnectionReadTimeout;

    @Bean
    public ConnectionPool connectionPool() {
        return new ConnectionPool(connectionPoolSize, connectionPoolKeepAlive, TimeUnit.MILLISECONDS);
    }

    @Bean
    public OkHttpClient resConnectionClient(@Autowired ConnectionPool connectionPool) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectionPool(connectionPool);

        if (resConnectionConnectTimeout > 0)
            builder = builder.connectTimeout(resConnectionConnectTimeout, TimeUnit.MILLISECONDS);

        if (resConnectionReadTimeout > 0)
            builder = builder.readTimeout(resConnectionReadTimeout, TimeUnit.MILLISECONDS);

        return builder.build();
    }
}
