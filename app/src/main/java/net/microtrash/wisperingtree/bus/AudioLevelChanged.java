package net.microtrash.wisperingtree.bus;

import java.io.Serializable;

/**
 * Created by steph on 5/14/15.
 */
public class AudioLevelChanged implements Serializable {
    float mValue;

    public float getValue() {
        return mValue;
    }

    public AudioLevelChanged(float value) {
        mValue = value;
    }
}
