package net.microtrash.wisperingtree.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import net.microtrash.wisperingtree.bluetooth.mananger.BluetoothManager;
import net.microtrash.wisperingtree.bus.ClientConnectionFail;
import net.microtrash.wisperingtree.bus.ClientConnectionSuccess;
import net.microtrash.wisperingtree.bus.FileSentToClient;
import net.microtrash.wisperingtree.bus.FileSentToClientFail;
import net.microtrash.wisperingtree.bus.LogMessage;
import net.microtrash.wisperingtree.bus.ServeurConnectionFail;
import net.microtrash.wisperingtree.bus.ServeurConnectionSuccess;
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
    private boolean mRunning = false;
    private Hashtable<String, String> mClients;
    private int mFilesCurrentlySending = 0;

    //private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
        mLogger.setOnLogListener(new LoggerInterface() {

            Handler mHandler = new Handler(Looper.getMainLooper());

            @Override
            public void log(final String message) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        EventBus.getDefault().post(new LogMessage(message));
                    }
                });
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
            log("Bluetooth not aviable");
            mBluetoothManager.enableBluetooth();
        } else {

            if (BluetoothAdapter.getDefaultAdapter().getAddress().equals(Static.SERVER_MAC)) {
                serverType();
                startFileTransfer();
            } else {
                clientType();
            }
        }
    }

    public void serverType() {
        log("Start Server ! Your mac address : " + mBluetoothManager.getYourBtMacAddress());
        setTimeDiscoverable(BluetoothManager.BLUETOOTH_TIME_DICOVERY_3600_SEC);
        selectServerMode();


        for (String mac : getClients().keySet()) {
            createServerForClient(getClients().get(mac), mac);
        }
    }

    private Hashtable<String, String> getClients() {
        if (mClients == null) {
            mClients = new Hashtable<>();
            mClients.put("40:B0:FA:F4:EC:B9", "LG-E430");
            //mClients.put("HTC-ONE", "98:0D:2E:C0:30:86");
            mClients.put("D8:90:E8:FB:D8:C2", "S4");
            mClients.put("94:D7:71:E3:E6:61", "S3");
            mClients.put("B4:CE:F6:77:27:B0", "HTC OPCV1");
            mClients.put("AC:36:13:D9:C8:1E", "Galaxy S3 Mini");
        }
        return mClients;
    }

    private void log(String s) {
        mLogger.log(s);
    }


    public void clientType() {
        log("Start Client ! Your mac address : " + mBluetoothManager.getYourBtMacAddress());
        setTimeDiscoverable(BluetoothManager.BLUETOOTH_TIME_DICOVERY_120_SEC);
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
                log("on file received: " + file.getName());
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            }
        });

        onDeviceSelectedForConnection(Static.SERVER_MAC);
    }

    public void onDeviceSelectedForConnection(String addressMac) {
        log("Connect to " + addressMac);
        createClient(addressMac);
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

    private void startFileTransfer(int delay){
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
        if(mFilesCurrentlySending > 0){
            return;
        }
        if (mBluetoothManager.getConnectedClientNum() > 0) {
            File rootDir = new File(Utils.getAppRootDir());
            int i = 0;
            for (File file : rootDir.listFiles()) {
                if(!file.getName().endsWith(".wav") && !file.getName().endsWith(".mp3")){
                    continue;
                }
                String key = file.getName() + file.length();
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
        log("file sent: "+ file.getName());
        String key = file.getName() + file.length();
        mFilesSent.put(key, file);

        startFileTransfer(1000);
    }

    public void onEvent(FileSentToClientFail event) {
        mFilesCurrentlySending--;
        startFileTransfer(1000);
    }

    public void onEvent(LogMessage message) {
        message.save();
    }

    public void onServerConnectionFail(String clientAdressConnectionFail) {
        log("Client connection lost! Mac: " + clientAdressConnectionFail);
        if (mRunning) {
            String clientName = getClients().get(clientAdressConnectionFail);
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
       // clientConnect();
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
