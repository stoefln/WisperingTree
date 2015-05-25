package net.microtrash.wisperingtree.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import net.microtrash.wisperingtree.audio.Sample;
import net.microtrash.wisperingtree.audio.SamplePlayer;
import net.microtrash.wisperingtree.bus.SamplePlayerSpeedChangeEvent;
import net.microtrash.wisperingtree.util.Logger;
import net.microtrash.wisperingtree.util.LoggerInterface;
import net.microtrash.wisperingtree.util.Utils;

import java.io.File;

import de.greenrobot.event.EventBus;

public class PlayService extends Service {

    private LoggerInterface mLogger;
    private SamplePlayer mSamplePlayer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mLogger = Logger.getInstance();

        EventBus.getDefault().register(this);

        init();

    }

    private void init() {
        File rootDir = new File(Utils.getAppRootDir());
        mSamplePlayer = new SamplePlayer(getBaseContext());
        for (File file : rootDir.listFiles()) {
            Sample s = new Sample(file);
            mSamplePlayer.addSample(s);
        }
        mSamplePlayer.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSamplePlayer.stop();
        EventBus.getDefault().unregister(this);
    }


    public void onEvent(SamplePlayerSpeedChangeEvent event) {
        if (mSamplePlayer != null) {
            mSamplePlayer.setSpeed(event.getSpeed());
        }
    }


}
