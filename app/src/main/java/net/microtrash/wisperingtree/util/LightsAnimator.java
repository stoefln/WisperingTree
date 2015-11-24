package net.microtrash.wisperingtree.util;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.util.Log;

import tv.piratemedia.lightcontroler.LightsController;

/**
 * Created by Stephan Petzl (stephan.petzl@gmail.com) on 11/21/15.
 */
public class LightsAnimator {
    final int FPS = 10;
    private static final String TAG = "LightsAnimator";
    private final LightsController mLightsController;
    private ValueAnimator mAnimator1;
    private int mZone;
    private int mPrevColor = 0;
    private int mNextColor;

    public LightsAnimator(LightsController c, int zone) {
        mLightsController = c;
        mZone = zone;
    }

    public void fadeToColor(int newColor) {
        mNextColor = newColor;
        final int animationLength = 1000;

        final int framesTotal = FPS * animationLength / 1000;
        final int frameLength = animationLength / framesTotal;
        Log.v(TAG, "Color fade from "+mPrevColor+ " to "+mNextColor);
        new Thread() {

            @Override
            public void run() {
                super.run();
                for (int i = 0; i <= framesTotal; i++) {
                    float progress = i * 1f / framesTotal;
                    int diff = mNextColor - mPrevColor;
                    int c = (int) (mPrevColor + diff * progress);
                    Log.v(TAG, "p: "+progress+" \t color:" + c);
                    mLightsController.setColorWithCircle2(mZone, c);

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

    public void setColorWithCircle(int zone, int colorDec) {

    }
}
