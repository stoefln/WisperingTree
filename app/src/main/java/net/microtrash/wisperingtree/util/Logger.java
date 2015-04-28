package net.microtrash.wisperingtree.util;


import android.util.Log;

public class Logger implements LoggerInterface {
    private static final String TAG = "Logger";
    private static LoggerInterface instance;

    public static LoggerInterface getInstance() {
        if(instance == null){
            instance = new Logger();
        }
        return instance;
    }

    @Override
    public void log(String message) {
        Log.v(TAG, message);
    }

    @Override
    public void log(String key, String value) {
        Log.v(TAG, key + ": " + value);
    }
}
