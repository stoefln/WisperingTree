package net.microtrash.wisperingtree.bus;

/**
 * Created by steph on 5/23/15.
 */
public class SamplePlayerSpeedChangeEvent {

    private int mSpeed;

    public SamplePlayerSpeedChangeEvent(int speed) {
        mSpeed = speed;
    }

    public int getSpeed() {
        return mSpeed;
    }
}
