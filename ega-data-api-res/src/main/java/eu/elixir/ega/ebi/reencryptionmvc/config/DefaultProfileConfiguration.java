package eu.elixir.ega.ebi.reencryptionmvc.config;

import java.util.Base64;

import org.apache.http.impl.client.CloseableHttpClient;
import org.cache2k.Cache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import eu.elixir.ega.ebi.reencryptionmvc.cache2k.My2KCachePageFactory;
import eu.elixir.ega.ebi.reencryptionmvc.dto.EgaAESFileHeader;
import eu.elixir.ega.ebi.reencryptionmvc.service.ArchiveService;
import eu.elixir.ega.ebi.reencryptionmvc.service.KeyService;
import eu.elixir.ega.ebi.reencryptionmvc.service.ResService;
import eu.elixir.ega.ebi.reencryptionmvc.service.internal.CacheResServiceImpl;
import eu.elixir.ega.ebi.reencryptionmvc.service.internal.CleversaveArchiveServiceImpl;
import eu.elixir.ega.ebi.reencryptionmvc.util.FireCommons;
import eu.elixir.ega.ebi.reencryptionmvc.util.S3Commons;
import uk.ac.ebi.ega.fire.service.IFireService;

@Configuration
@Profile("default")
@EnableDiscoveryClient
public class DefaultProfileConfiguration {

    @Value("${fire.user:testUser}")
    private String fireUsername;
    @Value("${fire.password:testPass}")
    private String firePassword;
    @Value("${fire.url:testUrl}")
    private String fireURL;

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
            My2KCachePageFactory pageDownloader, IFireService fireService, CloseableHttpClient httpClient) {
        return new CacheResServiceImpl(keyService, myHeaderCache, pageDownloader,
                new FireCommons(fireURL, base64EncodedCredentials(), fireService),
                new S3Commons(awsKey, awsSecretKey, awsEndpointUrl, awsRegion), httpClient);
    }

    @Bean
    @Primary
    public ArchiveService initCleversaveArchiveServiceImpl(RestTemplate restTemplate, KeyService keyService,
            IFireService fireService) {
        return new CleversaveArchiveServiceImpl(restTemplate, keyService,
                new FireCommons(fireURL, base64EncodedCredentials(), fireService));
    }

    private String base64EncodedCredentials() {
        return Base64.getEncoder().encodeToString((fireUsername + ":" + firePassword).getBytes());
    }
}
