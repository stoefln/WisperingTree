package net.microtrash.wisperingtree.audio;

import android.content.Context;
import android.os.Handler;

import java.util.ArrayList;

public class SamplePlayer {
    private final Context mContext;
    ArrayList<Sample> mSamples = new ArrayList<>();
    private int mBpm = 30;
    private boolean mActive = true;

    public SamplePlayer(Context context) {
        mContext = context;
    }

    public void addSample(Sample sample) {
        mSamples.add(sample);
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
                        nextSample.play(mContext);
                    }

                    start();
                }
            }
        }, 60 * 1000 / (mBpm + 1));
    }

    public void stop() {
        for (Sample sample : mSamples) {
            sample.stop();
        }
        mActive = false;
    }

    public void setSpeed(int speed) {
        mBpm = speed;
    }
}
