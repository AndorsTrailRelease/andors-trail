package com.gpl.rpg.AndorsTrail.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.util.Range;

public final class RangeBar extends RelativeLayout {
	private final ProgressBar progressBar;
	private final TextView progressBarText;
	private final TextView labelText;

	public RangeBar(Context context, AttributeSet attr) {
		super(context, attr);
		setFocusable(false);
		inflate(context, R.layout.rangebar, this);

		progressBarText = (TextView) findViewById(R.id.rangebar_text);
		progressBar = (ProgressBar) findViewById(R.id.rangebar_progress);
		labelText = (TextView) findViewById(R.id.rangebar_label);
	}

	public void init(int drawableID, int labelTextID) {
		// Wow, you actually need to call this twice (!), or the progressbar won't show the progress image, just the background.
		// TODO: investigate strangeness of setProgressDrawable
		progressBar.setProgressDrawable(getResources().getDrawable(drawableID));
		progressBar.setProgressDrawable(getResources().getDrawable(drawableID));
		labelText.setText(labelTextID);
		labelText.setSelected(true);
	}
	
	public void setLargeLabel(boolean large) {
		if (large) {
			labelText.getLayoutParams().width = getResources().getDimensionPixelSize(R.dimen.rangebar_labelwidth_large);
		} else {
			labelText.getLayoutParams().width = getResources().getDimensionPixelSize(R.dimen.rangebar_labelwidth_small);
		}
		this.postInvalidate();
	}

	public void update(final Range range) { update(range.max, range.current); }
	public void update(final int max, final int current) {
		progressBar.setProgress(0); // http://stackoverflow.com/questions/4348032/android-progressbar-does-not-update-progress-view-drawable

		progressBar.setMax(max);
		progressBar.setProgress(Math.min(current, max));
		progressBarText.setText(getResources().getString(R.string.rangebar_current_max, current, max));
		invalidate();
	}
}
