package net.microtrash.wisperingtree.bus;


import java.io.File;

public class FileSentToClient {
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
