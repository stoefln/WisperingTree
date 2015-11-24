package net.microtrash.wisperingtree.util;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.util.Log;

import tv.piratemedia.lightcontroler.LightsController;

/**
 * Created by Stephan Petzl (stephan.petzl@gmail.com) on 11/21/15.
 */
public class LightsAnimator {

    private static final String TAG = "LightsAnimator";
    private final LightsController mCommands;
    private ValueAnimator mAnimator1;
    private int mZone;
    private int mPrevColor = Color.argb(255,0,255,255);
    private int mNextColor;

    public LightsAnimator(LightsController c, int zone) {
        mCommands = c;
        mZone = zone;
    }

    public void fadeToColor(int newColor) {
        mNextColor = newColor;
        final int animationLength = 3000;
        final int fps = 10;
        final int framesTotal = fps * animationLength / 1000;
        final int frameLength = animationLength / framesTotal;
        new Thread() {

            @Override
            public void run() {
                super.run();
                for (int i = 0; i <= framesTotal; i++) {
                    float progress = i * 1f / framesTotal;

                    int c = blendColors(mPrevColor, mNextColor, progress);
                    Log.v(TAG, "p: "+progress+" \tr:" + Color.red(c) + " g:" + Color.green(c) + " b:" + Color.blue(c));
                    mCommands.setColor(mZone, c);

                    try {
                        Thread.sleep(frameLength);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                mPrevColor = mNextColor;
            }

        }.start();
    }

    private static int blendColors(int color1, int color2, float ratio) {
        final float inverseRation = 1f - ratio;
        float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation);
        float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation);
        float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation);
        return Color.rgb((int) r, (int) g, (int) b);
    }
}
