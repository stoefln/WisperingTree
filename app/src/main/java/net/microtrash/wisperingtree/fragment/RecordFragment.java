package net.microtrash.wisperingtree.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.microtrash.wisperingtree.R;
import net.microtrash.wisperingtree.bus.AdaptiveThresholdChanged;
import net.microtrash.wisperingtree.bus.AudioLevelChanged;
import net.microtrash.wisperingtree.bus.AudioPeakDetectionChanged;
import net.microtrash.wisperingtree.bus.SamplingStart;
import net.microtrash.wisperingtree.bus.SamplingStop;
import net.microtrash.wisperingtree.service.LightControlService;
import net.microtrash.wisperingtree.service.RecordService;
import net.microtrash.wisperingtree.util.LightsAnimator;
import net.microtrash.wisperingtree.util.Static;
import net.microtrash.wisperingtree.util.Tools;
import net.microtrash.wisperingtree.util.Utils;
import net.microtrash.wisperingtree.view.RangeSeekBar;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import tv.piratemedia.lightcontroler.LightsController;


public class RecordFragment extends Fragment implements RangeSeekBar.OnRangeSeekBarChangeListener {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "RecordFragment";
    private View mRootView;


    @InjectView(R.id.audioLevelBar)
    RangeSeekBar mAudioLevelBar;

    @InjectView(R.id.recording_indicator)
    View mRecordingIndicator;

    @InjectView(R.id.enable_record_switch)
    SwitchCompat mEnableRecordSwitch;
    private LightsController mLightsController;

    @InjectView(R.id.enable_light_control_switch)
    SwitchCompat mEnableLightControlSwitch;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static RecordFragment newInstance() {
        RecordFragment fragment = new RecordFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_record, container, false);
        ButterKnife.inject(this, mRootView);
        EventBus.getDefault().register(this);
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mEnableRecordSwitch.setChecked(Utils.isServiceRunning(getActivity(), RecordService.class));
        mEnableLightControlSwitch.setChecked(Utils.isServiceRunning(getActivity(), LightControlService.class));
    }


    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAudioLevelBar.setRangeValues(0, 60000);
        //mAudioLevelBar.setNormalizedMinValue(0.5);
        int min = 10;
        int max = 100;

        try {
            min = Tools.getPreferenceInt(getActivity(), Static.KEY_MIN_NOISE_VALUE);
            max = Tools.getPreferenceInt(getActivity(), Static.KEY_MAX_NOISE_VALUE);
        } catch (Exception e){
            e.printStackTrace();
        }
        mAudioLevelBar.setSelectedMinValue(min);
        mAudioLevelBar.setSelectedMaxValue(max);
        mAudioLevelBar.setOnRangeSeekBarChangeListener(this);

        mEnableRecordSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), RecordService.class);
                boolean isChecked = mEnableRecordSwitch.isChecked();
                if (isChecked) {
                    getActivity().startService(intent);
                } else {
                    getActivity().stopService(intent);
                }
                // check and display result
                mEnableRecordSwitch.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mEnableRecordSwitch.setChecked(Utils.isServiceRunning(getActivity(), RecordService.class));
                    }
                }, 1000);
            }
        });

        mEnableLightControlSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), LightControlService.class);
                boolean isChecked = mEnableLightControlSwitch.isChecked();
                if (isChecked) {
                    getActivity().startService(intent);
                } else {
                    getActivity().stopService(intent);
                }
                // check and display result
                mEnableLightControlSwitch.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mEnableLightControlSwitch.setChecked(Utils.isServiceRunning(getActivity(), LightControlService.class));
                    }
                }, 1000);
            }
        });

    }

    public void onEventMainThread(AdaptiveThresholdChanged event){
        mAudioLevelBar.setSelectedMinValue(event.getMinLevel());
        mAudioLevelBar.setSelectedMaxValue(event.getMaxLevel());
    }

    public void onEventMainThread(AudioLevelChanged event) {
        mAudioLevelBar.setLevel(event.getValue());
    }

    public void onEventMainThread(SamplingStart event){
        mRecordingIndicator.setBackgroundColor(getResources().getColor(R.color.recording_red));
    }

    public void onEventMainThread(SamplingStop event){
        mRecordingIndicator.setBackgroundColor(getResources().getColor(R.color.recording_green));
    }

    @Override
    public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
        int min = (Integer) minValue;
        int max = (Integer) maxValue;
        Tools.putPreference(getActivity(), Static.KEY_MIN_NOISE_VALUE, min);
        Tools.putPreference(getActivity(), Static.KEY_MAX_NOISE_VALUE, max);
        EventBus.getDefault().post(new AudioPeakDetectionChanged(min, max));
    }
}

