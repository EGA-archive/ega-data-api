/*
 * Copyright 2020 ELIXIR EGA
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
package eu.elixir.ega.ebi.reencryptionmvc.service.internal;

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

import eu.elixir.ega.ebi.reencryptionmvc.service.S3Service;

public class S3ServiceImpl implements S3Service {

    private final String awsAccessKeyId;
    private final String awsSecretAccessKey;
    private final String awsEndpointUrl;
    private final String awsRegion;

    public S3ServiceImpl(String awsAccessKeyId, String awsSecretAccessKey, String awsEndpointUrl, String awsRegion) {
        this.awsAccessKeyId = awsAccessKeyId;
        this.awsSecretAccessKey = awsSecretAccessKey;
        this.awsEndpointUrl = awsEndpointUrl;
        this.awsRegion = awsRegion;
    }

    public String getS3ObjectUrl(String fileLocation) {
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
