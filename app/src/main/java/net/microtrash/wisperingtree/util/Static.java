package net.microtrash.wisperingtree.util;

import java.util.Hashtable;

/**
 * Created by steph on 4/19/15.
 */
public class Static {
    public static final String FOLDER_NAME = "WisperingTree";
    public static final String KEY_LAST_REC_NUM = "last rec number";

    public static final int MAX_FILES = 20;
    public static final String KEY_MIN_NOISE_VALUE = "min_noise_value";
    public static final String KEY_MAX_NOISE_VALUE = "max_noise_value";
    public static final String BACKUP_DIR_NAME = "backup";
    public static final String KEY_FILES_TRANSFERRED = "files_transferred";
    private static Hashtable<String, String> mClients;

    public static final String SERVER_MAC = "B4:CE:F6:77:27:B0";

    public static Hashtable<String, String> getClients() {
        if (mClients == null) {
            mClients = new Hashtable<>();
            mClients.put("40:B0:FA:F4:EC:B9", "LG-E430");
            //mClients.put("70:F9:27:D4:EC:1E", "S3");
            mClients.put("78:F7:BE:5E:60:54", "S4");
            mClients.put("44:80:EB:1C:20:7E", "Moto G");
            mClients.put("00:73:E0:14:ED:87", "Y White");
            //mClients.put("4C:3C:16:16:4A:6A", "Y Black");
            //mClients.put("B4:CE:F6:77:27:B0", "HTC OPCV1");
            mClients.put("AC:36:13:D9:C8:1E", "S3 Mini");
            mClients.put("E4:12:1D:B4:DF:C5", "S5");
        }
        return mClients;
    }

}
