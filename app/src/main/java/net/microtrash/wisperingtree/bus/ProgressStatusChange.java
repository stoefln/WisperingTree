package net.microtrash.wisperingtree.bus;

/**
 * Created by steph on 5/5/15.
 */
public class ProgressStatusChange {

    private float mProgress = 0;
    private String mText;

    public ProgressStatusChange(float progress, String text) {
        mProgress = progress;
        mText = text;
    }

    public float getProgress() {
        return mProgress;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
    }
}
