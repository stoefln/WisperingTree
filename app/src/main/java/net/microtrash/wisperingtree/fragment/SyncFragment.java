package net.microtrash.wisperingtree.fragment;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.microtrash.wisperingtree.R;
import net.microtrash.wisperingtree.bluetooth.server.BluetoothServer;
import net.microtrash.wisperingtree.bus.ServerConnectionFail;
import net.microtrash.wisperingtree.bus.ServerConnectionSuccess;
import net.microtrash.wisperingtree.service.SyncService;
import net.microtrash.wisperingtree.util.Static;
import net.microtrash.wisperingtree.util.Utils;

import java.util.ArrayList;
import java.util.Hashtable;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;


public class SyncFragment extends Fragment {

    private static final String TAG = "SyncFragment";

    @InjectView(R.id.enable_sync_switch)
    SwitchCompat mEnableSyncSwitch;

    @InjectView(R.id.sync_device_status_container)
    LinearLayout mDeviceStatusContainer;

    private View mRootView;

    private Hashtable<String, View> mDevices = new Hashtable<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_sync, container, false);
        ButterKnife.inject(this, mRootView);
        EventBus.getDefault().register(this);
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mEnableSyncSwitch.setChecked(Utils.isServiceRunning(getActivity(), SyncService.class));

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        Hashtable<String, String> clients = Static.getClients();
        for (String mac : clients.keySet()) {
            if (mDevices.get(mac) == null) {
                inflater.inflate(R.layout.sync_device_status_item, mDeviceStatusContainer, true);
                TextView deviceIndicator = (TextView) mDeviceStatusContainer.getChildAt(mDeviceStatusContainer.getChildCount() - 1);
                deviceIndicator.setText(clients.get(mac));
                mDevices.put(mac, deviceIndicator);
            }
        }

        if (BluetoothAdapter.getDefaultAdapter().getAddress().equals(Static.SERVER_MAC)) {
            updateDeviceIndicators();
        } else {
            mDeviceStatusContainer.setVisibility(View.GONE);
        }
    }

    private SyncService mSyncService;
    ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            SyncService.LocalBinder mLocalBinder = (SyncService.LocalBinder) service;
            mSyncService = mLocalBinder.getServerInstance();
            updateDeviceIndicators();
        }
    };

    private void updateDeviceIndicators() {
        if (!Utils.isServiceRunning(getActivity(), SyncService.class)) {
            return;
        }
        if (mSyncService == null) {
            Intent mIntent = new Intent(getActivity(), SyncService.class);
            getActivity().bindService(mIntent, mConnection, Context.BIND_AUTO_CREATE);
            return;
        }
        ArrayList<BluetoothServer> connectors = mSyncService.getClientConnectors();

        for (String mac : mDevices.keySet()) {
            View view = mDevices.get(mac);
            view.setActivated(false);
        }
        for (BluetoothServer connector : connectors) {
            String mac = connector.getClientAddress();
            View view = mDevices.get(mac);
            view.setActivated(true);
        }
    }


    public void onEventMainThread(ServerConnectionFail event) {
        View view = mDevices.get(event.mClientAdressConnectionFail);
        view.setActivated(false);
    }

    public void onEventMainThread(ServerConnectionSuccess event) {
        View view = mDevices.get(event.mClientAdressConnected);
        view.setActivated(true);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        mEnableSyncSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SyncService.class);
                boolean isChecked = mEnableSyncSwitch.isChecked();
                if (isChecked) {
                    getActivity().startService(intent);
                } else {
                    if (mSyncService != null) {
                        try {
                            getActivity().unbindService(mConnection);
                        } catch (Exception e){}
                    }
                    getActivity().stopService(intent);
                }
                // check and display result
                mEnableSyncSwitch.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mEnableSyncSwitch.setChecked(Utils.isServiceRunning(getActivity(), SyncService.class));
                    }
                }, 1000);
            }
        });


    }

    @Override
    public void onDestroy() {
        if (mSyncService != null) {
            try {
                getActivity().unbindService(mConnection);
            } catch (Exception e){

            } finally {
                mSyncService = null;
            }
        }
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }


}

