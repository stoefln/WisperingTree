package net.microtrash.wisperingtree.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;

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

    public static void putPreference(Context context, String key, Serializable value) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        try {
            String serialized = Tools.serialize(value);
            editor.putString(key, serialized);
            editor.commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Object getPreferenceSerializable(Context context, String key) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String serialized = sharedPrefs.getString(key, null);
        if (serialized == null || serialized.equals("")) {
            return null;
        }
        try {
            return Tools.deserialize(serialized);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Read the object from Base64 string. */
    public static Object deserialize(String s) throws IOException, ClassNotFoundException {
        if (s.equals("")) {
            return null;
        }
        byte[] data = Base64.decodeFast(s.getBytes());
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        Object o = ois.readObject();
        ois.close();
        return o;
    }

    /** Write the object to a Base64 string. */
    public static String serialize(Serializable o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        return Base64.encodeToString(baos.toByteArray(), false);
    }

    public static String stacktraceToString(Throwable ex) {
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        String s = writer.toString();
        return s;
    }

}
