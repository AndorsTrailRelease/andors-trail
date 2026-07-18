package com.gpl.rpg.AndorsTrail.activity;

import android.os.Bundle;

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

		setupTabHost(R.layout.tabbedlayout, R.id.realtabcontent);

		addTab("buy",  R.string.shop_buy,  R.drawable.ui_icon_equipment, ShopActivity_Buy.class);
		addTab("sell", R.string.shop_sell, R.drawable.ui_icon_coins,     ShopActivity_Sell.class);
	}
}
