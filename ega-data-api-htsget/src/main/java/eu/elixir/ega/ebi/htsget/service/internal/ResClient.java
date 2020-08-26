package eu.elixir.ega.ebi.htsget.service.internal;

import htsjdk.samtools.seekablestream.SeekableHTTPStream;
import htsjdk.samtools.seekablestream.SeekableStream;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

import java.net.MalformedURLException;
import java.net.URL;

public class ResClient {
    private LoadBalancerClient loadBalancer;

    public ResClient(LoadBalancerClient loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public SeekableStream getStreamForFile(String id) throws MalformedURLException {
        URL resDataURL = new URL(resURL() + "/file/archive/" + id);
        return new SeekableHTTPStream(resDataURL);
    }

    private String resURL() {
        return loadBalancer.choose("RES2").getUri().toString() + "/ega-res";
    }
}
