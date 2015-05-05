package net.microtrash.wisperingtree;

import android.app.Application;

import net.microtrash.wisperingtree.util.Utils;

/**
 * Created by steph on 5/1/15.
 */
public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.mkDir(Utils.getAppRootDir());
    }
}
