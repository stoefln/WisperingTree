package net.microtrash.wisperingtree.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import net.microtrash.wisperingtree.bus.SamplingStart;
import net.microtrash.wisperingtree.bus.SamplingStop;
import net.microtrash.wisperingtree.util.Logger;
import net.microtrash.wisperingtree.util.LoggerInterface;

import de.greenrobot.event.EventBus;

public class LightControlService extends Service {


    private LoggerInterface mLogger;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mLogger = Logger.getInstance();

        EventBus.getDefault().register(this);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(SamplingStart event){

    }

    public void onEventMainThread(SamplingStop event){
        
    }

}
