package net.microtrash.wisperingtree.util;

import java.util.Hashtable;

/**
 * Created by steph on 4/19/15.
 */
public class Static {
    public static final String FOLDER_NAME = "WisperingTree";
    public static final String KEY_LAST_REC_NUM = "last rec number";
    public static final String SERVER_MAC = "E4:12:1D:B4:DF:C5";
    public static final int MAX_FILES = 20;
    public static final String KEY_MIN_NOISE_VALUE = "min_noise_value";
    public static final String KEY_MAX_NOISE_VALUE = "max_noise_value";
    private static Hashtable<String, String> mClients;

    public static Hashtable<String, String> getClients() {
        if (mClients == null) {
            mClients = new Hashtable<>();
            mClients.put("40:B0:FA:F4:EC:B9", "LG-E430");
            //mClients.put("HTC-ONE", "98:0D:2E:C0:30:86");
            //mClients.put("D8:90:E8:FB:D8:C2", "S4");
            //mClients.put("94:D7:71:E3:E6:61", "S3");
            mClients.put("00:73:E0:14:ED:87", "Young White");
            mClients.put("B4:CE:F6:77:27:B0", "HTC OPCV1");
            mClients.put("AC:36:13:D9:C8:1E", "Galaxy S3 Mini");
        }
        return mClients;
    }

}
