package net.microtrash.wisperingtree.bus;

/**
 * Created by Stephan Petzl (stephan.petzl@gmail.com) on 11/24/15.
 */
public class FileSendingToClient {
    private final String mReceiverMac;

    public FileSendingToClient(String receiverMac) {
        mReceiverMac = receiverMac;
    }

    public String getReceiverMac() {
        return mReceiverMac;
    }
}
