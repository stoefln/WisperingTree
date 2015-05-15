package net.microtrash.wisperingtree.bus;

/**
 * Created by steph on 5/14/15.
 */
public class AudioPeakDetectionChanged {
    private final int mMin;
    private final int mMax;

    public int getMin() {
        return mMin;
    }

    public int getMax() {
        return mMax;
    }

    public AudioPeakDetectionChanged(int min, int max) {
        mMin = min;
        mMax = max;
    }
}
