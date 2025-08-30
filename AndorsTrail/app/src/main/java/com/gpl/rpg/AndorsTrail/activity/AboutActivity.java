package com.gpl.rpg.AndorsTrail.activity;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.util.ThemeHelper;

public final class AboutActivity extends AndorsTrailBaseActivity implements ImageGetter {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(ThemeHelper.getBaseTheme());
		super.onCreate(savedInstanceState);

		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
		initializeView(this, R.layout.about, R.id.about_root);
		final Resources res = getResources();

		final TextView tv = (TextView) findViewById(R.id.about_contents);
		tv.setText(Html.fromHtml(res.getString(R.string.about_contents1)));

		Button b = (Button) findViewById(R.id.about_button1);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				tv.setText(Html.fromHtml(res.getString(R.string.about_contents1)));
			}
		});


		b = (Button) findViewById(R.id.about_button2);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				tv.setText(Html.fromHtml(res.getString(R.string.about_authors)));
			}
		});

		b = (Button) findViewById(R.id.about_button3);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				tv.setText(Html.fromHtml(res.getString(R.string.about_copyright) + res.getString(R.string.about_contents3)));
			}
		});

		b = (Button) findViewById(R.id.about_button4);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				tv.setText(Html.fromHtml(res.getString(R.string.about_interface), AboutActivity.this, null));
			}
		});

		tv.setMovementMethod(LinkMovementMethod.getInstance());

		TextView t = (TextView) findViewById(R.id.about_version);
		t.setText('v' + AndorsTrailApplication.CURRENT_VERSION_DISPLAY);
	}

	@Override
	public Drawable getDrawable(String s) {
		Resources res = getResources();
		Drawable d;
		if (s.equals("chest.png")) {
			Drawable r = res.getDrawable(R.drawable.ui_quickslots);
			r.setBounds(0, 0, r.getIntrinsicWidth(), r.getIntrinsicHeight());
			return r;
		}
		if (s.equals("char_hero.png")) {
			Drawable r = res.getDrawable(R.drawable.char_hero);
			r.setBounds(0, 0, r.getIntrinsicWidth(), r.getIntrinsicHeight()*4/5);
			return r;
		}
		if (s.equals("monster.png")) d = res.getDrawable(R.drawable.monsters_eye4);
		else if (s.equals("flee_example.png")) d = res.getDrawable(R.drawable.ui_flee_example);
		else if (s.equals("doubleattackexample.png")) d = res.getDrawable(R.drawable.ui_doubleattackexample);
		else return null;
		d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
		return d;
	}
}
