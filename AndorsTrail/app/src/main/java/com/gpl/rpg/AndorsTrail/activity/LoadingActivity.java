package com.gpl.rpg.AndorsTrail.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.WorldSetup;
import com.gpl.rpg.AndorsTrail.WorldSetup.OnResourcesLoadedListener;
import com.gpl.rpg.AndorsTrail.WorldSetup.OnSceneLoadedListener;
import com.gpl.rpg.AndorsTrail.savegames.Savegames;
import com.gpl.rpg.AndorsTrail.util.ThemeHelper;
import com.gpl.rpg.AndorsTrail.view.CloudsAnimatorView;
import com.gpl.rpg.AndorsTrail.view.CustomDialogFactory;
import com.gpl.rpg.AndorsTrail.view.CustomDialogFactory.CustomDialog;

public final class LoadingActivity extends AndorsTrailBaseActivity implements OnResourcesLoadedListener, OnSceneLoadedListener {

	private WorldSetup setup;
	private CustomDialog progressDialog;
	private CloudsAnimatorView clouds_back, clouds_mid, clouds_front;
	boolean loaded = false;

	private Object semaphore = new Object();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(ThemeHelper.getBaseTheme());
		super.onCreate(savedInstanceState);
		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
		initializeView(this, R.layout.startscreen, R.id.startscreen_fragment_container);

		TextView tv = (TextView) findViewById(R.id.startscreen_version);
		tv.setVisibility(View.GONE);
		
		clouds_back = (CloudsAnimatorView) findViewById(R.id.ts_clouds_animator_back);
		if (clouds_back != null) clouds_back.setCloudsCountAndLayer(40, CloudsAnimatorView.Layer.below);
		clouds_mid = (CloudsAnimatorView) findViewById(R.id.ts_clouds_animator_mid);
		if (clouds_mid != null) clouds_mid.setCloudsCountAndLayer(15, CloudsAnimatorView.Layer.center);
		clouds_front = (CloudsAnimatorView) findViewById(R.id.ts_clouds_animator_front);
		if (clouds_front != null) clouds_front.setCloudsCountAndLayer(8, CloudsAnimatorView.Layer.above);
		
		this.setup = app.getWorldSetup();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			((AnimationDrawable)((ImageView)findViewById(R.id.title_logo)).getDrawable()).start();
			ImageView iv = (ImageView) findViewById(R.id.ts_foreground);
			int ivWidth = iv.getWidth();
			int drawableWidth = iv.getDrawable().getIntrinsicWidth();
			float ratio = ((float)ivWidth) / ((float)drawableWidth);
			
			if (clouds_back != null) {
				clouds_back.setScalingRatio(ratio);
			}
			if (clouds_mid != null) {
				clouds_mid.setScalingRatio(ratio);
			}
			if (clouds_front != null) {
				clouds_front.setScalingRatio(ratio);
			}
			
			if (progressDialog == null) {
				progressDialog = CustomDialogFactory.createDialog(this, getResources().getString(R.string.dialog_loading_message), 
						getResources().getDrawable(R.drawable.loading_anim), null, null, false, false);
				synchronized (semaphore) {
					if (!loaded) {
						progressDialog.setOwnerActivity(this);
						CustomDialogFactory.show(progressDialog);
					}
				}
			}
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		setup.setOnResourcesLoadedListener(this);
		

		final ImageView iv = (ImageView) findViewById(R.id.ts_foreground);
		iv.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
			
			@Override
			public boolean onPreDraw() {
				float[] point = new float[]{0f,0.25f * iv.getDrawable().getIntrinsicHeight()};
				iv.getImageMatrix().mapPoints(point);
				int imgY = (int) (iv.getTop() + point[1]);

				if (clouds_back != null) {
					clouds_back.setYMax(imgY);
				}
				if (clouds_mid != null) {
					clouds_mid.setYMax(imgY);
				}
				if (clouds_front != null) {
					clouds_front.setYMax(imgY);
				}
				iv.getViewTreeObserver().removeOnPreDrawListener(this);
				return true;
			}
		});
		
		
		if (clouds_back != null)clouds_back.resumeAnimation();
		if (clouds_mid != null)clouds_mid.resumeAnimation();
		if (clouds_front != null)clouds_front.resumeAnimation();
	}

	@Override
	public void onPause() {
		super.onPause();
		setup.setOnResourcesLoadedListener(null);
		setup.removeOnSceneLoadedListener(this);
		if (clouds_back != null)clouds_back.pauseAnimation();
		if (clouds_mid != null)clouds_mid.pauseAnimation();
		if (clouds_front != null)clouds_front.pauseAnimation();
	}
	
	
	@Override
	public void onResourcesLoaded() {
		loaded = false;
		setup.startCharacterSetup(this);
	}

	
	
	@Override
	public void onSceneLoaded() {
		synchronized (semaphore) {
			if (progressDialog != null) progressDialog.dismiss();
			loaded =true;
		}
		startActivity(new Intent(this, MainActivity.class));
		this.finish();
	}

	@Override
	public void onSceneLoadFailed(Savegames.LoadSavegameResult loadResult) {
		synchronized (semaphore) {
			if (progressDialog != null) progressDialog.dismiss();
			loaded =true;
		}
		if (loadResult == Savegames.LoadSavegameResult.savegameIsFromAFutureVersion) {
			showLoadingFailedDialog(R.string.dialog_loading_failed_incorrectversion);
		} else if (loadResult == Savegames.LoadSavegameResult.cheatingDetected) {
			showLoadingFailedDialog(R.string.dialog_loading_failed_cheat);
		} else {
			showLoadingFailedDialog(R.string.dialog_loading_failed_message);
		}
	}

	private void showLoadingFailedDialog(int messageResourceID) {
		final CustomDialog d = CustomDialogFactory.createDialog(this, getResources().getString(R.string.dialog_loading_failed_title), null, getResources().getString(messageResourceID), null, true);
		CustomDialogFactory.addDismissButton(d, android.R.string.ok);
		CustomDialogFactory.setDismissListener(d, new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				LoadingActivity.this.finish();
			}
		});
		CustomDialogFactory.show(d);
		
	}
}
