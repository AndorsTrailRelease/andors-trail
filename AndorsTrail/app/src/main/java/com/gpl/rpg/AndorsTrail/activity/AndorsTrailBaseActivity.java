package com.gpl.rpg.AndorsTrail.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;

public abstract class AndorsTrailBaseActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
        app.setLocale(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
        app.setLocale(this);
    }
    protected void initializeView(Activity activity, @LayoutRes int layoutId, @IdRes int rootViewId) {
        AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(activity);
        app.setWindowParameters(activity);
        activity.setContentView(layoutId);
        View root = activity.findViewById(rootViewId);
        app.setUsablePadding(root);
        app.setFullscreenMode(activity);
    }
}

