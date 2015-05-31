package net.microtrash.wisperingtree.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

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
    private Thread mThread;


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

    private void startFileTransfer() {

        log("Starting worker task for file transfer...");
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    while (mRunning) {
                        Thread.sleep(1000);

                        if(mFilesCurrentlySending < 0){
                            mLogger.log("!!!mFilesCurrentlySending: "+mFilesCurrentlySending);
                            mFilesCurrentlySending = 0;
                        }
                        if (mFilesCurrentlySending > 0) {
                            continue;
                        }
                        if (mBluetoothManager.getConnectedClientNum() > 0) {
                            File rootDir = new File(Utils.getAppRootDir());
                            boolean newFileSent = false;
                            for (File file : rootDir.listFiles()) {
                                if (!file.getName().endsWith(".wav") && !file.getName().endsWith(".mp3")) {
                                    continue;
                                }
                                String key = file.getName() + file.lastModified();
                                File isTransferred = mFilesSent.get(key);
                                if (isTransferred == null) {
                                    mBluetoothManager.sendFileToRandomClient(file);
                                    mFilesCurrentlySending++;
                                    newFileSent = true;
                                    break;
                                }
                            }
                            if(!newFileSent) {
                                log("Files transferred, check again in 5 sek...");
                                Thread.sleep(5000);
                            }

                        } else {
                            log("No clients connected. Next try in 5 sek...");
                            Thread.sleep(5000);
                        }
                    }
                } catch (InterruptedException e) {
                    return null;
                }
                return null;
            }
        }.execute();

    }

    public void onEvent(FileSentToClient event) {
        File file = event.getFile();
        log("file sent: " + file.getName());
        String key = file.getName() + file.lastModified();
        mFilesSent.put(key, file);
        mFilesCurrentlySending--;
    }

    public void onEvent(FileSentToClientFail event) {
        mFilesCurrentlySending--;
    }

    public void onServerConnectionFail(String clientAdressConnectionFail) {
        log("Client connection lost to "+Static.getClients().get(clientAdressConnectionFail));
        if (mRunning) {
            String clientName = Static.getClients().get(clientAdressConnectionFail);
            createServerForClient(clientName, clientAdressConnectionFail);
            // check for other mClients who could pick up file transfers
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
        log("Client connection success !");
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
        log("Server connection success !");
    }

    public void onEventMainThread(ServerConnectionFail event) {
        mBluetoothManager.onServerConnectionFailed(event.mClientAdressConnectionFail);
        onServerConnectionFail(event.mClientAdressConnectionFail);
    }

    public ArrayList<BluetoothServer> getClientConnectors() {
        return mBluetoothManager.getClientConnectors();
    }
}
