package net.microtrash.wisperingtree.audio;


import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.PresetReverb;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class Sample {
    private static final String TAG = "Sample";
    private File mFile;
    private long mTimestampStart = Long.MAX_VALUE;
    private long mTimestampStop = 0;
    private boolean mPlaying = false;
    private MediaPlayer mPlayer;
    private MediaPlayer.OnCompletionListener mOnCompletionListener;
    private boolean mRandomizePan = false;

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener onCompletionListener) {
        mOnCompletionListener = onCompletionListener;
    }

    public File getFile() {
        return mFile;
    }

    public void setFile(File file) {
        mFile = file;
    }

    public long getTimestampStart() {
        return mTimestampStart;
    }

    public void setTimestampStart(long timestampStart) {
        mTimestampStart = timestampStart;
    }

    public long getTimestampStop() {
        return mTimestampStop;
    }

    public long getDurationSinceLastPlayed() {
        if (mPlaying) {
            return 0;
        } else if (mTimestampStop == 0) {
            return Long.MAX_VALUE;
        } else {
            return System.currentTimeMillis() - mTimestampStop;
        }
    }

    public void setTimestampStop(long timestampStop) {
        mTimestampStop = timestampStop;
    }

    public Sample(File file) {
        mFile = file;
    }

    public void play(Context context) throws IOException {
        try {
            if (mFile != null && mFile.exists()) {
                Log.v(TAG, "Starting " + mFile.getName());
                mPlayer = new MediaPlayer();
                mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.setDataSource(context, Uri.fromFile(mFile));
                PresetReverb mReverb = new PresetReverb(0, 0);
                mReverb.setPreset(PresetReverb.PRESET_LARGEHALL);
                mReverb.setEnabled(true);
                if(mRandomizePan) {
                    float pan = (float) Math.random();
                    float vol = 0.2f + (float) Math.random() * 0.8f;
                    mPlayer.setVolume(vol * pan, vol * (1 - pan));
                }
                mPlayer.attachAuxEffect(mReverb.getId());
                mPlayer.setAuxEffectSendLevel(1.0f);
                mPlayer.prepare();
                mPlayer.start();
                mTimestampStart = System.currentTimeMillis();
                mTimestampStop = 0;
                mPlaying = true;
                mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();
                        mTimestampStop = System.currentTimeMillis();
                        mPlaying = false;
                        if (mOnCompletionListener != null) {
                            mOnCompletionListener.onCompletion(mp);
                        }
                    }
                });
            }
        } catch (IOException e) {
            mTimestampStop = System.currentTimeMillis();
            if (mPlayer != null) {
                mPlayer.release();
                mPlayer = null;
            }
            throw new IOException("Error while playing file "+mFile.getAbsolutePath());
        }
    }

    public void stop() {
        if (mPlayer != null) {
            if (mPlaying) {
                mPlayer.stop();
            }
            mPlayer.release();
        }
    }
}
