package net.microtrash.wisperingtree.audio;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;

public abstract class SamplePlayer {

    protected final Context mContext;

    ArrayList<Sample> mSamples = new ArrayList<>();
    protected boolean mActive = true;
    protected int mBpm = 30;
    public void setSpeed(int speed) {
        mBpm = speed;
    }

    public SamplePlayer(Context context) {
        mContext = context;
    }

    public void addSample(Sample sample) {
        mSamples.add(sample);
    }

    public abstract void start();

    public void stop() {
        for (Sample sample : mSamples) {
            sample.stop();
        }
        mActive = false;
    }


    public boolean hasFile(File file) {
        for (Sample sample : mSamples) {
            if (sample.getFile().getAbsolutePath().equals(file.getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<Sample> getSamples() {
        return mSamples;
    }
}
