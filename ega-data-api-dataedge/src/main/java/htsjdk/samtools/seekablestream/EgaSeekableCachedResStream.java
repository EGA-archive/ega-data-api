/*
 * Copyright 2017 ELIXIR EGA
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
package htsjdk.samtools.seekablestream;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.HttpUtils;

import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Attach a SeekableStream directly to a RES_MVC microservice.
 * This is for archive internal usage - there is no access control level in this
 * stream. Other microservices than RES are responsible for AuthN/Z
 * 'auth' is retained in case RES_MVC is deployed with Basic Auth control
 * <p>
 * Assume: File Archive ID specified as part of URL upin instantiation.
 * Destination Format, Key specified upon use; not known upon instantiation.
 */

public class EgaSeekableCachedResStream extends SeekableStream {
    // Build logger
    private Logger logger = LoggerFactory.getLogger(EgaSeekableCachedResStream.class);

    // HTTP Client to access CleverSafe
    private OkHttpClient client;

    private LoadingCache<Integer, byte[]> cache;
    private static final int PAGE_SIZE = 1024 * 1024;
    private static final int NUM_PAGES = 15;

    private long position = 0;
    private long contentLength = -1;
    private final URL url;  // RES_MVC Microservice
    private final Proxy proxy;
    private final String auth; // Basic Auth (optional)

    private boolean hack = false;
    private String hack_extension = "";

    public EgaSeekableCachedResStream(final URL url) {
        this(url, null, null);
    }

    public EgaSeekableCachedResStream(final URL url, String auth) {
        this(url, null, auth);
    }

    public EgaSeekableCachedResStream(final URL url, Proxy proxy) {
        this(url, proxy, null);
    }

    public EgaSeekableCachedResStream(final URL url, Proxy proxy, String auth) {
        this(url, proxy, auth, -1);
    }

    public EgaSeekableCachedResStream(final URL url, Proxy proxy, String auth, long fileSize) {

        this.proxy = proxy;
        this.url = url;
        this.auth = auth;
        this.contentLength = fileSize - 16; // This is true for AES Encrypted Streams (first 16 bytes = IV)

        // Instatiate HTTP client
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        ConnectionPool cp = new ConnectionPool(NUM_PAGES, 5000, TimeUnit.MILLISECONDS);
        client = builder.retryOnConnectionFailure(true)
                .connectionPool(cp)
                .connectTimeout(2500, TimeUnit.MILLISECONDS).build();

        // Init cache 
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(NUM_PAGES)
                .concurrencyLevel(NUM_PAGES)
                .build(
                        new CacheLoader<Integer, byte[]>() {
                            public byte[] load(Integer page) throws Exception {
                                return populateCache(page);
                            }
                        });

        // Try to get the file length
        // Note: This also sets setDefaultUseCaches(false), which is important
        final String contentLengthString = HttpUtils.getHeaderField(url, "Content-Length");
        if (contentLengthString != null && contentLength == -1) {
            try {
                logger.info("this is the content length: {}", contentLengthString);
                contentLength = Long.parseLong(contentLengthString) - 16;
            } catch (NumberFormatException ignored) {
                logger.warn("WARNING: Invalid content length {} for the: {} ", contentLengthString, url);
                contentLength = -1;
            }
        }

    }

    public long position() {
        return position;
    }

    public long length() {
        return contentLength;
    }

    @Override
    public long skip(long n) throws IOException {
        long bytesToSkip = Math.min(n, contentLength - position);
        position += bytesToSkip;
        return bytesToSkip;
    }

    public boolean eof() throws IOException {
        return contentLength > 0 && position >= contentLength;
    }

    public void seek(final long position) throws IOException {
        if (position < this.contentLength)
            this.position = position;
        else
            throw new IOException("requesting seek past end of stream: " + position + " (max: " + this.contentLength + ") " + url.toString());
    }

    public int read(byte[] buffer, int offset, int len) throws IOException {
        return read(buffer, offset, len, "plain", ""); // Default unencrypted Stream
    }

    public int read(byte[] buffer, int offset, int len,
                    String destinationFormat, String destinationKey) throws IOException {

        if (offset < 0 || len < 0 || (offset + len) > buffer.length) {
            throw new IndexOutOfBoundsException("Offset=" + offset + ",len=" + len + ",buflen=" + buffer.length);
        }
        if (len == 0 || position == contentLength) {
            if (position >= contentLength) {
                return -1;
            }
            return 0;
        }
        if (position + len > contentLength) {
            len = (int) (contentLength - position);
        }

        // Get the size of the file; offset is for buffer, not for stream
        long fsize = this.contentLength;
        int bytesToRead = (int) Math.min(fsize - position, len);

        int cachePage = (int) (position / PAGE_SIZE); // 0,1,2,...

        try {
            byte[] page = this.get(cachePage);

            int page_offset = (int) (position - cachePage * PAGE_SIZE); // delta page start to 'Read from'
            int bytesToCopy = Math.min(bytesToRead, page.length - page_offset); // don't read past end of page
            int offset_ = offset;
            System.arraycopy(page, page_offset, buffer, offset_, bytesToCopy);
            offset_ += bytesToCopy;
            this.position += bytesToCopy;

            int bytesRemaining = bytesToRead - bytesToCopy;
            while (bytesRemaining > 0) {
                page = this.get(++cachePage); // this.cache.get(cachePage+1);
                bytesToCopy = Math.min(bytesRemaining, page.length); // don't read past end of page
                System.arraycopy(page, 0, buffer, offset_, bytesToCopy);
                bytesRemaining -= bytesToCopy;
                offset_ += bytesToCopy;
                this.position += bytesToCopy;
            }
        } catch (ExecutionException e) {
            logger.error("Error message: {}", e);
            return 0;
        }

        return bytesToRead;
    }


    public void close() throws IOException {
        // Nothing to do
    }


    public int read() throws IOException {
        byte[] tmp = new byte[1];
        read(tmp, 0, 1);
        return (int) tmp[0] & 0xFF;
    }

    @Override
    public String getSource() {
        return hack ? null : url.toString() + hack_extension;
    }

    // A hack to return null as source, which will then default to BAM format in HTSJDK
    public EgaSeekableCachedResStream setSourceNull(boolean hack) {
        this.hack = hack;
        return this;
    }

    // A hack to fool HTSJDK to recognize the file format based on the extension
    public EgaSeekableCachedResStream setExtension(String hack_extension) {
        this.hack_extension = hack_extension;
        return this;
    }

    // ------------------------------------------------------------------------- Cache Population
    // ------------------------------------------------------------------------- Guava Cache

    // separate from stream reading/position
    private byte[] get(int page_number) throws ExecutionException {
        int maxPage = (int) (this.contentLength / PAGE_SIZE + 1); // Don'd read past end of stream

        int firstPage = page_number > 0 ? page_number - 1 : 0; // Get prior cache page, just in case
        int lastPage = (page_number + NUM_PAGES - 1) > maxPage ? maxPage : (page_number + NUM_PAGES - 1);
        ExecutorService pool = Executors.newCachedThreadPool();

        for (int i = firstPage; i < lastPage; i++) {
            final int pageIndex = i;

            pool.submit(() -> {
                try {
                    this.cache.get(pageIndex);
                } catch (ExecutionException e) {
                }
            });
        }

        return this.cache.get(page_number);
    }

    private byte[] populateCache(int page_number) {
        // Last Page Handling
        int maxPage = (int) (this.contentLength / PAGE_SIZE + 1) - 1; // Don'd read past end of stream
        if (page_number > maxPage)
            return new byte[]{};

        long offset = (long) page_number * (long) PAGE_SIZE;
        final int bytesToRead = (int) ((page_number == maxPage) ? (this.contentLength - offset) : PAGE_SIZE);

        // Prepare buffer to read from file
        byte[] bytesRead = new byte[bytesToRead];
        int totalBytesRead = 0;

        synchronized (this) {
            try {
                String byteRange = "bytes=" + offset + "-" + (offset + bytesToRead);
                String url = this.url.toString() + "?startCoordinate=" + offset +
                        "&endCoordinate=" + (offset + bytesToRead) +
                        "&destinationFormat=" + "Plain";

                Request datasetRequest = new Request.Builder()
                        .url(url)
                        .build();

                // Execute the request and retrieve the response.
                okhttp3.Response response = client.newCall(datasetRequest).execute();
                try (ResponseBody body = response.body()){
                    InputStream byteStream = body.byteStream();
                    byte[] buff = new byte[8000];
                    ByteArrayOutputStream bao = new ByteArrayOutputStream();

                    int bytesRead_ = 0;
                    while ((bytesRead_ = byteStream.read(buff)) != -1) {
                        totalBytesRead += bytesRead_;
                        bao.write(buff, 0, bytesRead_);
                    }

                    byte[] result = bao.toByteArray();
                    bytesRead = Arrays.copyOf(result, bytesToRead);
                }

            } catch (Throwable t) {
                logger.error("ERROR " + t.toString() + " Page: " + page_number);
            }
        }
        return bytesRead;
    }
}
