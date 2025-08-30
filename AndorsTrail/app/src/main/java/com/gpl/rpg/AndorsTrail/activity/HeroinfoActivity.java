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
import com.gpl.rpg.AndorsTrail.activity.fragment.HeroinfoActivity_Inventory;
import com.gpl.rpg.AndorsTrail.activity.fragment.HeroinfoActivity_Quests;
import com.gpl.rpg.AndorsTrail.activity.fragment.HeroinfoActivity_Skills;
import com.gpl.rpg.AndorsTrail.activity.fragment.HeroinfoActivity_Stats;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.util.ThemeHelper;

public final class HeroinfoActivity extends AndorsTrailBaseFragmentActivity {
	private WorldContext world;

	private FragmentTabHost tabHost;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(ThemeHelper.getBaseTheme());
		super.onCreate(savedInstanceState);
		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
		if (!app.isInitialized()) { finish(); return; }
		this.world = app.getWorld();
		initializeView(this, R.layout.tabbedlayout, android.R.id.tabhost);

		Resources res = getResources();

		tabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
		tabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);

		LayoutInflater inflater = getLayoutInflater();
		
		ViewGroup v;
		
		v = (ViewGroup) inflater.inflate(R.layout.tabindicator, null);
		((TextView)v.findViewById(R.id.tabindicator_text)).setText(res.getString(R.string.heroinfo_char));
		((ImageView)v.findViewById(R.id.tabindicator_icon)).setImageDrawable(res.getDrawable(R.drawable.char_hero));
		tabHost.addTab(tabHost.newTabSpec("char")
				.setIndicator(v)
				,HeroinfoActivity_Stats.class, null);
		
		v = (ViewGroup) inflater.inflate(R.layout.tabindicator, null);
		((TextView)v.findViewById(R.id.tabindicator_text)).setText(res.getString(R.string.heroinfo_quests));
		((ImageView)v.findViewById(R.id.tabindicator_icon)).setImageDrawable(res.getDrawable(R.drawable.ui_icon_quest));
		tabHost.addTab(tabHost.newTabSpec("quests")
				.setIndicator(v)
				,HeroinfoActivity_Quests.class, null);
		
		v = (ViewGroup) inflater.inflate(R.layout.tabindicator, null);
		((TextView)v.findViewById(R.id.tabindicator_text)).setText(res.getString(R.string.heroinfo_skill));
		((ImageView)v.findViewById(R.id.tabindicator_icon)).setImageDrawable(res.getDrawable(R.drawable.ui_icon_skill));
		tabHost.addTab(tabHost.newTabSpec("skills")
				.setIndicator(v)
				,HeroinfoActivity_Skills.class, null);
		
		v = (ViewGroup) inflater.inflate(R.layout.tabindicator, null);
		((TextView)v.findViewById(R.id.tabindicator_text)).setText(res.getString(R.string.heroinfo_inv));
		((ImageView)v.findViewById(R.id.tabindicator_icon)).setImageDrawable(res.getDrawable(R.drawable.ui_icon_equipment));
		tabHost.addTab(tabHost.newTabSpec("inv")
				.setIndicator(v)
				,HeroinfoActivity_Inventory.class, null);
		String t = world.model.uiSelections.selectedTabHeroInfo;
		if (t != null && t.length() > 0) {
			tabHost.setCurrentTabByTag(t);
		}
		updateIconForPlayer();

	}
	
	@Override
	protected void onResume() {
		super.onResume();
		updateIconForPlayer();
	}
	
	private void updateIconForPlayer() {
		ImageView iv = (ImageView) tabHost.getTabWidget().getChildTabViewAt(0).findViewById(R.id.tabindicator_icon);
		world.tileManager.setImageViewTileForPlayer(getResources(), iv, world.model.player.iconID);
	}

	@Override
	protected void onPause() {
		super.onPause();
		world.model.uiSelections.selectedTabHeroInfo = tabHost.getCurrentTabTag();
	}
}