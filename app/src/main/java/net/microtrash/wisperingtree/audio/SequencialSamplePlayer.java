package net.microtrash.wisperingtree.audio;

import android.content.Context;
import android.media.MediaPlayer;

import java.io.IOException;

public class SequencialSamplePlayer extends SamplePlayer implements MediaPlayer.OnCompletionListener {

    int mCurrentSampleIndex = 0;

    public SequencialSamplePlayer(Context context) {
        super(context);
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
            Sample sample = mSamples.get(mCurrentSampleIndex);
            sample.setOnCompletionListener(this);
            try {
                sample.play(mContext);
            } catch (IOException e) {
                e.printStackTrace();
                mSamples.remove(mCurrentSampleIndex);
                startNextSample();
            }
        }
    }
}
