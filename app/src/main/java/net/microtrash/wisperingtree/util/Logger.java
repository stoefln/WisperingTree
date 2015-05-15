package net.microtrash.wisperingtree.util;


import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import net.microtrash.wisperingtree.bus.LogMessage;

import de.greenrobot.event.EventBus;

public class Logger implements LoggerInterface {
    private static final String TAG = "Logger";
    private static LoggerInterface instance;
    private LoggerInterface mLogListener;

    public static LoggerInterface getInstance() {
        if(instance == null){
            instance = new Logger();
            instance.setOnLogListener(new LoggerInterface() {

                Handler mHandler = new Handler(Looper.getMainLooper());

                @Override
                public void log(final String message) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            EventBus.getDefault().post(new LogMessage(message));
                        }
                    });
                }

                @Override
                public void log(String key, String value) {
                    EventBus.getDefault().post(new LogMessage(key + ": " + value));
                }

                @Override
                public void setOnLogListener(LoggerInterface logListener) {

                }
            });
        }
        return instance;
    }

    @Override
    public void log(String message) {
        Log.v(TAG, message);
        if (mLogListener != null) {
            mLogListener.log(message);
        }
    }

    @Override
    public void log(String key, String value) {
        log(key + ": " + value);
    }

    @Override
    public void setOnLogListener(LoggerInterface logListener) {
        mLogListener = logListener;
    }
}
