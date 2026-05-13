package com.gpl.rpg.AndorsTrail.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTabHost;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.R;

public abstract class AndorsTrailBaseFragmentActivity extends FragmentActivity {

    protected FragmentTabHost tabHost;

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

    /*
      Common routines for setting up a tabs (used by HeroinfoActivity and
      ShopActivity). The only other class that uses this base is StartScreen, so it doesn't
      really need to go to an intermediate base class.
     */

    // Inflate the tabbed layout, find the tab host, and wire it to the fragment manager.
    protected void setupTabHost(@LayoutRes int layoutId, @IdRes int contentId) {
        initializeView(this, layoutId, android.R.id.tabhost);
        tabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        tabHost.setup(this, getSupportFragmentManager(), contentId);
    }

    // Inflate one tab indicator, attach it to the tab host, and wire select-on-focus.
    protected void addTab(String tag, int textResId, int iconResId, Class<? extends Fragment> fragmentClass) {
        ViewGroup v = (ViewGroup) getLayoutInflater().inflate(R.layout.tabindicator, null);
        ((TextView)  v.findViewById(R.id.tabindicator_text)).setText(getString(textResId));
        ((ImageView) v.findViewById(R.id.tabindicator_icon)).setImageDrawable(getResources().getDrawable(iconResId));

        // Select this tab as soon as its indicator receives d-pad / keyboard focus and manage
        // focus on the other tabs so that up-navigation from the content will be forced to this tab.
        v.setOnFocusChangeListener(this::onFocusChangeListener);

        tabHost.addTab(tabHost.newTabSpec(tag).setIndicator(v), fragmentClass, null);
    }

    private void onFocusChangeListener(View view, boolean hasFocus) {
        int thisTabIndex = tabHost.getTabWidget().indexOfChild(view);

        if (getSupportFragmentManager().isStateSaved()) return; // Don't change focus during activity shutdown

        if (hasFocus) {
            // We got focus, so just enable focus on all tabs and select this one.
            for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {
                tabHost.getTabWidget().getChildAt(i).setFocusable(true);
            }

            tabHost.setCurrentTab(thisTabIndex);

        } else {
            // Lost focus - if it went to another tab header, don't do anything (the focus event
            // for that tab will make all tabs focusable so they can be selected), but if
            // the focus went to content below, we want to disable focus on all other tabs so that
            // up-navigation from the content will be forced to this tab.
            boolean anyTabHasFocus = false;
            for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {
                if (tabHost.getTabWidget().getChildAt(i).hasFocus()) {
                    anyTabHasFocus = true;
                    break;
                }
            }
            if (!anyTabHasFocus) {
                for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {
                    tabHost.getTabWidget().getChildAt(i).setFocusable(i == thisTabIndex);
                }
            }
        }
    }
}
