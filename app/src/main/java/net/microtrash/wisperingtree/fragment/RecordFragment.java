package net.microtrash.wisperingtree.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.microtrash.wisperingtree.R;
import net.microtrash.wisperingtree.bus.AudioLevelChanged;
import net.microtrash.wisperingtree.bus.AudioPeakDetectionChanged;
import net.microtrash.wisperingtree.bus.SamplingStart;
import net.microtrash.wisperingtree.bus.SamplingStop;
import net.microtrash.wisperingtree.service.RecordService;
import net.microtrash.wisperingtree.util.Static;
import net.microtrash.wisperingtree.util.Tools;
import net.microtrash.wisperingtree.util.Utils;
import net.microtrash.wisperingtree.view.RangeSeekBar;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;


public class RecordFragment extends Fragment implements RangeSeekBar.OnRangeSeekBarChangeListener {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "RecordFragment";
    private View mRootView;

    /*@InjectView(R.id.progressBar)
    ProgressBar mAudioLevelBar;*/

    @InjectView(R.id.audioLevelBar)
    RangeSeekBar mAudioLevelBar;

    @InjectView(R.id.recording_indicator)
    View mRecordingIndicator;

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
        if (!Utils.isServiceRunning(getActivity(), RecordService.class)){
            Intent intent = new Intent(getActivity(), RecordService.class);
            getActivity().startService(intent);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAudioLevelBar.setRangeValues(0, 15000);
        mAudioLevelBar.setNormalizedMinValue(0.5);

        mAudioLevelBar.setOnRangeSeekBarChangeListener(this);
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
        Integer min = (Integer) minValue;
        Integer max = (Integer) maxValue;
        Tools.putPreference(getActivity(), Static.KEY_MIN_NOISE_VALUE, min);
        Tools.putPreference(getActivity(), Static.KEY_MAX_NOISE_VALUE, max);
        EventBus.getDefault().post(new AudioPeakDetectionChanged(min, max));
    }
}

