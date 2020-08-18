package eu.elixir.ega.ebi.htsget.formats;

import eu.elixir.ega.ebi.commons.config.UnsupportedFormatException;

public class DataProviderFactory {
    public DataProvider getProviderForFormat(String format) {
        if (format.equalsIgnoreCase("bam"))
            return new BAMDataProvider();

        if (format.equalsIgnoreCase("cram"))
            return new CRAMDataProvider();

        if (format.equalsIgnoreCase("vcf"))
            return new VCFDataProvider();

        if (format.equalsIgnoreCase("bcf"))
            throw new UnsupportedFormatException("Slicing BCF is not currently supported");

        throw new UnsupportedFormatException(format);
    }
}
