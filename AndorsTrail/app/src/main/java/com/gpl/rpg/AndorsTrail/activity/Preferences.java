package com.gpl.rpg.AndorsTrail.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.BuildConfig;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.util.L;
import com.gpl.rpg.AndorsTrail.util.ThemeHelper;

public final class Preferences extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(ThemeHelper.getBaseTheme());
		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
		app.setWindowParameters(this);
		super.onCreate(savedInstanceState);
		app.setFullscreenMode(this);


		app.setLocale(this);
		addPreferencesFromResource(R.xml.preferences);


	}

	@Override
	protected void onResume() {
		super.onResume();
		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
		app.setLocale(this);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (ActivityKeyHandler.handleBackMappedKey(this, event)) return true;
		return super.dispatchKeyEvent(event);
	}

	@Override
	public boolean dispatchGenericMotionEvent(MotionEvent event) {
		if (ActivityKeyHandler.handleBackMappedMouseButton(this, event)) return true;
		return super.dispatchGenericMotionEvent(event);
	}
}
