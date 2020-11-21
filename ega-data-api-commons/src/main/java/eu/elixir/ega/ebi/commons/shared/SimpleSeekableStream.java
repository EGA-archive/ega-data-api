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
package eu.elixir.ega.ebi.commons.shared;

import htsjdk.samtools.seekablestream.SeekableStream;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.net.URL;

public class SimpleSeekableStream extends SeekableStream {

    private static final int DEFAULT_CHUNK_SIZE = 4096;

    private final URL uri;
    private final int chunkSize;
    private final long length;
    private final OkHttpClient client;

    private long position;
    private byte[] buffer;
    private long bufferPosition;

    public SimpleSeekableStream(URL url, OkHttpClient client) throws IOException {
        this(url, client, DEFAULT_CHUNK_SIZE);
    }

    public SimpleSeekableStream(URL url, OkHttpClient client, int chunkSize) throws IOException {
        this.uri = url;
        this.chunkSize = chunkSize;
        this.client = client;

        String contentLengthValue = client.newCall(new Request.Builder().url(url).head().build())
                .execute()
                .header(HttpHeaders.CONTENT_LENGTH);

        this.length = Long.parseLong(contentLengthValue);
    }

    @Override
    public long length() {
        return length;
    }

    @Override
    public long position() throws IOException {
        return position;
    }

    @Override
    public void seek(long position) throws IOException {
        if (position < 0 || position >= length) {
            throw new IOException("Invalid seek position");
        }
        this.position = position;
    }

    @Override
    public int read() throws IOException {
        if (eof())
            return -1;

        prepareToReadAt(position);

        return buffer[(int) ((position++) - bufferPosition)];
    }

    protected void prepareToReadAt(long position) throws IOException {
        if (buffer == null || position < bufferPosition || position >= bufferPosition + buffer.length) {
            Request request = new Request.Builder()
                    .url(uri)
                    .addHeader(HttpHeaders.RANGE, String.format("bytes=%d-%d", position, position + chunkSize - 1))
                    .build();

            buffer = client.newCall(request)
                    .execute()
                    .body()
                    .bytes();

            bufferPosition = position;
        }
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        if (eof())
            return -1;

        int bytesToRead = (int) Math.min(length, this.length - position);
        int bytesRead = 0;
        while (bytesRead < bytesToRead) {
            prepareToReadAt(position);
            int bytesToCopy = (int) Math.min(buffer.length - (position - bufferPosition), bytesToRead - bytesRead);
            System.arraycopy(buffer, (int) (position - bufferPosition), bytes, offset + bytesRead, bytesToCopy);
            position += bytesToCopy;
            bytesRead += bytesToCopy;
        }

        return bytesRead;
    }

    @Override
    public void close() throws IOException {
        buffer = null;
        bufferPosition = 0;
    }

    @Override
    public boolean eof() throws IOException {
        return position >= length;
    }

    @Override
    public String getSource() {
        return uri.toString();
    }
}
