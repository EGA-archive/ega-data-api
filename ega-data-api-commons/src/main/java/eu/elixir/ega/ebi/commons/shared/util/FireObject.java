package eu.elixir.ega.ebi.commons.shared.util;

import java.util.Objects;

public class FireObject {
    private String fileURL;
    private long fileSize;

    public FireObject(String fileURL, long fileSize) {
        this.fileURL = fileURL;
        this.fileSize = fileSize;
    }

    public String getFileURL() {
        return fileURL;
    }

    public long getFileSize() {
        return fileSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FireObject that = (FireObject) o;
        return fileSize == that.fileSize &&
                Objects.equals(fileURL, that.fileURL);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileURL, fileSize);
    }
}
