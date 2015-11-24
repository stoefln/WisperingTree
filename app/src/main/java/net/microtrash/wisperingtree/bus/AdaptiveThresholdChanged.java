package net.microtrash.wisperingtree.bus;

import java.io.Serializable;

/**
 * Created by Stephan Petzl (stephan.petzl@gmail.com) on 11/22/15.
 */
public class AdaptiveThresholdChanged implements Serializable{
    public static final int NO_CHANGE = -1;
    private int mMinLevel = NO_CHANGE;
    private int mMaxLevel = NO_CHANGE;

    public AdaptiveThresholdChanged(int minLevel, int maxLevel) {
        mMinLevel = minLevel;
        mMaxLevel = maxLevel;
    }

    public int getMaxLevel() {
        return mMaxLevel;
    }

    public int getMinLevel() {
        return mMinLevel;
    }
}
