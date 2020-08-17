package eu.elixir.ega.ebi.htsget.service.internal;

import eu.elixir.ega.ebi.commons.config.UnsupportedFormatException;

public class DataProviderFactory {
    public DataProvider getProviderForFormat(String format)
    {
        if (format.equalsIgnoreCase("bam"))
            return new BAMDataProvider();

        if (format.equalsIgnoreCase("cram"))
            return new CRAMDataProvider();

        throw new UnsupportedFormatException(format);
    }
}
