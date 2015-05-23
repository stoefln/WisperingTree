package net.microtrash.wisperingtree.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import net.microtrash.wisperingtree.R;
import net.microtrash.wisperingtree.audio.Sample;
import net.microtrash.wisperingtree.audio.SamplePlayer;
import net.microtrash.wisperingtree.util.Utils;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class PlayFragment extends Fragment {

    private static final String TAG = "PlayFragment";
    private View mRootView;
    private SamplePlayer mSamplePlayer;

    @InjectView(R.id.speed_seekbar)
    SeekBar mSeekBar;
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

    public PlayFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_play, container, false);
        ButterKnife.inject(this, mRootView);
        mSeekBar.setMax(300);
        mSeekBar.setProgress(60);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSamplePlayer.setSpeed(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        return mRootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        File rootDir = new File(Utils.getAppRootDir());
        mSamplePlayer = new SamplePlayer(getActivity());
        for (File file : rootDir.listFiles()) {
            Sample s = new Sample(file);
            mSamplePlayer.addSample(s);
        }
        mSamplePlayer.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSamplePlayer.stop();
    }
}

