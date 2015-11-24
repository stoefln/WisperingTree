package net.microtrash.wisperingtree.bus;


import java.io.File;
import java.io.Serializable;

public class FileSentToClient implements Serializable {
    private final String mReceiverMac;
    File mFile;

    public File getFile() {
        return mFile;
    }

    public void setFile(File file) {
        mFile = file;
    }

    public String getReceiverMac() {
        return mReceiverMac;
    }

    public FileSentToClient(File file, String receiverMac) {
        mFile = file;
        mReceiverMac = receiverMac;
    }
}
