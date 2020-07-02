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
package eu.elixir.ega.ebi.htsjdk.variant.vcf;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.Interval;
import htsjdk.samtools.util.IntervalList;
import htsjdk.tribble.AbstractFeatureReader;
import htsjdk.tribble.FeatureCodec;
import htsjdk.tribble.FeatureReader;
import htsjdk.tribble.TribbleException;
import htsjdk.variant.bcf2.BCF2Codec;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;
import htsjdk.variant.vcf.VCFHeader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * Simplified interface for reading from VCF/BCF files.
 */
public class MyVCFFileReader implements Closeable, Iterable<VariantContext> {

    private static String downloaderUrl = "";

    private final FeatureReader<VariantContext> reader;

    /**
     * Returns true if the given file appears to be a BCF file.
     */
    public static boolean isBCF(final File file) {
        return file.getAbsolutePath().endsWith(".bcf");
    }

    public static boolean isBCF(final String url) {
        String fileName = url;
        if (url.toLowerCase().contains("egaf")) {
            int startCoord = url.indexOf("EGAF");
            String fileId = url.substring(startCoord, startCoord + 15);
            fileName = getFile(fileId);
        }

        return (fileName.toLowerCase().endsWith(".bcf") ||
                fileName.toLowerCase().endsWith(".bcf.gz") ||
                fileName.toLowerCase().endsWith(".bcf.cip") ||
                fileName.toLowerCase().endsWith(".bcf.gz.cip"));
    }

    private static String getFile(String fileId) {

        RestTemplate rt = new RestTemplate();
        ResponseEntity<eu.elixir.ega.ebi.shared.dto.File[]> forEntity = rt.getForEntity(downloaderUrl + "/file/{fileId}", eu.elixir.ega.ebi.shared.dto.File[].class, fileId);
        eu.elixir.ega.ebi.shared.dto.File[] body = forEntity.getBody();

        return body[0].getFileName();

    }

    /**
     * Returns the SAMSequenceDictionary from the provided VCF file.
     */
    public static SAMSequenceDictionary getSequenceDictionary(final File file) {
        try (final MyVCFFileReader vcfFileReader = new MyVCFFileReader(file, false)) {
            return vcfFileReader.getFileHeader().getSequenceDictionary();
        }
    }

    /**
     * Constructs a VCFFileReader that requires the index to be present.
     */
    public MyVCFFileReader(final File file) {
        this(file, true);
    }

    /**
     * Constructs a VCFFileReader with a specified index.
     */
    public MyVCFFileReader(final File file, final File indexFile) {
        this(file, indexFile, true);
    }

    /**
     * Allows construction of a VCFFileReader that will or will not assert the presence of an index as desired.
     */
    public MyVCFFileReader(final File file, final boolean requireIndex) {
        // Note how we deal with type safety here, just casting to (FeatureCodec)
        // in the call to getFeatureReader is not enough for Java 8.
        FeatureCodec<VariantContext, ?> codec = isBCF(file) ? new BCF2Codec() : new VCFCodec();
        this.reader = AbstractFeatureReader.getFeatureReader(
                file.getAbsolutePath(),
                codec,
                requireIndex);
    }

    public MyVCFFileReader(final String url, final boolean requireIndex) {
        // Note how we deal with type safety here, just casting to (FeatureCodec)
        // in the call to getFeatureReader is not enough for Java 8.
        FeatureCodec<VariantContext, ?> codec = isBCF(url) ? new BCF2Codec() : new VCFCodec();
        this.reader = AbstractFeatureReader.getFeatureReader(
                url,
                codec,
                requireIndex);
    }

    /**
     * Allows construction of a VCFFileReader with a specified index file.
     */
    public MyVCFFileReader(final File file, final File indexFile, final boolean requireIndex) {
        // Note how we deal with type safety here, just casting to (FeatureCodec)
        // in the call to getFeatureReader is not enough for Java 8.
        FeatureCodec<VariantContext, ?> codec = isBCF(file) ? new BCF2Codec() : new VCFCodec();
        this.reader = AbstractFeatureReader.getFeatureReader(
                file.getAbsolutePath(),
                indexFile.getAbsolutePath(),
                codec,
                requireIndex);
    }

    public MyVCFFileReader(final String url, final String indexUrl, final boolean requireIndex) {
        // Note how we deal with type safety here, just casting to (FeatureCodec)
        // in the call to getFeatureReader is not enough for Java 8.
        FeatureCodec<VariantContext, ?> codec = isBCF(url) ? new BCF2Codec() : new VCFCodec();
        this.reader = AbstractFeatureReader.getFeatureReader(
                url,
                indexUrl,
                codec,
                requireIndex);
    }

    public MyVCFFileReader(final String url, final String indexUrl, final boolean requireIndex, String downloaderUrl) {
        // Note how we deal with type safety here, just casting to (FeatureCodec)
        // in the call to getFeatureReader is not enough for Java 8.
        MyVCFFileReader.downloaderUrl = downloaderUrl;
        FeatureCodec<VariantContext, ?> codec = isBCF(url) ? new BCF2Codec() : new VCFCodec();

        this.reader = AbstractFeatureReader.getFeatureReader(
                url,
                indexUrl,
                codec,
                requireIndex);
    }

    /**
     * Parse a VCF file and convert to an IntervalList The name field of the IntervalList is taken from the ID field of the variant, if it exists. if not,
     * creates a name of the format interval-n where n is a running number that increments only on un-named intervals
     *
     * @param file
     * @return
     */
    public static IntervalList fromVcf(final File file) {
        return fromVcf(file, false);
    }

    public static IntervalList fromVcf(final File file, final boolean includeFiltered) {
        final MyVCFFileReader vcfFileReader = new MyVCFFileReader(file, false);
        final IntervalList intervalList = fromVcf(vcfFileReader, includeFiltered);
        vcfFileReader.close();
        return intervalList;
    }

    /**
     * Converts a vcf to an IntervalList. The name field of the IntervalList is taken from the ID field of the variant, if it exists. If not,
     * creates a name of the format interval-n where n is a running number that increments only on un-named intervals
     * Will use a "END" tag in the info field as the end of the interval (if exists).
     *
     * @param vcf the vcfReader to be used for the conversion
     * @return an IntervalList constructed from input vcf
     */

    public static IntervalList fromVcf(final MyVCFFileReader vcf) {
        return fromVcf(vcf, false);
    }

    public static IntervalList fromVcf(final MyVCFFileReader vcf, final boolean includeFiltered) {

        //grab the dictionary from the VCF and use it in the IntervalList
        final SAMSequenceDictionary dict = vcf.getFileHeader().getSequenceDictionary();
        final SAMFileHeader samFileHeader = new SAMFileHeader();
        samFileHeader.setSequenceDictionary(dict);
        final IntervalList list = new IntervalList(samFileHeader);

        int intervals = 0;
        for (final VariantContext vc : vcf) {
            if (includeFiltered || !vc.isFiltered()) {
                String name = vc.getID();
                final Integer intervalEnd = vc.getCommonInfo().getAttributeAsInt("END", vc.getEnd());
                if (".".equals(name) || name == null)
                    name = "interval-" + (++intervals);
                list.add(new Interval(vc.getContig(), vc.getStart(), intervalEnd, false, name));
            }
        }

        return list;
    }

    /**
     * Returns the VCFHeader associated with this VCF/BCF file.
     */
    public VCFHeader getFileHeader() {
        return (VCFHeader) reader.getHeader();
    }

    /**
     * Returns an iterator over all records in this VCF/BCF file.
     */
    @Override
    public CloseableIterator<VariantContext> iterator() {
        try {
            return reader.iterator();
        } catch (final IOException ioe) {
            throw new TribbleException("Could not create an iterator from a feature reader.", ioe);
        }
    }

    /**
     * Queries for records overlapping the region specified.
     * Note that this method requires VCF files with an associated index.  If no index exists a TribbleException will be thrown.
     *
     * @param chrom the chomosome to query
     * @param start query interval start
     * @param end   query interval end
     * @return non-null iterator over VariantContexts
     */
    public CloseableIterator<VariantContext> query(final String chrom, final int start, final int end) {
        try {
            return reader.query(chrom, start, end);
        } catch (final IOException ioe) {
            throw new TribbleException("Could not create an iterator from a feature reader.", ioe);
        }
    }

    @Override
    public void close() {
        try {
            this.reader.close();
        } catch (final IOException ioe) {
            throw new TribbleException("Could not close a variant context feature reader.", ioe);
        }
    }
}