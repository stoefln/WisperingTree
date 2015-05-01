package net.microtrash.wisperingtree.fragment;

import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import net.microtrash.wisperingtree.AudioRecorder;
import net.microtrash.wisperingtree.R;
import net.microtrash.wisperingtree.service.SyncService;
import net.microtrash.wisperingtree.util.Profiler;
import net.microtrash.wisperingtree.util.Static;
import net.microtrash.wisperingtree.util.Tools;
import net.microtrash.wisperingtree.util.Utils;
import net.microtrash.wisperingtree.view.RangeSeekBar;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class RecordFragment extends Fragment {
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

    @InjectView(R.id.enable_sync_switch)
    SwitchCompat mEnableSyncSwitch;

    private int mRecNum;
    private Long mLastTimeAboveMin = null;
    private long mSampleStartTime;
    private boolean mSampling = false;

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

    public RecordFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_record, container, false);
        ButterKnife.inject(this, mRootView);
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mRecNum = Tools.getPreferenceInt(getActivity(), Static.KEY_LAST_REC_NUM);
        initRecorder(getNextFilename());
        mRecorder.start();
        observeAudio();

        mEnableSyncSwitch.setChecked(Utils.isServiceRunning(getActivity(), SyncService.class));
    }

    @Override
    public void onPause() {
        super.onPause();
        Tools.putPreference(getActivity(), Static.KEY_LAST_REC_NUM, mRecNum);

        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAudioLevelBar.setNormalizedMinValue(0.5);
        mEnableSyncSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){

                }
            }
        });
    }


    private void observeAudio() {

        mRootView.postDelayed(new Runnable() {
            @Override
            public void run() {
            if (mRecorder != null) {
                int amp = mRecorder.getMaxAmplitude();
                //Log.v(TAG, "vol: " + amp + " state: " + mRecorder.getState());

                //mAudioLevelBar.getAbsoluteMaxValue(30000);
                float normalizedLevel = (float) amp / (float) 15000;

                if (normalizedLevel > mAudioLevelBar.getNormalizedMaxValue() && !mSampling) {
                    Log.v(TAG, "vol: " + amp + " normalized: " + normalizedLevel);
                    startSampling();
                    mSampling = true;
                }
                if (normalizedLevel > mAudioLevelBar.getNormalizedMinValue()) {
                    mLastTimeAboveMin = System.currentTimeMillis();
                }
                if (mSampling && mLastTimeAboveMin != null) {
                    long timeDiff = System.currentTimeMillis() - mLastTimeAboveMin;
                    // if recording was started AND level has stayed below min level for at least a second -> stop
                    if (timeDiff > 1000 && normalizedLevel < mAudioLevelBar.getNormalizedMinValue()) {
                        stopSampling();
                        mSampling = false;
                        mLastTimeAboveMin = null;
                    }
                }
                mAudioLevelBar.setLevel(normalizedLevel);
                observeAudio();
            }
            }
        }, 50);

    }

    private void startSampling() {
        mSampleStartTime = System.currentTimeMillis();
        Profiler.start("start Sampling");
        mRecorder.setAddSamples(true);
        mRecordingIndicator.setBackgroundColor(getResources().getColor(R.color.recording_red));
    }

    private void stopSampling() {
        Log.v(TAG, "StopSampling. Filelength: " + (float) (System.currentTimeMillis() - mSampleStartTime) / 1000f + " sec. Filename: " + mRecorder.getOutputFile());
        if (mRecorder != null && mLastTimeAboveMin != null) {
            try {
                mRecorder.stop();
                mRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                initRecorder(getNextFilename());
                mRecorder.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mRecordingIndicator.setBackgroundColor(getResources().getColor(R.color.recording_green));
    }

    private String getNextFilename() {
        String dirPath = Utils.getAppRootDir();
        Utils.mkDir(dirPath);
        String nextfilename = dirPath + "/audiorecordtest" + mRecNum % 20 + ".wav";
        mRecNum++;
        return nextfilename;
    }

    private static final String LOG_TAG = "AudioRecordTest";
    private static String mEmptyFileName = null;

    private AudioRecorder mRecorder = null;

    private MediaPlayer mPlayer = null;


    private final static int[][] sampleRates =
            {
                    {44100, 22050, 11025, 8000},
                    {22050, 11025, 8000, 44100},
                    {11025, 8000, 22050, 44100},
                    {8000, 11025, 22050, 44100}
            };

    private void initRecorder(String filename) {
        //mRecorder = new RehearsalAudioRecorder(true, MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int i = 0;

        do {
            Log.v(TAG, "initializing with: " + sampleRates[0][i]);
            mRecorder = new AudioRecorder(true, MediaRecorder.AudioSource.MIC, sampleRates[0][i], AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
        }
        while ((++i < sampleRates.length) & !(mRecorder.getState() == AudioRecorder.State.INITIALIZING));

        mRecorder.setOutputFile(filename);
        mRecorder.prepare();
    }


}

