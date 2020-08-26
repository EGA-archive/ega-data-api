package eu.elixir.ega.ebi.htsget.formats;

import eu.elixir.ega.ebi.commons.config.UnsupportedFormatException;
import htsjdk.samtools.seekablestream.SeekableStream;

import java.io.IOException;

public class DataProviderFactory {
    public DataProvider getProviderForFormat(String format, SeekableStream dataStream) throws IOException {
        if (format.equalsIgnoreCase("bam"))
            return new BAMDataProvider(dataStream);

        if (format.equalsIgnoreCase("cram"))
            return new CRAMDataProvider(dataStream);

        if (format.equalsIgnoreCase("vcf"))
            return new VCFDataProvider(dataStream);

        if (format.equalsIgnoreCase("bcf"))
            throw new UnsupportedFormatException("Slicing BCF is not currently supported");

        throw new UnsupportedFormatException(format);
    }
}
