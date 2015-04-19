package net.microtrash.wisperingtree.fragment;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.PresetReverb;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.microtrash.wisperingtree.R;
import net.microtrash.wisperingtree.util.Utils;

import java.io.File;
import java.io.IOException;

import butterknife.ButterKnife;


public class PlayFragment extends Fragment {

    private static final String TAG = "PlayFragment";
    private View mRootView;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_play, container, false);
        ButterKnife.inject(this, mRootView);
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


        File file = getFile(0);

        playFile(file);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                playFile(getFile(1));
            }
        }, 500);
    }

    private void playFile(File file) {
        try {
            if(file != null) {
                MediaPlayer player = new MediaPlayer();
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                player.setDataSource(getActivity().getApplicationContext(), Uri.fromFile(file));
                PresetReverb mReverb = new PresetReverb(0,0);//<<<<<<<<<<<<<
                mReverb.setPreset(PresetReverb.PRESET_LARGEHALL);
                mReverb.setEnabled(true);
                float pan = (float) Math.random();
                player.setVolume(pan, 1-pan);
                player.attachAuxEffect(mReverb.getId());
                player.setAuxEffectSendLevel(1.0f);

                player.prepare();
                player.start();
                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getFile(int i) {
        File rootDir = new File(Utils.getAppRootDir());
        if(i < rootDir.listFiles().length){
            return rootDir.listFiles()[i];
        }
        return null;
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}

