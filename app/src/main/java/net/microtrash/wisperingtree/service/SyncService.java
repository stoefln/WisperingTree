package net.microtrash.wisperingtree.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import net.microtrash.wisperingtree.bluetooth.mananger.BluetoothManager;
import net.microtrash.wisperingtree.bluetooth.server.BluetoothServer;
import net.microtrash.wisperingtree.bus.ClientConnectionFail;
import net.microtrash.wisperingtree.bus.ClientConnectionSuccess;
import net.microtrash.wisperingtree.bus.FileSentToClient;
import net.microtrash.wisperingtree.bus.FileSentToClientFail;
import net.microtrash.wisperingtree.bus.ServerConnectionFail;
import net.microtrash.wisperingtree.bus.ServerConnectionSuccess;
import net.microtrash.wisperingtree.util.Logger;
import net.microtrash.wisperingtree.util.LoggerInterface;
import net.microtrash.wisperingtree.util.Static;
import net.microtrash.wisperingtree.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

import de.greenrobot.event.EventBus;

public class SyncService extends Service implements BluetoothManager.OnFileReceivedListener {
    private BluetoothManager mBluetoothManager;
    private LoggerInterface mLogger;

    private Hashtable<String, File> mFilesSent = new Hashtable<>();
    private boolean mRunning = false;
    private int mFilesCurrentlySending = 0;

    //private final IBinder mBinder = new LocalBinder();


    IBinder mBinder = new LocalBinder();


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public SyncService getServerInstance() {
            return SyncService.this;
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        mRunning = true;
        mLogger = Logger.getInstance();

        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);

        mBluetoothManager = new BluetoothManager(getApplicationContext(), mLogger);
        mBluetoothManager.setNbrClientMax(7);

        if (!mBluetoothManager.checkBluetoothAviability()) {
            log("Bluetooth not aviable");
            mBluetoothManager.enableBluetooth();
        } else {

            if (BluetoothAdapter.getDefaultAdapter().getAddress().equals(Static.SERVER_MAC)) {
                startServer();
                startFileTransfer();
            } else {
                startClient(0);
            }
        }
    }

    public void startServer() {
        log("Start Server ! Your mac address : " + mBluetoothManager.getYourBtMacAddress());
        setTimeDiscoverable(BluetoothManager.BLUETOOTH_TIME_DICOVERY_3600_SEC);
        selectServerMode();


        for (String mac : Static.getClients().keySet()) {
            createServerForClient(Static.getClients().get(mac), mac);
        }
    }


    private void log(String s) {
        mLogger.log(s);
    }


    public void startClient(int delay) {
        log("Start Client ! Your mac address : " + mBluetoothManager.getYourBtMacAddress());
        setTimeDiscoverable(BluetoothManager.BLUETOOTH_TIME_DICOVERY_120_SEC);
        selectClientMode();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //showDiscoveredDevicesDialog();
                if (mRunning) {
                    mBluetoothManager.setOnFileReceivedListener(SyncService.this);
                    log("Connect to " + Static.SERVER_MAC);
                    createClient(Static.SERVER_MAC);
                }

            }
        }, delay);
    }


    @Override
    public void onFileReceived(File file) {
        log("on file received: " + file.getName());
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
    }

    void createServerForClient(String name, String mac) {
        log("Creating server for client \"" + name + "\" address " + mac + "...");
        mBluetoothManager.createServer(mac);
    }

    public void onClientConnectionSuccess() {
        log("Client connection success !");
    }

    public void onServeurConnectionSuccess() {
        log("Serveur Connexion success !");
        startFileTransfer();
    }

    private void startFileTransfer(int delay) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mRunning) {
                    startFileTransfer();
                }
            }
        }, delay);
    }

    private void startFileTransfer() {
        if (mFilesCurrentlySending > 0) {
            return;
        }
        if (mBluetoothManager.getConnectedClientNum() > 0) {
            File rootDir = new File(Utils.getAppRootDir());
            int i = 0;
            for (File file : rootDir.listFiles()) {
                if (!file.getName().endsWith(".wav") && !file.getName().endsWith(".mp3")) {
                    continue;
                }
                String key = file.getName() + file.lastModified();
                File isTransferred = mFilesSent.get(key);
                if (isTransferred == null) {
                    mBluetoothManager.sendFileToRandomClient(file);
                    mFilesCurrentlySending++;
                    return;
                }
            }
            log("Files transferred, check again...");
            startFileTransfer(4000);

        } else {
            log("No clients connected. Next try in 5 sek...");
            startFileTransfer(5000);
        }
    }

    public void onEvent(FileSentToClient event) {
        mFilesCurrentlySending--;
        File file = event.getFile();
        log("file sent: " + file.getName());
        String key = file.getName() + file.lastModified();
        mFilesSent.put(key, file);

        startFileTransfer(1000);
    }

    public void onEvent(FileSentToClientFail event) {
        mFilesCurrentlySending--;
        startFileTransfer(1000);
    }

    public void onServerConnectionFail(String clientAdressConnectionFail) {
        log("Client connection lost! Mac: " + clientAdressConnectionFail);
        if (mRunning) {
            String clientName = Static.getClients().get(clientAdressConnectionFail);
            createServerForClient(clientName, clientAdressConnectionFail);
            // check for other mClients who could pick up file transfers
            startFileTransfer(500);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log("Stopping service");
        mRunning = false;
        mBluetoothManager.closeAllConnexion();
        EventBus.getDefault().unregister(this);
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
        mBluetoothManager.disconnectClient();
        log("Client connection fail !");
        // try to reconnect
        startClient(5000);
    }

    public void onEventMainThread(ServerConnectionSuccess event) {
        mBluetoothManager.isConnected = true;
        mBluetoothManager.onServerConnectionSuccess(event.mClientAdressConnected);
        onServeurConnectionSuccess();
    }

    public void onEventMainThread(ServerConnectionFail event) {
        mBluetoothManager.onServerConnectionFailed(event.mClientAdressConnectionFail);
        onServerConnectionFail(event.mClientAdressConnectionFail);
    }

    public ArrayList<BluetoothServer> getClientConnectors(){
        return mBluetoothManager.getClientConnectors();
    }
}
