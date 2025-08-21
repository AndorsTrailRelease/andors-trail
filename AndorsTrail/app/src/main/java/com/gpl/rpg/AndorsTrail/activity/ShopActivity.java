package com.gpl.rpg.AndorsTrail.activity;

import android.content.res.Resources;
import android.os.Bundle;
import androidx.fragment.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.activity.fragment.ShopActivity_Buy;
import com.gpl.rpg.AndorsTrail.activity.fragment.ShopActivity_Sell;
import com.gpl.rpg.AndorsTrail.util.ThemeHelper;

public final class ShopActivity extends AndorsTrailBaseFragmentActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(ThemeHelper.getBaseTheme());
		super.onCreate(savedInstanceState);

		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
		if (!app.isInitialized()) { finish(); return; }

		initializeView(this, R.layout.tabbedlayout, android.R.id.tabhost);

		final Resources res = getResources();

		FragmentTabHost tabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
		tabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);

		LayoutInflater inflater = getLayoutInflater();
		
		ViewGroup v;
		
		v = (ViewGroup) inflater.inflate(R.layout.tabindicator, null);
		((TextView)v.findViewById(R.id.tabindicator_text)).setText(res.getString(R.string.shop_buy));
		((ImageView)v.findViewById(R.id.tabindicator_icon)).setImageResource(R.drawable.ui_icon_equipment);
		tabHost.addTab(tabHost.newTabSpec("buy")
				.setIndicator(v)
				,ShopActivity_Buy.class, null);
		
		v = (ViewGroup) inflater.inflate(R.layout.tabindicator, null);
		((TextView)v.findViewById(R.id.tabindicator_text)).setText(res.getString(R.string.shop_sell));
		((ImageView)v.findViewById(R.id.tabindicator_icon)).setImageResource(R.drawable.ui_icon_coins);
		tabHost.addTab(tabHost.newTabSpec("sell")
				.setIndicator(v)
				,ShopActivity_Sell.class, null);
	}
}
