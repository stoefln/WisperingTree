package net.microtrash.wisperingtree.util;

import android.util.Log;

public class Profiler {

	private static final String TAG = "Profiler";
	private static long sLastMeasure;

	public static void start() {
		sLastMeasure = System.currentTimeMillis();
		Log.v(TAG, "profiler started");
	}
	
	public static void start(String _message) {
		sLastMeasure = System.currentTimeMillis();
		Log.v(TAG, "profiler started: " + _message);
	}

	public static void measure() {
		Log.v(TAG, "millisec since last measure: " + (System.currentTimeMillis() - sLastMeasure));
		sLastMeasure = System.currentTimeMillis();
	}

	public static void measure(String _tag) {
		Log.v(TAG, "millisec since last measure: " + (System.currentTimeMillis() - sLastMeasure) + " [" + _tag + "]");
		sLastMeasure = System.currentTimeMillis();
	}
}
