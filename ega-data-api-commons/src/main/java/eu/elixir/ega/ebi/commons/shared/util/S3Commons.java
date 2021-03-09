package eu.elixir.ega.ebi.commons.shared.util;

import static com.amazonaws.HttpMethod.GET;

import java.net.URL;
import java.util.Date;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class S3Commons {
    
    private final String awsAccessKeyId;
    private final String awsSecretAccessKey;
    private final String awsEndpointUrl;
    private final String awsRegion;
    
    public S3Commons(String awsAccessKeyId, String awsSecretAccessKey, String awsEndpointUrl, String awsRegion) {
        this.awsAccessKeyId = awsAccessKeyId;
        this.awsSecretAccessKey = awsSecretAccessKey;
        this.awsEndpointUrl = awsEndpointUrl;
        this.awsRegion = awsRegion;
    }


    public String getS3ObjectUrl(String fileLocation) {
        log.info("Inside load loadHeaders3 - 2" + awsEndpointUrl + "==" + awsRegion);
        // Load first 16 bytes; set stats
        final String bucket = fileLocation.substring(5, fileLocation.indexOf("/", 5));
        final String awsPath = fileLocation.substring(fileLocation.indexOf("/", 5) + 1);

        final AWSCredentials credentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials)).withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(awsEndpointUrl, awsRegion))
                .build();

        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += (1000 * 3600) * 24;
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucket, awsPath)
                .withMethod(GET).withExpiration(expiration);
        URL url = s3.generatePresignedUrl(generatePresignedUrlRequest);

        return url.toString();
    }

}
