package net.microtrash.wisperingtree.fragment;

import android.bluetooth.BluetoothDevice;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.ramimartin.multibluetooth.fragment.BluetoothFragment;

/**
 * Created by steph on 4/23/15.
 */
public class SyncFragment extends BluetoothFragment {
    private static final String TAG = "SyncFragment";

    @Override
    public int myNbrClientMax() {
        return 5;
    }

    @Override
    public void onBluetoothDeviceFound(BluetoothDevice device) {
        Log.v(TAG, "found!");
    }

    @Override
    public void onClientConnectionSuccess() {
        Log.v(TAG, "client connected!");
    }

    @Override
    public void onClientConnectionFail() {
        Log.v(TAG, "Feil!");
    }

    @Override
    public void onServeurConnectionSuccess() {
        Log.v(TAG, "onServeurConnectionSuccess!");
    }

    @Override
    public void onServeurConnectionFail() {
        Log.v(TAG, "onServeurConnectionFail!");
    }

    @Override
    public void onBluetoothStartDiscovery() {
        Log.v(TAG, "onBluetoothStartDiscovery");
    }

    @Override
    public void onBluetoothCommunicator(String messageReceive) {
        Log.v(TAG, "receive string: "+messageReceive);
    }

    @Override
    public void onBluetoothNotAviable() {

    }

    public static Fragment newInstance() {
        return new SyncFragment();
    }
}
