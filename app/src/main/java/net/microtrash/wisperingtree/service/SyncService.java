package net.microtrash.wisperingtree.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;

import com.ramimartin.multibluetooth.bluetooth.mananger.BluetoothManager;
import com.ramimartin.multibluetooth.bus.ClientConnectionFail;
import com.ramimartin.multibluetooth.bus.ClientConnectionSuccess;
import com.ramimartin.multibluetooth.bus.ServeurConnectionFail;
import com.ramimartin.multibluetooth.bus.ServeurConnectionSuccess;

import net.microtrash.wisperingtree.bus.FileSentToClient;
import net.microtrash.wisperingtree.bus.LogMessage;
import net.microtrash.wisperingtree.util.Logger;
import net.microtrash.wisperingtree.util.LoggerInterface;
import net.microtrash.wisperingtree.util.Static;
import net.microtrash.wisperingtree.util.Utils;

import java.io.File;
import java.util.Hashtable;

import de.greenrobot.event.EventBus;

public class SyncService extends Service {
    private BluetoothManager mBluetoothManager;
    private LoggerInterface mLogger;

    private Hashtable<String, File> mFilesSent = new Hashtable<>();

    //private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        mLogger = Logger.getInstance();
        mLogger.setOnLogListener(new LoggerInterface() {
            @Override
            public void log(String message) {
                EventBus.getDefault().post(new LogMessage(message));
            }

            @Override
            public void log(String key, String value) {
                EventBus.getDefault().post(new LogMessage(key + ": " + value));
            }

            @Override
            public void setOnLogListener(LoggerInterface logListener) {

            }
        });


        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);

        mBluetoothManager = new BluetoothManager(getApplicationContext(), mLogger);
        mBluetoothManager.setNbrClientMax(7);

        if (!mBluetoothManager.checkBluetoothAviability()) {
            log("===> Bluetooth not aviable");
            mBluetoothManager.enableBluetooth();
        } else {

            if (BluetoothAdapter.getDefaultAdapter().getAddress().equals(Static.SERVER_MAC)) {
                serverType();
            } else {
                clientType();
            }
        }

    }


    public void serverType() {
        log("===> Start Server ! Your mac address : " + mBluetoothManager.getYourBtMacAddress());
        //setTimeDiscoverable(BluetoothManager.BLUETOOTH_TIME_DICOVERY_3600_SEC);
        selectServerMode();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                createServerForClient(Static.CLIENT_MAC1);
            }
        }, 2000);
    }

    private void log(String s) {
        mLogger.log(s);
    }


    public void clientType() {
        log("===> Start Client ! Your mac address : " + mBluetoothManager.getYourBtMacAddress());
        //setTimeDiscoverable(BluetoothManager.BLUETOOTH_TIME_DICOVERY_120_SEC);
        selectClientMode();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                clientConnect();
            }
        }, 2000);
    }


    private void clientConnect() {
        //showDiscoveredDevicesDialog();
        mBluetoothManager.setOnFileReceivedListener(new BluetoothManager.OnFileReceivedListener() {
            @Override
            public void onFileReceived(File file) {
                log("===> on file received: " + file.getName());
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            }
        });

        onDeviceSelectedForConnection(Static.SERVER_MAC);
    }

    public void onDeviceSelectedForConnection(String addressMac) {
        log("===> Connect to " + addressMac);
        createClient(addressMac);
    }

    void createServerForClient(String mac) {
        log("===> Creating server for client address " + mac + "...");
        mBluetoothManager.createServer(mac);
    }

    public void onClientConnectionSuccess() {
        log("===> Client Connexion success !");
    }

    public void onClientConnectionFail() {
        log("===> Client Connexion fail !");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                clientConnect();
            }
        }, 1000);
    }

    public void onServeurConnectionSuccess() {
        log("===> Serveur Connexion success !");
        startFileTransfer();
    }

    private void startFileTransfer() {
        File rootDir = new File(Utils.getAppRootDir());
        for (File file : rootDir.listFiles()) {
            String key = file.getName() + file.length();
            File isTransferred = mFilesSent.get(key);
            if(isTransferred == null) {
                mBluetoothManager.sendFile(file);
                break;
            }
        }
    }

    public void onServerConnectionFail(String clientAdressConnectionFail) {
        log("===> Client connection lost! Mac: " + clientAdressConnectionFail);
        createServerForClient(Static.CLIENT_MAC1);
    }

    public void onEvent(FileSentToClient event){
        File file = event.getFile();
        String key = file.getName() + file.length();
        mFilesSent.put(key, file);
        startFileTransfer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        mBluetoothManager.closeAllConnexion();
    }

    public void setTimeDiscoverable(int timeInSec) {
        mBluetoothManager.setTimeDiscoverable(timeInSec);
    }

    public void startDiscovery() {
        mBluetoothManager.startDiscovery();
    }

    public boolean isConnected() {
        return mBluetoothManager.isConnected;
    }

    public void scanAllBluetoothDevice() {
        mBluetoothManager.scanAllBluetoothDevice();
    }

    public void disconnectClient() {
        mBluetoothManager.disconnectClient();
    }

    public void disconnectServer() {
        mBluetoothManager.disconnectServer();
    }

    public void createServer(String address) {
        mBluetoothManager.createServer(address);
    }

    public void selectServerMode() {
        mBluetoothManager.selectServerMode();
    }

    public void selectClientMode() {
        mBluetoothManager.selectClientMode();
    }

    public BluetoothManager.TypeBluetooth getTypeBluetooth() {
        return mBluetoothManager.mType;
    }

    public BluetoothManager.TypeBluetooth getBluetoothMode() {
        return mBluetoothManager.mType;
    }

    public void createClient(String addressMac) {
        mBluetoothManager.createClient(addressMac);
    }

    public void sendMessage(String message) {
        mBluetoothManager.sendMessage(message);
    }


    public void onEventMainThread(ClientConnectionSuccess event) {
        mBluetoothManager.isConnected = true;
        onClientConnectionSuccess();
    }

    public void onEventMainThread(ClientConnectionFail event) {
        mBluetoothManager.isConnected = false;
        onClientConnectionFail();
    }

    public void onEventMainThread(ServeurConnectionSuccess event) {
        mBluetoothManager.isConnected = true;
        mBluetoothManager.onServerConnectionSuccess(event.mClientAdressConnected);
        onServeurConnectionSuccess();
    }

    public void onEventMainThread(ServeurConnectionFail event) {
        mBluetoothManager.onServerConnectionFailed(event.mClientAdressConnectionFail);
        onServerConnectionFail(event.mClientAdressConnectionFail);
    }

}
