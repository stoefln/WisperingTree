package net.microtrash.wisperingtree.audio;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;

public class SequencialSamplePlayer extends SamplePlayer implements MediaPlayer.OnCompletionListener {

    private final Handler mHandler;
    int mCurrentSampleIndex = 0;

    public SequencialSamplePlayer(Context context) {
        super(context);
        mHandler = new Handler();
    }

    @Override
    public void start() {
        startNextSample();
    }


    @Override
    public void onCompletion(MediaPlayer mp) {
        startNextSample();
    }

    private void startNextSample() {
        if (mActive) {
            mCurrentSampleIndex++;
            if (mCurrentSampleIndex >= mSamples.size()) {
                mCurrentSampleIndex = 0;
            }
            try {
                Sample sample = mSamples.get(mCurrentSampleIndex);
                sample.setOnCompletionListener(this);
                sample.play(mContext);
            } catch (Exception e) {
                e.printStackTrace();
                if(mSamples.size() > 0) {
                    mSamples.remove(mCurrentSampleIndex);
                }
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startNextSample();
                    }
                }, 1000);

            }
        }
    }
}
