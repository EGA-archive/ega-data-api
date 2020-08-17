package eu.elixir.ega.ebi.htsget.service.internal;

import eu.elixir.ega.ebi.htsget.rest.HtsgetResponse;
import eu.elixir.ega.ebi.htsget.rest.HtsgetUrl;
import htsjdk.samtools.CRAMCRAIIndexer;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.cram.CRAIEntry;
import htsjdk.samtools.cram.CRAIIndex;
import htsjdk.samtools.cram.build.CramIO;
import htsjdk.samtools.cram.structure.Container;
import htsjdk.samtools.cram.structure.ContainerHeader;
import htsjdk.samtools.cram.structure.CramHeader;
import htsjdk.samtools.seekablestream.SeekableStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;

public class CRAMDataProvider implements DataProvider {

    CramHeader cramHeader;
    SAMFileHeader samHeader;

    @Override
    public boolean supportsFileType(String filename) {
        return filename.toLowerCase().endsWith(".cram") || filename.toLowerCase().endsWith(".cram.cip");
    }

    @Override
    public void readHeader(SeekableStream stream) throws IOException {
        cramHeader = CramIO.readCramHeader(stream);
        samHeader = Container.readSAMFileHeaderContainer(cramHeader.getCRAMVersion(), stream, new String(cramHeader.getId()));
    }

    @Override
    public SAMFileHeader getHeader() {
        return samHeader;
    }

    @Override
    public URI getHeaderAsDataUri() throws IOException, URISyntaxException {
        try(ByteArrayOutputStream headerOutputStream = new ByteArrayOutputStream()) {

            CramIO.writeCramHeader(cramHeader, headerOutputStream);
            Container.writeSAMFileHeaderContainer(cramHeader.getCRAMVersion(), samHeader, headerOutputStream);

            return new URI(String.format("data:base64,%s", Base64.getEncoder().encodeToString(headerOutputStream.toByteArray())));
        }
    }

    @Override
    public void addContentUris(int sequenceIndex, Long start, Long end, URI baseURI, HtsgetResponse urls, SeekableStream dataStream, SeekableStream indexStream) throws IOException, URISyntaxException {
        CRAIIndex index = CRAMCRAIIndexer.readIndex(indexStream);
        List<CRAIEntry> indexEntries = CRAIIndex.find(index.getCRAIEntries(), sequenceIndex, start.intValue(), ((Long)(end - start)).intValue());
        for(CRAIEntry entry : indexEntries) {
            long byteStart = entry.getContainerStartByteOffset();

            // the index does not tell the container sizes
            // they have to come from the data itself
            dataStream.seek(byteStart);
            ContainerHeader containerHeader = new ContainerHeader(cramHeader.getCRAMVersion(), dataStream);
            long byteEnd = dataStream.position() + containerHeader.getContainerBlocksByteSize();

            HtsgetUrl chunkURL = new HtsgetUrl(baseURI, "body");
            chunkURL.setHeader("Range", String.format("bytes=%d-%d", byteStart, byteEnd));
            urls.addUrl(chunkURL);
        }
    }

    @Override
    public URI getFooterAsDataUri() throws IOException, URISyntaxException {
        try(ByteArrayOutputStream footerOutputStream = new ByteArrayOutputStream()) {

            CramIO.writeCramEOF(cramHeader.getCRAMVersion(), footerOutputStream);

            return new URI(String.format("data:base64,%s", Base64.getEncoder().encodeToString(footerOutputStream.toByteArray())));
        }
    }
}
