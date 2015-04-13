package net.microtrash.wisperingtree;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.microtrash.wisperingtree.view.RangeSeekBar;

import java.io.IOException;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


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

    private int mRecNum;
    private Long mLastTimeAboveMax = null;
    private long mSampleStartTime;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static RecordFragment newInstance(int sectionNumber) {
        RecordFragment fragment = new RecordFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        mEmptyFileName = "/dev/null";


        mAudioLevelBar.setNormalizedMinValue(0.5);
        initRecorder(mEmptyFileName);
        mRecorder.start();
        observeAudio();
    }

    private void observeAudio() {

        mRootView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mRecorder != null) {
                    int amp = mRecorder.getMaxAmplitude();
                    //Log.v(TAG, "vol: " + amp + " max: " + MediaRecorder.getAudioSourceMax());

                    //mAudioLevelBar.getAbsoluteMaxValue(30000);
                    float normalizedLevel = (float) amp / (float) 20000;
                    if (normalizedLevel > mAudioLevelBar.getNormalizedMaxValue()) {
                        if (mLastTimeAboveMax == null) {
                            startSampling();
                        }
                        mLastTimeAboveMax = System.currentTimeMillis();

                    } else if (mLastTimeAboveMax != null) {
                        long timeDiff = System.currentTimeMillis() - mLastTimeAboveMax;

                        if (timeDiff > 1000 && normalizedLevel < mAudioLevelBar.getNormalizedMinValue()) {
                            stopSampling();
                            mLastTimeAboveMax = null;
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
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
        mRecordingIndicator.setBackgroundColor(getResources().getColor(R.color.recording_red));
        String filename = Environment.getExternalStorageDirectory().getAbsolutePath();
        filename += "/audiorecordtest" + mRecNum % 10 + ".3gp";
        initRecorder(filename);

        Log.v(TAG, "startSampling " + filename);
        mRecorder.start();
        mRecNum++;
    }

    private void stopSampling() {
        Log.v(TAG, "StopSampling. Filelength: " + (float) (System.currentTimeMillis() - mSampleStartTime) / 1000f + " sec");
        if (mRecorder != null && mLastTimeAboveMax != null) {
            try {
                mRecorder.stop();
                mRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mRecorder = null;

            try {
                initRecorder(mEmptyFileName);
                mRecorder.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mRecordingIndicator.setBackgroundColor(getResources().getColor(R.color.recording_green));
    }


    private static final String LOG_TAG = "AudioRecordTest";
    private static String mEmptyFileName = null;

    private MediaRecorder mRecorder = null;

    @InjectView(R.id.btn_play)
    protected View mPlayButton = null;

    private MediaPlayer mPlayer = null;


    @OnClick(R.id.btn_play)
    void onPlayButtonClick() {
        if (mLastTimeAboveMax != null) {
            stopSampling();
        } else {
            startSampling();
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mEmptyFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    private void initRecorder(String filename) {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(filename);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }
}

