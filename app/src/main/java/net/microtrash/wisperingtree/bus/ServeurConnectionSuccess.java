package net.microtrash.wisperingtree.bus;

/**
 * Created by Rami MARTIN on 13/04/2014.
 */
public class ServeurConnectionSuccess {

    public String mClientAdressConnected;

    public ServeurConnectionSuccess(String clientAdressConnected){
        mClientAdressConnected = clientAdressConnected;
    }
}
