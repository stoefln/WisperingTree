package net.microtrash.wisperingtree.bus;

/**
 * Created by Rami MARTIN on 13/04/2014.
 */
public class ServerConnectionFail {

    public String mMac;

    public ServerConnectionFail(String clientAdressConnectionFail){
        mMac = clientAdressConnectionFail;
    }

    public String getMac() {
        return mMac;
    }
}
