package net.microtrash.wisperingtree.bus;


import java.io.File;
import java.io.Serializable;

public class FileSentToClient implements Serializable {
    File mFile;

    public File getFile() {
        return mFile;
    }

    public void setFile(File file) {
        mFile = file;
    }

    public FileSentToClient(File file) {
        mFile = file;
    }
}
