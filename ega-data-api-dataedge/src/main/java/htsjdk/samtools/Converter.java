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
package htsjdk.samtools;

import htsjdk.samtools.cram.ref.CRAMReferenceSource;
import htsjdk.samtools.cram.ref.ReferenceSource;
import htsjdk.samtools.util.CloseableIterator;

import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author vadim
 */
public class Converter {
    CRAMReferenceSource referenceSource = new ReferenceSource((Path) null);
    ExecutorService es = Executors.newFixedThreadPool(10);

    /**
	 * Create a new {@code Converter} with the given reference source.
     *
     * @param referenceSource A {@code CRAMReferenceSource} to use as data
     *     souce for this.
     */
    public Converter(CRAMReferenceSource referenceSource) {
        this.referenceSource = referenceSource;
    }

    /**
     * Closes the thread pool executor service.
     */
    public void close() {
        es.shutdown();
    }

    /**
     * Creates a new {@code BAMFileWriter} using the given output stream, writes
     * the file header to it and returns the writer.
     *
     * @param os An {@code OutputStream} to write data to.
     * @param fileHeader A {@code SAMFileHeader} to write to the output stream
     * @return A {@code BAMFileWriter} with the given file header.
     */
    public static SAMFileWriter createBAMwriter(OutputStream os, SAMFileHeader fileHeader) {
        final BAMFileWriter writer = new BAMFileWriter(os, null);
        writer.setHeader(fileHeader);
        return writer;
    }

    /**
     * Returns a stream writing the given header and records in BAM format.
     *
     * @param header Header of the output stream.
     * @param records Records for the output stream
     * @return A BAM file stream
     * @throws IOException
     */
    public InputStream asBAM(SAMFileHeader header, CloseableIterator<SAMRecord> records) throws IOException {
        PipedInputStream is = new PipedInputStream();
        PipedOutputStream pos = new PipedOutputStream(is);

        es.submit((Runnable) () -> {
            try {
                final BAMFileWriter writer = new BAMFileWriter(pos, null);
                writer.setHeader(header);
                System.out.println("Pumping records as BAM");
                long count = 0;
                while (records.hasNext()) {
                    writer.addAlignment(records.next());
                    count++;
                }
                System.out.println("Pumping records is done: " + count + " records.");
                writer.close();
            } catch (Exception e) {
                try {
                    records.close();
                } catch (Exception e1) {
                }
                try {
                    pos.close();
                } catch (Exception e1) {

                }
                try {
                    is.close();
                } catch (Exception e1) {

                }
            }
        });

        return is;
    }

    /**
     * Returns a stream writing the given header and records in SAM format.
     *
     * @param header Header of the output stream.
     * @param records Records for the output stream
     * @return A SAM file stream
     * @throws IOException
     */
    public InputStream asSAM(SAMFileHeader header, CloseableIterator<SAMRecord> records) throws IOException {
        PipedInputStream is = new PipedInputStream();
        PipedOutputStream pos = new PipedOutputStream(is);

        es.submit((Runnable) () -> {
            try {
                final SAMTextWriter writer = new SAMTextWriter(pos);
                writer.setHeader(header);
                System.out.println("Pumping records as BAM");
                while (records.hasNext()) {
                    writer.addAlignment(records.next());
                }
                System.out.println("Pumping records is done");
                writer.close();
            } catch (Exception e) {
                try {
                    records.close();
                } catch (Exception e1) {
                }
                try {
                    pos.close();
                } catch (Exception e1) {

                }
                try {
                    is.close();
                } catch (Exception e1) {

                }
            }
        });

        return is;
    }

    /**
     * Returns a stream writing the given header and records in CRAM format.
     *
     * @param header Header of the output stream.
     * @param records Records for the output stream
     * @return A CRAM file stream
     * @throws IOException
     */
    public InputStream asCRAM(SAMFileHeader header, CloseableIterator<SAMRecord> records) throws IOException {
        PipedInputStream is = new PipedInputStream();
        PipedOutputStream pos = new PipedOutputStream(is);

        es.submit((Runnable) () -> {
            try {
                final CRAMFileWriter writer = new CRAMFileWriter(pos, referenceSource, header, "");
                System.out.println("Pumping records as CRAM");
                while (records.hasNext()) {
                    writer.addAlignment(records.next());
                }
                System.out.println("Pumping records is done");
                writer.close();
                records.close();
            } catch (Exception e) {
                try {
                    records.close();
                } catch (Exception e1) {
                }
                try {
                    pos.close();
                } catch (Exception e1) {

                }
                try {
                    is.close();
                } catch (Exception e1) {

                }
            }
        });

        return is;
    }
}
