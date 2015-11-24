package net.microtrash.wisperingtree.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;

import net.microtrash.wisperingtree.bus.SamplingStart;
import net.microtrash.wisperingtree.bus.SamplingStop;
import net.microtrash.wisperingtree.util.LightsAnimator;
import net.microtrash.wisperingtree.util.Logger;
import net.microtrash.wisperingtree.util.LoggerInterface;

import de.greenrobot.event.EventBus;
import tv.piratemedia.lightcontroler.LightsController;

public class LightControlService extends Service {


    private LoggerInterface mLogger;
    private LightsController mLightsController;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLogger = Logger.getInstance();
        EventBus.getDefault().register(this);

        Context ctx = getBaseContext();
        mLightsController = new LightsController(ctx, null);
        //mLightsController.lightsOn(1);
        //mLightsController.setColor(1, Color.argb(255, 0, 255, 0));
        //LightsAnimator la = new LightsAnimator(mLightsController, 2);
        //la.fadeToColor(Color.argb(255, 0, 255, 255));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLightsController.killUDPC();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(SamplingStart event){
        LightsAnimator la2 = new LightsAnimator(mLightsController, 2);
        la2.fadeToColor(Color.argb(255, 255, 255, 0));

    }

    public void onEventMainThread(SamplingStop event){
        LightsAnimator la2 = new LightsAnimator(mLightsController, 2);
        la2.fadeToColor(Color.argb(255, 0, 255, 255));

    }

}
