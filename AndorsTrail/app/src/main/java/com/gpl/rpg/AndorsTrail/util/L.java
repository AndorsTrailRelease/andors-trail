package com.gpl.rpg.AndorsTrail.util;

import android.util.Log;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;

public final class L {
	private static final String TAG = "AndorsTrail";

	public static void debug(String s) {
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) {
			Log.d(TAG, s);
		}
	}

	public static void info(String s) {
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) {
			Log.i(TAG, s);
		}
	}

	public static void warn(String s) {
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) {
			Log.w(TAG, s);
		}
	}

	public static void error(String s) {
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) {
			Log.e(TAG, s);
		}
	}

	public static void error(String s, Throwable t) {
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) {
			Log.e(TAG, s, t);
		}
	}

	public static void log(String s) {
		warn(s);
	}

}
