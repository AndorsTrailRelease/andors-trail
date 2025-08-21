package com.gpl.rpg.AndorsTrail.activity;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
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

public final class MonsterEncounterActivity extends AndorsTrailBaseActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(ThemeHelper.getDialogTheme());
		super.onCreate(savedInstanceState);
		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
		if (!app.isInitialized()) { finish(); return; }
		final WorldContext world = app.getWorld();
		final ControllerContext controllers = app.getControllerContext();

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		final Monster monster = Dialogs.getMonsterFromIntent(getIntent(), world);
		if (monster == null) {
			finish();
			return;
		}

		initializeView(this, R.layout.monsterencounter, R.id.monsterencounter_root);

		CharSequence difficulty = getText(MonsterInfoActivity.getMonsterDifficultyResource(controllers, monster));

		TextView tv = (TextView) findViewById(R.id.monsterencounter_title);
		tv.setText(monster.getName());
		world.tileManager.setImageViewTile(getResources(), tv, monster, world.model.currentMaps.tiles);

		tv = (TextView) findViewById(R.id.monsterencounter_description);
		tv.setText(getString(R.string.dialog_monsterencounter_message, difficulty));

		Button b = (Button) findViewById(R.id.monsterencounter_attack);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				setResult(RESULT_OK);
				MonsterEncounterActivity.this.finish();
			}
			 });
		b = (Button) findViewById(R.id.monsterencounter_cancel);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				setResult(RESULT_CANCELED);
				MonsterEncounterActivity.this.finish();
			}
		});
		b = (Button) findViewById(R.id.monsterencounter_info);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Dialogs.showMonsterInfo(MonsterEncounterActivity.this, monster);
			}
		});
	}
}
