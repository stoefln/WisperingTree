package net.microtrash.wisperingtree.bus;

/**
 * Created by steph on 5/14/15.
 */
public class AudioLevelChanged {
    float mValue;

    public float getValue() {
        return mValue;
    }

    public AudioLevelChanged(float value) {
        mValue = value;
    }
}
