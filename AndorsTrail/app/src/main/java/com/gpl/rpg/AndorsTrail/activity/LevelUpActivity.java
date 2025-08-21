package com.gpl.rpg.AndorsTrail.activity;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.ActorStatsController;
import com.gpl.rpg.AndorsTrail.controller.Constants;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.util.ThemeHelper;

public final class LevelUpActivity extends AndorsTrailBaseActivity {
	private WorldContext world;
	private ControllerContext controllers;
	private Player player;
	private TextView levelup_description;
	private TextView levelup_title;
	private View levelup_adds_new_skillpoint;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(ThemeHelper.getDialogTheme());
		super.onCreate(savedInstanceState);
		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
		if (!app.isInitialized()) { finish(); return; }
		this.world = app.getWorld();
		this.controllers = app.getControllerContext();
		this.player = world.model.player;

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		initializeView(this, R.layout.levelup, R.id.levelup_root);

		levelup_title = (TextView) findViewById(R.id.levelup_title);
		levelup_description = (TextView) findViewById(R.id.levelup_description);
		levelup_adds_new_skillpoint = findViewById(R.id.levelup_adds_new_skillpoint);

		Button b;

		b = (Button) findViewById(R.id.levelup_add_health);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				levelup(ActorStatsController.LevelUpSelection.health);
			}
		});
		b.setText(getString(R.string.levelup_add_health, Constants.LEVELUP_EFFECT_HEALTH));

		b = (Button) findViewById(R.id.levelup_add_attackchance);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				levelup(ActorStatsController.LevelUpSelection.attackChance);
			}
		});
		b.setText(getString(R.string.levelup_add_attackchance, Constants.LEVELUP_EFFECT_ATK_CH));

		b = (Button) findViewById(R.id.levelup_add_attackdamage);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				levelup(ActorStatsController.LevelUpSelection.attackDamage);
			}
		});
		b.setText(getString(R.string.levelup_add_attackdamage, Constants.LEVELUP_EFFECT_ATK_DMG));

		b = (Button) findViewById(R.id.levelup_add_blockchance);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				levelup(ActorStatsController.LevelUpSelection.blockChance);
			}
		});
		b.setText(getString(R.string.levelup_add_blockchance, Constants.LEVELUP_EFFECT_DEF_CH));
	}

	@Override
	protected void onResume() {
		super.onResume();
		final Resources res = getResources();

		if (!player.canLevelup()) {
			this.finish();
			return;
		}

		world.tileManager.setImageViewTile(res, levelup_title, player);
		levelup_description.setText(res.getString(R.string.levelup_description, player.getLevel() + 1));
		if (player.nextLevelAddsNewSkillpoint()) {
			levelup_adds_new_skillpoint.setVisibility(View.VISIBLE);
		} else {
			levelup_adds_new_skillpoint.setVisibility(View.GONE);
		}
	}

	public void levelup(ActorStatsController.LevelUpSelection selectionID) {
		if (LevelUpActivity.this.isFinishing()) return;

		controllers.actorStatsController.addLevelupEffect(player, selectionID);
		finish();
	}
}
