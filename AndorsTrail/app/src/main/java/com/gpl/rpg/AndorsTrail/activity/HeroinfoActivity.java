package com.gpl.rpg.AndorsTrail.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.ImageView;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.activity.fragment.HeroinfoActivity_Inventory;
import com.gpl.rpg.AndorsTrail.activity.fragment.HeroinfoActivity_Quests;
import com.gpl.rpg.AndorsTrail.activity.fragment.HeroinfoActivity_Skills;
import com.gpl.rpg.AndorsTrail.activity.fragment.HeroinfoActivity_Stats;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.InputController;
import com.gpl.rpg.AndorsTrail.util.ThemeHelper;

public final class HeroinfoActivity extends AndorsTrailBaseFragmentActivity {
	private WorldContext world;
	private AndorsTrailApplication app;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(ThemeHelper.getBaseTheme());
		super.onCreate(savedInstanceState);
		app = AndorsTrailApplication.getApplicationFromActivity(this);
		if (!app.isInitialized()) { finish(); return; }
		this.world = app.getWorld();

		setupTabHost(R.layout.tabbedlayout, R.id.realtabcontent);

		addTab("char",   R.string.heroinfo_char,   R.drawable.char_hero,          HeroinfoActivity_Stats.class);
		addTab("quests", R.string.heroinfo_quests,  R.drawable.ui_icon_quest,      HeroinfoActivity_Quests.class);
		addTab("skills", R.string.heroinfo_skill,   R.drawable.ui_icon_skill,      HeroinfoActivity_Skills.class);
		addTab("inv",    R.string.heroinfo_inv,     R.drawable.ui_icon_equipment,  HeroinfoActivity_Inventory.class);

		String t = world.model.uiSelections.selectedTabHeroInfo;
		if (t != null && !t.isEmpty()) {
			tabHost.post(() -> tabHost.setCurrentTabByTag(t));
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

	@SuppressLint("RestrictedApi")
    @Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
			if (InputController.isMappedKey(event.getKeyCode(), InputController.KEY_HERO_INFO)) {
				finish();
				return true;
			}
		}
		return super.dispatchKeyEvent(event);
    }

}