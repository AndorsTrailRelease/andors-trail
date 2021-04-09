package com.gpl.rpg.AndorsTrail.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.Dialogs;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.model.actor.Monster;
import com.gpl.rpg.AndorsTrail.util.ThemeHelper;
import com.gpl.rpg.AndorsTrail.view.ItemEffectsView;
import com.gpl.rpg.AndorsTrail.view.RangeBar;
import com.gpl.rpg.AndorsTrail.view.TraitsInfoView;

public final class MonsterInfoActivity extends AndorsTrailBaseActivity {

	private WorldContext world;
	private ControllerContext controllers;

	private TextView monsterinfo_title;
	private TextView monsterinfo_difficulty;
	private ItemEffectsView monsterinfo_onhiteffects;
	private RangeBar hp;
	private ViewGroup monsterinfo_container;
	private TextView monsterinfo_max_ap;
	private TextView monsterinfo_killcount;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(ThemeHelper.getDialogTheme());
		super.onCreate(savedInstanceState);
		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
		if (!app.isInitialized()) { finish(); return; }
		this.world = app.getWorld();
		this.controllers = app.getControllerContext();
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.monsterinfo);

		monsterinfo_title = (TextView) findViewById(R.id.monsterinfo_title);
		monsterinfo_difficulty = (TextView) findViewById(R.id.monsterinfo_difficulty);
		monsterinfo_max_ap = (TextView) findViewById(R.id.monsterinfo_max_ap);
		monsterinfo_killcount = (TextView) findViewById(R.id.monsterinfo_killcount);

		Button b = (Button) findViewById(R.id.monsterinfo_close);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				MonsterInfoActivity.this.finish();
			}
		});

		monsterinfo_onhiteffects = (ItemEffectsView) findViewById(R.id.actorinfo_onhiteffects);
		hp = (RangeBar) findViewById(R.id.monsterinfo_healthbar);
		hp.init(R.drawable.ui_progress_health, R.string.status_hp);
		monsterinfo_container = (ViewGroup) findViewById(R.id.monsterinfo_container);
	}

	@Override
	protected void onResume() {
		super.onResume();

		Monster monster = Dialogs.getMonsterFromIntent(getIntent(), world);
		if (monster == null) {
			finish();
			return;
		}

		updateTitle(monster);
		updateTraits(monster);
	}

	private void updateTitle(Monster monster) {
		monsterinfo_title.setText(monster.getName());
		world.tileManager.setImageViewTile(getResources(), monsterinfo_title, monster, world.model.currentMaps.tiles);
		monsterinfo_difficulty.setText(getMonsterDifficultyResource(controllers, monster));
	}

	private void updateTraits(Monster monster) {
		TraitsInfoView.update(monsterinfo_container, monster);
		monsterinfo_onhiteffects.update(
				null,
				null,
				monster.getOnHitEffectsAsList(),
				null,
				monster.getOnHitReceivedEffectsAsList(),
				monster.getOnDeathEffects(),
				false);
		hp.update(monster.getMaxHP(), monster.getCurrentHP());
		monsterinfo_max_ap.setText(Integer.toString(monster.getMaxAP()));
		monsterinfo_killcount.setText(Integer.toString(world.model.statistics.getNumberOfKillsForMonsterName(monster.getName()) ));
	}

	public static int getMonsterDifficultyResource(ControllerContext controllerContext, Monster monster) {
		final int difficulty = controllerContext.combatController.getMonsterDifficulty(monster);
		if (difficulty >= 80) return R.string.monster_difficulty_veryeasy;
		if (difficulty >= 60) return R.string.monster_difficulty_easy;
		if (difficulty >= 40) return R.string.monster_difficulty_normal;
		if (difficulty >= 20) return R.string.monster_difficulty_hard;
		if (difficulty == 0) return R.string.monster_difficulty_impossible;
		return R.string.monster_difficulty_veryhard;
	}
}
