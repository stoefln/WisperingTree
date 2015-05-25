package net.microtrash.wisperingtree.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import net.microtrash.wisperingtree.R;
import net.microtrash.wisperingtree.bus.SamplePlayerSpeedChangeEvent;
import net.microtrash.wisperingtree.service.PlayService;
import net.microtrash.wisperingtree.util.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;


public class PlayFragment extends Fragment {

    private static final String TAG = "PlayFragment";
    private View mRootView;

    @InjectView(R.id.speed_seekbar)
    SeekBar mSeekBar;

    @InjectView(R.id.enable_play_switch)
    SwitchCompat mEnablePlaySwitch;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static PlayFragment newInstance() {
        PlayFragment fragment = new PlayFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_play, container, false);
        ButterKnife.inject(this, mRootView);
        return mRootView;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSeekBar.setMax(300);
        mSeekBar.setProgress(60);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                EventBus.getDefault().post(new SamplePlayerSpeedChangeEvent(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mEnablePlaySwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PlayService.class);
                boolean isChecked = mEnablePlaySwitch.isChecked();
                if (isChecked) {
                    getActivity().startService(intent);
                } else {
                    getActivity().stopService(intent);
                }
                // check and display result
                mEnablePlaySwitch.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mEnablePlaySwitch.setChecked(Utils.isServiceRunning(getActivity(), PlayService.class));
                    }
                }, 1000);
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        mEnablePlaySwitch.setChecked(Utils.isServiceRunning(getActivity(), PlayService.class));
    }
}

