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
    private static final int COLOR_SLEEPING = 90;
    private static final int COLOR_SENDING = 0;
    private static final int COLOR_RECORDING = 150;
    private static final int COLOR_ERROR = 200;
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
        mLightsController.setColorWithCircle2(1, COLOR_SLEEPING);
        mLightsController.setColorWithCircle2(2, COLOR_SLEEPING);
        mLightsController.setColorWithCircle2(3, COLOR_SLEEPING);
        mLightsController.setColorWithCircle2(4, COLOR_SLEEPING);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLightsController.killUDPC();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(SamplingStart event){
        mLogger.log("Lights: Sampling start");
        mAnimator.fadeToColor(COLOR_RECORDING);
        //mLightsController.setColorWithCircle2(4, 0);

    }

    public void onEventMainThread(SamplingStop event){
        mLogger.log("Lights: Sampling stop");
        //mLightsController.setColorWithCircle2(4, COLOR_SLEEPING);
        mAnimator.fadeToColor(COLOR_SENDING);
    }

    public void onEventMainThread(FileSendingToClient event){
        int lightBulbIndex = Static.getLightBulbIndexByMac(event.getReceiverMac());
        mLogger.log("Lights: FileSendingToClient: "+lightBulbIndex);
        mLightsController.setColorWithCircle2(lightBulbIndex, COLOR_SENDING);
    }

    public void onEventMainThread(ServerConnectionFail event){
        int lightBulbIndex = Static.getLightBulbIndexByMac(event.getMac());
        mLightsController.setColorWithCircle2(lightBulbIndex, COLOR_ERROR);
        switchOff(event.mMac);
    }

    public void onEventMainThread(FileSentToClient event){
        switchOff(event.getReceiverMac());
    }

    private void switchOff(String mac) {
        int lightBulbIndex = Static.getLightBulbIndexByMac(mac);
        mLogger.log("Lights: FileSentToClient: " + lightBulbIndex);
        if(lightBulbIndex != 0) {
            mLightsController.setColorWithCircle2(lightBulbIndex, COLOR_SLEEPING + (int) (Math.random() * COLOR_RANDOMNESS) - COLOR_RANDOMNESS / 2);
        }
    }
}
