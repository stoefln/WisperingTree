package net.microtrash.wisperingtree.audio;

import android.content.Context;
import android.os.Handler;

import java.io.IOException;

public class MergeSamplePlayer extends SamplePlayer{


    public MergeSamplePlayer(Context context) {
        super(context);
    }

    public void start() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mActive) {
                    long maxNotPlayDuration = 0;
                    Sample nextSample = null;
                    for (Sample sample : mSamples) {
                        long lastPlay = sample.getDurationSinceLastPlayed();
                        if (lastPlay >= maxNotPlayDuration) {
                            maxNotPlayDuration = lastPlay;
                            nextSample = sample;
                        }
                    }

                    if (nextSample != null) {
                        try {
                            nextSample.play(mContext);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    start();
                }
            }
        }, 60 * 1000 / (mBpm + 1));
    }


}
