package eu.elixir.ega.ebi.reencryptionmvc.config;

import org.cache2k.Cache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import eu.elixir.ega.ebi.reencryptionmvc.dto.CachePage;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaAESFileHeader;
import eu.elixir.ega.ebi.reencryptionmvc.service.ArchiveService;
import eu.elixir.ega.ebi.reencryptionmvc.service.KeyService;
import eu.elixir.ega.ebi.reencryptionmvc.service.ResService;
import eu.elixir.ega.ebi.reencryptionmvc.service.internal.CacheResServiceImpl;
import eu.elixir.ega.ebi.reencryptionmvc.service.internal.CleversaveArchiveServiceImpl;
import eu.elixir.ega.ebi.reencryptionmvc.util.FireCommons;
import eu.elixir.ega.ebi.reencryptionmvc.util.S3Commons;

@Configuration
@Profile("default")
@EnableDiscoveryClient
public class DefaultProfileConfiguration {

    @Value("${ega.ebi.fire.url}")
    private String fireUrl;
    @Value("${ega.ebi.fire.archive}")
    private String fireArchive;
    @Value("${ega.ebi.fire.key}")
    private String fireKey;

    @Value("${ega.ebi.aws.access.key:#{null}}")
    private String awsKey;
    @Value("${ega.ebi.aws.access.secret:#{null}}")
    private String awsSecretKey;
    @Value("${ega.ebi.aws.endpoint.url:#{null}}")
    private String awsEndpointUrl;
    @Value("${ega.ebi.aws.endpoint.region:#{null}}")
    private String awsRegion;

    @Bean
    @Primary
    public ResService initCacheResService(KeyService keyService, Cache<String, EgaAESFileHeader> myHeaderCache,
            Cache<String, CachePage> myPageCache) {
        return new CacheResServiceImpl(keyService, myHeaderCache, myPageCache,
                new FireCommons(fireUrl, fireArchive, fireKey),
                new S3Commons(awsKey, awsSecretKey, awsEndpointUrl, awsRegion));
    }

    @Bean
    @Primary
    public ArchiveService initCleversaveArchiveServiceImpl(RestTemplate restTemplate, KeyService keyService) {
        return new CleversaveArchiveServiceImpl(restTemplate, keyService,
                new FireCommons(fireUrl, fireArchive, fireKey));
    }

}
