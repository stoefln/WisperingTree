package net.microtrash.wisperingtree.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by steph on 4/19/15.
 */
public class Tools {

    public static String getPreference(Context context, String key) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPrefs.getString(key, null);
    }


    public static int getPreferenceInt(Context context, String key) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPrefs.getInt(key, 0);
    }

    public static void putPreference(Context context, String key, boolean value) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static void putPreference(Context context, String key, int value) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt(key, value);
        editor.commit();
    }
}
