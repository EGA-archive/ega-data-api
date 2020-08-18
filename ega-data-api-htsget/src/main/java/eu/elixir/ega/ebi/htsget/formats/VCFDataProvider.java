package eu.elixir.ega.ebi.htsget.formats;

import eu.elixir.ega.ebi.htsget.dto.HtsgetResponseV2;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.util.BlockCompressedInputStream;
import htsjdk.samtools.util.BlockCompressedOutputStream;
import htsjdk.samtools.util.BlockCompressedStreamConstants;
import htsjdk.tribble.FeatureCodecHeader;
import htsjdk.tribble.index.Block;
import htsjdk.tribble.index.tabix.TabixIndex;
import htsjdk.variant.variantcontext.writer.Options;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.VCFCodec;
import htsjdk.variant.vcf.VCFHeader;
import org.apache.commons.io.input.CloseShieldInputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class VCFDataProvider extends AbstractDataProvider implements DataProvider {
    VCFHeader header;

    @Override
    public boolean supportsFileType(String filename) {
        return filename.toLowerCase().endsWith(".vcf.gz");
    }

    @Override
    public void readHeader(SeekableStream stream) throws IOException {
        VCFCodec codec = new VCFCodec();
        try (CloseShieldInputStream shield = new CloseShieldInputStream(stream);
             BlockCompressedInputStream blockStream = new BlockCompressedInputStream(shield)) {
            FeatureCodecHeader featureCodecHeader = codec.readHeader(codec.makeSourceFromStream(blockStream));
            header = (VCFHeader) featureCodecHeader.getHeaderValue();
        }
    }

    @Override
    public URI getHeaderAsDataUri() throws IOException, URISyntaxException {
        try (ByteArrayOutputStream headerOutputStream = new ByteArrayOutputStream()) {
            try (BlockCompressedOutputStream compressedOutputStream = new BlockCompressedOutputStream(headerOutputStream, (File) null);
                 VariantContextWriter writer = new VariantContextWriterBuilder()
                         .unsetOption(Options.INDEX_ON_THE_FLY)
                         .setOutputVCFStream(compressedOutputStream)
                         .setReferenceDictionary(header.getSequenceDictionary())
                         .build()) {
                writer.writeHeader(header);
            }
            return makeDataUriFromBytes(headerOutputStream.toByteArray());
        }
    }

    @Override
    public void addContentUris(String referenceName, Long start, Long end, URI baseURI, HtsgetResponseV2 urls, SeekableStream dataStream, SeekableStream indexStream) throws IOException, URISyntaxException {

        try (BlockCompressedInputStream compressedIndexStream = new BlockCompressedInputStream(indexStream)) {
            TabixIndex index = new TabixIndex(compressedIndexStream);
            List<Block> blocks = index.getBlocks(referenceName, start.intValue(), end.intValue());

            for (Block block : blocks) {
                makeUrlsForBGZFBlocks(baseURI, urls, dataStream, block.getStartPosition(), block.getEndPosition());
            }
        }
    }

    @Override
    public URI getFooterAsDataUri() throws URISyntaxException {
        return makeDataUriFromBytes(BlockCompressedStreamConstants.EMPTY_GZIP_BLOCK);
    }

}
