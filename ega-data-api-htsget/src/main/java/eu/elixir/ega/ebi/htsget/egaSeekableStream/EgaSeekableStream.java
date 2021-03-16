package eu.elixir.ega.ebi.htsget.egaSeekableStream;

import java.io.IOException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.elixir.ega.ebi.commons.cache2k.My2KCachePageFactory;
import htsjdk.samtools.seekablestream.SeekableStream;

public class EgaSeekableStream extends SeekableStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(EgaSeekableStream.class);
    private String fileId;
    private long position = 0;
    private long marked = 0;
    private int readLimit = 0;
    private long contentLength = 0;
    private My2KCachePageFactory fileDownloader;

    public EgaSeekableStream(String fileId, My2KCachePageFactory fileDownloader, long fileSize) {
        this.fileId = fileId;
        this.fileDownloader = fileDownloader;
        this.contentLength = fileSize - 16;
        
        LOGGER.info("######## fileId : " + fileId + " , contentLength : " + contentLength);
    }

    @Override
    public long length() {
        LOGGER.info("######## fileId length : " + fileId + " , length : " + contentLength);
        return contentLength;
    }

    @Override
    public long position() throws IOException {
        LOGGER.info("######## fileId position : " + fileId + " , position : " + position);
        return position;
    }

    @Override
    public void seek(long position) throws IOException {
        LOGGER.info("######## fileId seek : " + fileId + " , position : " + position);
        if (position < contentLength) {
            this.position = position;
        } else {
            throw new IOException(
                    "requesting seek past end of stream: " + position + " (max: " + this.contentLength + ") ");
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        return read(buffer, offset, length, "plain", "");
    }

    @Override
    public void close() throws IOException {
        LOGGER.info("######## fileId close : " + fileId);

    }

    @Override
    public boolean eof() throws IOException {
        LOGGER.info("######## fileId eof contentLength : " + contentLength + " , position : " + position);
        return contentLength > 0L && position >= contentLength;
    }

    @Override
    public String getSource() {
        return "";
    }

    @Override
    public int read() throws IOException {
        LOGGER.info("######## read ");
        byte[] data = new byte[1];
        this.read(data, 0, 1);
        return data[0] & 0xFF;
    }

    @Override
    public long skip(long n) throws IOException {      
        long bytesToSkip = Math.min(n, contentLength - position);
        LOGGER.info("######## skip  fileId : " + fileId + " , position : "+position + " , bytesToSkip : "+ bytesToSkip + " , n : "+ n);
        position += bytesToSkip;
        return bytesToSkip;
    }

    public int read(byte[] buffer, int offset, int len, String destinationFormat, String destinationKey)
            throws IOException {
        LOGGER.info("######## fileId : " + fileId + " , offset : " + offset + " , len : " + len + " , buffersize : " + buffer.length+ " , position : " + position);
        if (offset < 0 || len < 0 || (offset + len) > buffer.length) {
            throw new IndexOutOfBoundsException("Offset=" + offset + ", len=" + len + ", buflen=" + buffer.length);
        }

        if (len == 0 || position == contentLength) {
            if (position >= contentLength) {
                LOGGER.info("######## -1 fileId : "+ fileId + " , position : "+ position+ " , contentLength : "+ contentLength);
                return -1;
            }
            return 0;
        }

        if (position + len > contentLength) {
            len = (int) (contentLength - position);
        }

        long startCoordinate = position;
        
        int bytesToRead = (int) Math.min(contentLength - position, len);
        long endRange = position + bytesToRead; 

        LOGGER.info("######## fileId : " + fileId + " , startCoordinate : " + startCoordinate + " , endRange : " + endRange + " , bytesToRead : " + bytesToRead);
        byte[] chunk = fileDownloader.downloadPage(fileId, startCoordinate, endRange);
        if (chunk != null) {
            System.arraycopy(chunk, 0, buffer, offset, bytesToRead);
            position = position + bytesToRead;
            if (bytesToRead < 200)
                LOGGER.info("######## fileid : " + fileId + " , chunk : "+Arrays.toString(chunk));
            
            LOGGER.info("######## returnfileId : " + fileId + " , startCoordinate : " + startCoordinate + " , endRange : " + endRange + " , return len : "+bytesToRead);
            return bytesToRead;
        }
        
        LOGGER.info("######## -1 fileId : " + fileId + " , startCoordinate : " + startCoordinate + " , endRange : " + endRange + " , return len : -1");
        return -1;
    }

    @Override
    public void reset() {
        LOGGER.info("######## fileId reset : " + fileId + " , position : " + position + " , marked : " + marked + " , readLimit : " + readLimit );

        if (position - marked > readLimit) {
            this.position = 0;
        } else {
            this.position = marked;
        }
    }
    
}
