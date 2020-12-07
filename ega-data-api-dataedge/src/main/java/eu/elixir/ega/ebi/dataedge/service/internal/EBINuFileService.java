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
package eu.elixir.ega.ebi.dataedge.service.internal;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import eu.elixir.ega.ebi.commons.exception.NotFoundException;
import eu.elixir.ega.ebi.commons.shared.dto.File;
import eu.elixir.ega.ebi.dataedge.exception.EgaFileNotFoundException;
import eu.elixir.ega.ebi.dataedge.exception.FileNotAvailableException;
import eu.elixir.ega.ebi.dataedge.exception.RangesNotSatisfiableException;
import eu.elixir.ega.ebi.dataedge.exception.UnretrievableFileException;
import eu.elixir.ega.ebi.dataedge.service.FileMetaService;
import eu.elixir.ega.ebi.dataedge.service.KeyService;
import eu.elixir.ega.ebi.dataedge.service.NuFileService;
import eu.elixir.ega.ebi.dataedge.utils.DecryptionUtils;
import htsjdk.samtools.seekablestream.cipher.ebi.Glue;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.springframework.security.core.Authentication;
import uk.ac.ebi.ega.fire.exceptions.ClientProtocolException;
import uk.ac.ebi.ega.fire.exceptions.FireServiceException;
import uk.ac.ebi.ega.fire.service.IFireService;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

public class EBINuFileService implements NuFileService {

    private final KeyService keyService;
    private final FileMetaService fileMetaService;
    private final IFireService fireClientService;
    private final LoadingCache<String, byte[]> aesHeaderCache;

    public EBINuFileService(KeyService keyService, FileMetaService fileMetaService, IFireService fireClientService) {
        this.keyService = keyService;
        this.fileMetaService = fileMetaService;
        this.fireClientService = fireClientService;
        this.aesHeaderCache = Caffeine.newBuilder()
                .expireAfterAccess(1, TimeUnit.DAYS)
                .build(this::getAESHeader);
    }

    @Override
    public long getPlainFileSize(String fileId, Authentication auth, String sessionId) throws EgaFileNotFoundException, UnretrievableFileException, FileNotAvailableException {

        return getPlainFileSize(getFile(fileId, auth, sessionId), keyService.getEncryptionAlgorithm(fileId));
    }

    private File getFile(String fileId, Authentication auth, String sessionId) throws EgaFileNotFoundException, FileNotAvailableException {
        File file;
        try {
            file = fileMetaService.getFile(auth, fileId, sessionId);
        } catch(NotFoundException exception) {
            throw new EgaFileNotFoundException(fileId);
        }
        if (file.getFileId() == null) {
            throw new FileNotAvailableException(fileId);
        }
        return file;
    }

    private long getPlainFileSize(File file, String encryptionFormat) throws UnretrievableFileException {
        long size = file.getFileSize();

        if ("aes128".equals(encryptionFormat) || "aes256".equals(encryptionFormat)) {
            size -= 16L;
        } else if (!"plain".equals(encryptionFormat)) {
            throw new UnretrievableFileException(file.getFileId());
        }

        return size;
    }

    @Override
    public InputStream getSpecificByteRange(String fileId, long start, long end, Authentication auth, String sessionId) throws EgaFileNotFoundException, UnretrievableFileException, FileNotAvailableException, RangesNotSatisfiableException {
        File file = getFile(fileId, auth, sessionId);
        String encryptionAlgorithm = keyService.getEncryptionAlgorithm(fileId);

        long plainFileSize = getPlainFileSize(file, encryptionAlgorithm);
        if (start < 0 || end >= plainFileSize || end < start)
            throw new RangesNotSatisfiableException(fileId, start, end);

        try {
            switch (encryptionAlgorithm) {
                case "plain": {
                    return fireClientService.downloadByteRangeByPath(file.getDisplayFilePath(), start, end);
                }
                case "aes128":
                case "aes256": {
                    return decryptAES(file, encryptionAlgorithm, start, end);
                }
                default:
                    throw new UnretrievableFileException(file.getFileId());
            }
        } catch (FileNotFoundException e) {
            throw new EgaFileNotFoundException(fileId, e);
        } catch (IOException | FireServiceException | URISyntaxException | ClientProtocolException e) {
            throw new FileNotAvailableException(fileId, e);
        }
    }

    private InputStream decryptAES(File file, String algorithm, final long start, final long end) throws IOException, FileNotAvailableException, URISyntaxException, FireServiceException, ClientProtocolException {

        // AES works in blocks of 16 bytes so we have to request always whole blocks
        final int startPadding = (int) (start % 16);
        final int endPadding = (int) (16 - (end % 16) - 1);
        final long fireStartByte = start - startPadding;
        final long fireEndByte = end + endPadding;

        // Add 16 because the first 16 bytes are the IV header
        InputStream encryptedStream = fireClientService.downloadByteRangeByPath(file.getDisplayFilePath(),
                fireStartByte + 16,
                fireEndByte + 16);

        SecretKey key = Glue.getInstance().getKey(keyService.getFileKey(file.getFileId()).toCharArray(),
                "aes128".equals(algorithm) ? 128 : 256);

        byte[] header = aesHeaderCache.get(file.getDisplayFilePath());
        assert header != null;

        // update the IV for where we are in the file
        if (fireStartByte > 0) {
            header = header.clone();
            DecryptionUtils.byteIncrementFast(header, fireStartByte);
        }

        Cipher cipher;
        try {
            cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(header));
        } catch (NoSuchAlgorithmException
                | NoSuchPaddingException
                | InvalidKeyException
                | InvalidAlgorithmParameterException e) {
            throw new FileNotAvailableException(file.getFileId(), e);
        }

        CipherInputStream decryptedStream = new CipherInputStream(encryptedStream, cipher);

        try {
            cipherSkip(decryptedStream, startPadding);
        } catch (IOException e) {
            throw new FileNotAvailableException(file.getFileId(), e);
        }

        return new BoundedInputStream(decryptedStream, end - start + 1);
    }

    private void cipherSkip(CipherInputStream stream, int n) throws IOException {
        for (int i = 0; i < n; i++) {
            // CipherInputStream.skip does not work properly so use read
            //noinspection ResultOfMethodCallIgnored
            stream.read();
        }
    }

    private byte[] getAESHeader(String firePath) throws IOException, FireServiceException, ClientProtocolException, URISyntaxException {
        try (InputStream stream = fireClientService.downloadByteRangeByPath(firePath, 0, 15)) {
            return IOUtils.toByteArray(stream);
        }
    }

    protected void invalidateAESHeaderCache() {
        aesHeaderCache.invalidateAll();
    }

}
