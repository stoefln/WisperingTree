package net.microtrash.wisperingtree.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import net.microtrash.wisperingtree.audio.Sample;
import net.microtrash.wisperingtree.audio.SamplePlayer;
import net.microtrash.wisperingtree.audio.SequencialSamplePlayer;
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
        mSamplePlayer = new SequencialSamplePlayer(getBaseContext());
        //mSamplePlayer = new MergeSamplePlayer(getBaseContext());
        initSamples();
        mSamplePlayer.start();
    }

    private void initSamples() {
        try {
            File rootDir = new File(Utils.getAppRootDir());
            for (File file : rootDir.listFiles()) {
                Sample s = new Sample(file);
                if (!mSamplePlayer.hasFile(file)) {
                    mSamplePlayer.addSample(s);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mSamplePlayer != null) {
                    // observe files
                    initSamples();
                }
            }
        }, 2000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSamplePlayer.stop();
        mSamplePlayer = null;
        EventBus.getDefault().unregister(this);
    }


    public void onEvent(SamplePlayerSpeedChangeEvent event) {
        if (mSamplePlayer != null) {
            mSamplePlayer.setSpeed(event.getSpeed());
        }
    }


}
