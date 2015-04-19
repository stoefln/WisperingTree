package net.microtrash.wisperingtree.util;

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
}
