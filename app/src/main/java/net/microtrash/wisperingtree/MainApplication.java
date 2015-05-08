package net.microtrash.wisperingtree;

import net.microtrash.wisperingtree.util.Utils;

/**
 * Created by steph on 5/1/15.
 */
public class MainApplication extends com.activeandroid.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.mkDir(Utils.getAppRootDir());
    }
}
