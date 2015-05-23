package net.microtrash.wisperingtree;

import net.microtrash.wisperingtree.bus.LogMessage;
import net.microtrash.wisperingtree.util.Utils;

import de.greenrobot.event.EventBus;

public class MainApplication extends com.activeandroid.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.mkDir(Utils.getAppRootDir());
        EventBus.getDefault().register(this);
    }


    public void onEvent(LogMessage message) {
        message.save();
    }

}
