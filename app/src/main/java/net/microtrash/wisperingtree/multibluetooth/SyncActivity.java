package net.microtrash.wisperingtree.multibluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ToggleButton;

import com.ramimartin.multibluetooth.activity.BluetoothFragmentActivity;
import com.ramimartin.multibluetooth.bluetooth.mananger.BluetoothManager;

import net.microtrash.wisperingtree.R;
import net.microtrash.wisperingtree.util.Static;
import net.microtrash.wisperingtree.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class SyncActivity extends BluetoothFragmentActivity implements DiscoveredDialogFragment.DiscoveredDialogListener {

    @InjectView(R.id.listview)
    ListView mListView;
    ArrayAdapter<String> mAdapter;
    List<String> mListLog;

    @InjectView(R.id.communication)
    EditText mEditText;
    @InjectView(R.id.send)
    ImageButton mSendBtn;

    @InjectView(R.id.client)
    ToggleButton mClientToggleBtn;
    @InjectView(R.id.serveur)
    ToggleButton mServerToggleBtn;

    @InjectView(R.id.connect)
    Button mConnectBtn;
    @InjectView(R.id.disconnect)
    Button mDisconnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_bluetooth);
        ButterKnife.inject(this);

        mListLog = new ArrayList<String>();
        mAdapter = new ArrayAdapter<String>(this, R.layout.item_console, mListLog);
        mListView.setAdapter(mAdapter);

        Utils.mkDir(Utils.getAppRootDir());

        if(BluetoothAdapter.getDefaultAdapter().getAddress().equals(Static.SERVER_MAC)){
            serverType();
        } else{
            clientType();
        }
    }

    @Override
    public int myNbrClientMax() {
        return 7;
    }

    @OnClick(R.id.serveur)
    public void serverType() {
        setLogText("===> Start Server ! Your mac address : " + mBluetoothManager.getYourBtMacAddress());
        //setTimeDiscoverable(BluetoothManager.BLUETOOTH_TIME_DICOVERY_3600_SEC);
        selectServerMode();
        mServerToggleBtn.setChecked(true);
        mClientToggleBtn.setChecked(false);
        mConnectBtn.setEnabled(true);
        mConnectBtn.setText("Scan Devices");

        mConnectBtn.postDelayed(new Runnable() {
            @Override
            public void run() {
                createServerForClient(Static.CLIENT_MAC1);
            }
        },2000);
    }

    @OnClick(R.id.client)
    public void clientType() {
        setLogText("===> Start Client ! Your mac address : " + mBluetoothManager.getYourBtMacAddress());
        //setTimeDiscoverable(BluetoothManager.BLUETOOTH_TIME_DICOVERY_120_SEC);
        selectClientMode();
        mServerToggleBtn.setChecked(false);
        mClientToggleBtn.setChecked(true);
        mConnectBtn.setEnabled(true);

        mConnectBtn.postDelayed(new Runnable() {
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
                setLogText("===> on file received: " + file.getName());
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            }
        });

        onDeviceSelectedForConnection(Static.SERVER_MAC);
    }

    void createServerForClient(String mac){
        setLogText("===> Creating server for client address " + mac + "...");
        mBluetoothManager.createServer(mac);
    }

    @OnClick(R.id.disconnect)
    public void disconnect() {
        setLogText("===> Disconnect");
        disconnectClient();
        disconnectServer();
    }

    @OnClick(R.id.send)
    public void send() {
        if (isConnected()) {
            sendMessage(mEditText.getText().toString());
            setLogText("===> Send : " + mEditText.getText().toString());
        }
    }

    @Override
    public void onBluetoothStartDiscovery() {
        setLogText("===> Start discovering ! Your mac address : " + mBluetoothManager.getYourBtMacAddress());
    }

    @Override
    public void onBluetoothDeviceFound(BluetoothDevice device) {
        if(getTypeBluetooth() == BluetoothManager.TypeBluetooth.Server) {
            setLogText("===> Device detected and Thread Server created for this address : " + device.getAddress());
        }else{
            setLogText("===> Device detected : "+ device.getAddress());
        }
    }

    @Override
    public void onClientConnectionSuccess() {
        setLogText("===> Client Connexion success !");
        mEditText.setText("Client");
        mSendBtn.setEnabled(true);
        mConnectBtn.setEnabled(false);
        mDisconnect.setEnabled(true);
    }

    @Override
    public void onClientConnectionFail() {
        setLogText("===> Client Connexion fail !");
        mServerToggleBtn.setChecked(false);
        mClientToggleBtn.setChecked(false);
        mDisconnect.setEnabled(false);
        mConnectBtn.setEnabled(false);
        mConnectBtn.setText("Connect");
        mEditText.setText("");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                clientConnect();
            }
        }, 1000);
    }

    @Override
    public void onServeurConnectionSuccess() {
        setLogText("===> Serveur Connexion success !");
        mEditText.setText("Server");
        mDisconnect.setEnabled(true);
        File rootDir = new File(Utils.getAppRootDir());
        for (File file : rootDir.listFiles()) {
            mBluetoothManager.sendFile(file);
            break;
        }
    }

    @Override
    public void onServerConnectionFail(String clientAdressConnectionFail) {
        setLogText("===> Client connection lost! Mac: "+clientAdressConnectionFail);
        createServerForClient(Static.CLIENT_MAC1);
    }

    @Override
    public void onBluetoothCommunicator(String messageReceive) {
        setLogText("===> receive msg : " + messageReceive);
    }

    @Override
    public void onBluetoothNotAviable() {
        setLogText("===> Bluetooth not aviable on this device");
        mSendBtn.setEnabled(false);
        mClientToggleBtn.setEnabled(false);
        mServerToggleBtn.setEnabled(false);
        mConnectBtn.setEnabled(false);
        mDisconnect.setEnabled(false);
    }

    public void setLogText(String text) {
        mListLog.add(text);
        mAdapter.notifyDataSetChanged();
        mListView.setSelection(mListView.getCount() - 1);
    }

    private void showDiscoveredDevicesDialog() {
        String tag = DiscoveredDialogFragment.class.getSimpleName();
        DiscoveredDialogFragment fragment = DiscoveredDialogFragment.newInstance();
        fragment.setListener(this);
        showDialogFragment(fragment, tag);
    }

    private void showDialogFragment(DialogFragment dialogFragment, String tag) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(dialogFragment, tag);
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onDeviceSelectedForConnection(String addressMac) {
        setLogText("===> Connect to " + addressMac);
        createClient(addressMac);
    }

    @Override
    public void onScanClicked() {
        scanAllBluetoothDevice();
    }
}
