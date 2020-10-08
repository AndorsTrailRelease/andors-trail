package com.gpl.rpg.AndorsTrail.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.AndorsTrailPreferences;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.InputController;

public final class VirtualDpadView extends ImageView implements OnClickListener {
	private final WorldContext world;
	private final InputController inputController;

	private int one_third_width;
	private int two_thirds_width;
	private int full_width;
	private int one_third_height;
	private int two_thirds_height;
	private int full_height;
	private boolean isMinimized = false;
	private int lastTouchPosition_dx;
	private int lastTouchPosition_dy;
	private boolean isMinimizeable = true;

	public VirtualDpadView(final Context context, AttributeSet attr) {
		super(context, attr);
		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivityContext(context);
		this.world = app.getWorld();
		final ControllerContext controllers = app.getControllerContext();
		this.inputController = controllers.inputController;

		setImageResource(R.drawable.ui_dpad);
		setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		setFocusable(false);
		setOnClickListener(this);

		setAdjustViewBounds(true);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (!isMinimized) {
			one_third_width = w / 3;
			two_thirds_width = w * 2 / 3;
			full_width = w;
			one_third_height = h / 3;
			two_thirds_height = h * 2 / 3;
			full_height = h;
		}

		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (isMinimized) return super.onTouchEvent(event);

		if (!world.model.uiSelections.isMainActivityVisible) return true;

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:

			final int x = (int)event.getX();
			lastTouchPosition_dx = 0;
			if (x < one_third_width) lastTouchPosition_dx = -1;
			else if (x >= two_thirds_width) lastTouchPosition_dx = 1;

			final int y = (int)event.getY();
			lastTouchPosition_dy = 0;
			if (y < one_third_height) lastTouchPosition_dy = -1;
			else if (y >= two_thirds_height) lastTouchPosition_dy = 1;

			// Minimize the dpad if enabled in options - otherwise attack or move(0, 0)
			if (isMinimizeable && lastTouchPosition_dx == 0 && lastTouchPosition_dy == 0) break;

			this.inputController.onRelativeMovement(lastTouchPosition_dx, lastTouchPosition_dy);
			return true;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_OUTSIDE:
			this.inputController.onKeyboardCancel();
			break;
		}
		return super.onTouchEvent(event);
	}

	public void updateVisibility(AndorsTrailPreferences preferences) {
		int dpadPosition = preferences.dpadPosition;
		if (dpadPosition == AndorsTrailPreferences.DPAD_POSITION_DISABLED) {
			setVisibility(View.GONE);
			this.inputController.setDpadActive(false);
			return;
		}

		setVisibility(View.VISIBLE);
		this.inputController.setDpadActive(!isMinimized);
		isMinimizeable = preferences.dpadMinimizeable;

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		switch (dpadPosition) {
			case AndorsTrailPreferences.DPAD_POSITION_LOWER_RIGHT:
				params.addRule(RelativeLayout.ALIGN_RIGHT, R.id.main_mainview);
				params.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.main_mainview);
				break;
			case AndorsTrailPreferences.DPAD_POSITION_LOWER_LEFT:
				params.addRule(RelativeLayout.ALIGN_LEFT, R.id.main_mainview);
				params.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.main_mainview);
				break;
			case AndorsTrailPreferences.DPAD_POSITION_LOWER_CENTER:
				params.addRule(RelativeLayout.CENTER_HORIZONTAL);
				params.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.main_mainview);
				break;
			case AndorsTrailPreferences.DPAD_POSITION_UPPER_RIGHT:
				params.addRule(RelativeLayout.ALIGN_RIGHT, R.id.main_mainview);
				params.addRule(RelativeLayout.ALIGN_TOP, R.id.main_mainview);
				break;
			case AndorsTrailPreferences.DPAD_POSITION_UPPER_LEFT:
				params.addRule(RelativeLayout.ALIGN_LEFT, R.id.main_mainview);
				params.addRule(RelativeLayout.ALIGN_TOP, R.id.main_mainview);
				break;
			case AndorsTrailPreferences.DPAD_POSITION_UPPER_CENTER:
				params.addRule(RelativeLayout.CENTER_HORIZONTAL);
				params.addRule(RelativeLayout.ALIGN_TOP, R.id.main_mainview);
				break;
			case AndorsTrailPreferences.DPAD_POSITION_CENTER_LEFT:
				params.addRule(RelativeLayout.ALIGN_LEFT, R.id.main_mainview);
				params.addRule(RelativeLayout.CENTER_VERTICAL);
				break;
			case AndorsTrailPreferences.DPAD_POSITION_CENTER_RIGHT:
				params.addRule(RelativeLayout.ALIGN_RIGHT, R.id.main_mainview);
				params.addRule(RelativeLayout.CENTER_VERTICAL);
				break;
		}

		setLayoutParams(params);
	}

	@Override
	public void onClick(View arg0) {
		if (isMinimized) {
			isMinimized = false;
			setMaxWidth(full_width);
			setMaxHeight(full_height);
		} else {
			if (lastTouchPosition_dx != 0 || lastTouchPosition_dy != 0) return;
			if (!isMinimizeable) return;
			isMinimized = true;
			setMaxWidth(one_third_width);
			setMaxHeight(one_third_height);
		}
		this.inputController.setDpadActive(!isMinimized);
		this.requestLayout();
	}
}
