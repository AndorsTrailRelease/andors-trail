package com.gpl.rpg.AndorsTrail.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Window;
import android.view.WindowManager;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.R;
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
}

