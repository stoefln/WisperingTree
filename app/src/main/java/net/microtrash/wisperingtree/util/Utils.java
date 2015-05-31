package net.microtrash.wisperingtree.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Created by steph on 4/19/15.
 */
public class Utils {


    public static void mkDir(String dirpath) {
        File dir = new File(dirpath);
        if(!dir.exists()){
            dir.mkdir();
        }
    }

    public static String getAppRootDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Static.FOLDER_NAME;
    }

    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        if(context == null){
            return false;
        }
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
