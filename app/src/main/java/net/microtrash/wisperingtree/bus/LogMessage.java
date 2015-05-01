package net.microtrash.wisperingtree.bus;

/**
 * Created by steph on 5/1/15.
 */
public class LogMessage {
    private String mText;

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        mText = text;
    }

    public LogMessage(String text) {
        mText = text;
    }
}
