package net.microtrash.wisperingtree.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import net.microtrash.wisperingtree.bus.FileSendingToClient;
import net.microtrash.wisperingtree.bus.FileSentToClient;
import net.microtrash.wisperingtree.bus.SamplingStart;
import net.microtrash.wisperingtree.bus.SamplingStop;
import net.microtrash.wisperingtree.bus.ServerConnectionFail;
import net.microtrash.wisperingtree.util.LightsAnimator;
import net.microtrash.wisperingtree.util.Logger;
import net.microtrash.wisperingtree.util.LoggerInterface;
import net.microtrash.wisperingtree.util.Static;

import de.greenrobot.event.EventBus;
import tv.piratemedia.lightcontroler.LightsController;

public class LightControlService extends Service {


    private static final int COLOR_RANDOMNESS = 20;
    private static final int COLOR_REC = 90;
    private LoggerInterface mLogger;
    private LightsController mLightsController;
    private LightsAnimator mAnimator;


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
        mAnimator = new LightsAnimator(mLightsController, 4);
        //mAnimator.fadeToColor(Color.argb(255, 0, 255, 255));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLightsController.killUDPC();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(SamplingStart event){
        mLogger.log("Lights: Sampling start");
        mAnimator.fadeToColor(COLOR_REC);
        //mLightsController.setColorWithCircle2(4, 0);

    }

    public void onEventMainThread(SamplingStop event){
        mLogger.log("Lights: Sampling stop");
        //mLightsController.setColorWithCircle2(4, COLOR_REC);
        mAnimator.fadeToColor(0);
    }

    public void onEventMainThread(FileSendingToClient event){
        int lightBulbIndex = Static.getLightBulbIndexByMac(event.getReceiverMac());
        mLogger.log("Lights: FileSendingToClient: "+lightBulbIndex);
        mLightsController.setColorWithCircle2(lightBulbIndex, 0);
    }

    public void onEventMainThread(ServerConnectionFail event){
        switchOff(event.mClientAdressConnectionFail);
    }

    public void onEventMainThread(FileSentToClient event){
        switchOff(event.getReceiverMac());
    }

    private void switchOff(String mac) {
        int lightBulbIndex = Static.getLightBulbIndexByMac(mac);
        mLogger.log("Lights: FileSentToClient: " + lightBulbIndex);
        if(lightBulbIndex != 0) {
            mLightsController.setColorWithCircle2(lightBulbIndex, COLOR_REC + (int) (Math.random() * COLOR_RANDOMNESS) - COLOR_RANDOMNESS / 2);
        }
    }
}
