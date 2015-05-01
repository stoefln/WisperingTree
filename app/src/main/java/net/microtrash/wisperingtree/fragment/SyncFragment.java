package net.microtrash.wisperingtree.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.microtrash.wisperingtree.R;
import net.microtrash.wisperingtree.service.SyncService;
import net.microtrash.wisperingtree.util.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class SyncFragment extends Fragment {

    private static final String TAG = "SyncFragment";

    @InjectView(R.id.enable_sync_switch)
    SwitchCompat mEnableSyncSwitch;

    private View mRootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_sync, container, false);
        ButterKnife.inject(this, mRootView);

        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mEnableSyncSwitch.setChecked(Utils.isServiceRunning(getActivity(), SyncService.class));
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
        super.onDestroy();
    }
}

