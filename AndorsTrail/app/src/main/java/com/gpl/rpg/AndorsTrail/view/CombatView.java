package com.gpl.rpg.AndorsTrail.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.AndorsTrailPreferences;
import com.gpl.rpg.AndorsTrail.Dialogs;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.activity.MainActivity;
import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.CombatController;
import com.gpl.rpg.AndorsTrail.controller.listeners.ActorConditionListener;
import com.gpl.rpg.AndorsTrail.controller.listeners.ActorStatsListener;
import com.gpl.rpg.AndorsTrail.controller.listeners.CombatSelectionListener;
import com.gpl.rpg.AndorsTrail.controller.listeners.CombatTurnListener;
import com.gpl.rpg.AndorsTrail.model.ability.ActorCondition;
import com.gpl.rpg.AndorsTrail.model.actor.Actor;
import com.gpl.rpg.AndorsTrail.model.actor.Monster;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.util.Coord;

public final class CombatView extends RelativeLayout implements CombatSelectionListener, CombatTurnListener, ActorStatsListener, ActorConditionListener {
	private final RangeBar playerAPBar;
	private final Button attackMoveButton;
	private final Button endTurnButton;
	private final Button fleeButton;
	private final ImageButton monsterInfo;
	private final RangeBar monsterHealth;
	private final ImageButton monsterConditionsButton;
	private final RelativeLayout activeConditionsBar;
	private DisplayActiveActorConditionIcons activeConditions;
	private final ViewGroup monsterBar;
	private final ViewGroup actionBar;
	private final TextView monsterActionText;

	private final WorldContext world;
	private final ControllerContext controllers;
	private final Resources res;
	private final AndorsTrailPreferences preferences;
	private final Player player;
	private final Animation displayAnimation;
	private final Animation hideAnimation;
	private final Animation displayConditionsButtonAnimation;
	private final Animation hideConditionsButtonAnimation;
	private final Animation displayConditionsBarAnimation;
	private final Animation hideConditionsBarAnimation;

	private Monster currentMonster;
	private boolean conditionsBarToggled = false;

	public CombatView(final Context context, AttributeSet attr) {
		super(context, attr);
		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivityContext(context);
		this.world = app.getWorld();
		this.player = world.model.player;
		this.controllers = app.getControllerContext();
		this.preferences = app.getPreferences();
		this.res = getResources();

		setFocusable(false);
		inflate(context, R.layout.combatview, this);
		//Prevents mis-taps from registering as main area taps by going through the combat view.
		findViewById(R.id.combatview_fixedarea).setClickable(true);

		final CombatController c = controllers.combatController;
		attackMoveButton = findViewById(R.id.combatview_moveattack);
		//Enable marquee if text is too long.
		attackMoveButton.setSelected(true);
		attackMoveButton.setOnClickListener((View v) -> {
			c.executeMoveAttack(0, 0);
			MainView mv = ((MainActivity) getContext()).getMainView();
			mv.post(mv::requestFocus);
		});

		endTurnButton = findViewById(R.id.combatview_endturn);
		//Enable marquee if text is too long.
		endTurnButton.setSelected(true);
		endTurnButton.setOnClickListener((View v) -> {
			c.endPlayerTurn();
			((MainActivity) getContext()).getMainView().post(this::requestFocus);
		});

		fleeButton = findViewById(R.id.combatview_flee);
		//Enable marquee if text is too long.
		fleeButton.setSelected(true);
		fleeButton.setOnClickListener((View v) -> {
			c.startFlee();
			// After fleeing, set the focus back to the main view so the player can choose a direction.
			MainView mv = ((MainActivity) getContext()).getMainView();
			mv.post(mv::requestFocus);
		});

		playerAPBar = (RangeBar) findViewById(R.id.combatview_status);
		playerAPBar.init(R.drawable.ui_progress_ap, R.string.status_ap);

		monsterInfo = (ImageButton) findViewById(R.id.combatview_monsterinfo);
		monsterInfo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Dialogs.showMonsterInfo(context, currentMonster);
			}
		});

		monsterHealth = (RangeBar) findViewById(R.id.combatview_monsterhealth);
		monsterHealth.init(R.drawable.ui_progress_health, R.string.combat_monsterhealth);
		monsterBar = (ViewGroup) findViewById(R.id.combatview_monsterbar);
		actionBar = (ViewGroup) findViewById(R.id.combatview_actionbar);
		monsterActionText = (TextView) findViewById(R.id.combatview_monsterismoving);

		monsterConditionsButton = (ImageButton) findViewById(R.id.combatview_monsterconditions_button);
		monsterConditionsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				toggleConditionsBarVisibility();
			}
		});
		
		activeConditionsBar = (RelativeLayout) findViewById(R.id.combatview_activeconditions);
		activeConditions = new DisplayActiveActorConditionIcons(controllers, world, context, activeConditionsBar);
		
		
//		monsterBar.setBackgroundColor(res.getColor(color.transparent));
//		actionBar.setBackgroundColor(res.getColor(color.transparent));

		displayAnimation = AnimationUtils.loadAnimation(context, R.anim.showcombatbar);
		hideAnimation = AnimationUtils.loadAnimation(context, R.anim.hidecombatbar);
		hideAnimation.setAnimationListener(new AnimationListener() {
			@Override public void onAnimationStart(Animation animation) {}
			@Override public void onAnimationRepeat(Animation animation) {}
			@Override public void onAnimationEnd(Animation arg0) {
				CombatView.this.setVisibility(View.GONE);
			}
		});
		
		displayConditionsButtonAnimation = AnimationUtils.loadAnimation(context, R.anim.showmonsterconditionbutton);
		hideConditionsButtonAnimation = AnimationUtils.loadAnimation(context, R.anim.hidemonsterconditionbutton);
		hideConditionsButtonAnimation.setAnimationListener(new AnimationListener() {
			@Override public void onAnimationStart(Animation animation) {}
			@Override public void onAnimationRepeat(Animation animation) {}
			@Override public void onAnimationEnd(Animation arg0) {
				monsterConditionsButton.setVisibility(View.GONE);
			}
		});

		displayConditionsBarAnimation = AnimationUtils.loadAnimation(context, R.anim.showmonsterconditionbar);
		hideConditionsBarAnimation = AnimationUtils.loadAnimation(context, R.anim.hidemonsterconditionbar);
		hideConditionsBarAnimation.setAnimationListener(new AnimationListener() {
			@Override public void onAnimationStart(Animation animation) {}
			@Override public void onAnimationRepeat(Animation animation) {}
			@Override public void onAnimationEnd(Animation arg0) {
				activeConditionsBar.setVisibility(View.GONE);
			}
		});
	}

	private void toggleConditionsBarVisibility() {
		conditionsBarToggled = !conditionsBarToggled;
		if (conditionsBarToggled) showConditionsBar();
		else hideConditionsBar();
	}
	
	private void updateTurnInfo(Monster currentActiveMonster) {
		if (currentActiveMonster != null) {
			actionBar.setVisibility(View.INVISIBLE);
			monsterActionText.setVisibility(View.VISIBLE);
			monsterActionText.setText(res.getString(R.string.combat_monsteraction, currentActiveMonster.getName()));
		} else {
			actionBar.setVisibility(View.VISIBLE);
			monsterActionText.setVisibility(View.GONE);
		}
	}

	private void updateMonsterHealth(Monster m) {
		monsterHealth.update(m.getMaxHP(), m.getCurrentHP());
	}
	private void updatePlayerAP() {
		playerAPBar.update(player.getMaxAP(), player.getCurrentAP());
		updateAttackMoveButtonText();
	}
	private void updateSelectedMonster(Monster selectedMonster) {
		if (currentMonster != null && currentMonster == selectedMonster) return;

		attackMoveButton.setEnabled(true);
		monsterBar.setVisibility(View.INVISIBLE);
		currentMonster = null;
		if (selectedMonster != null) {
			monsterBar.setVisibility(View.VISIBLE);
			world.tileManager.setImageViewTile(res, monsterInfo, selectedMonster, world.model.currentMaps.tiles);
			updateMonsterHealth(selectedMonster);
			currentMonster = selectedMonster;
		}
		updateAttackMoveButtonText(selectedMonster != null);
		updateConditions();
	}

	private void updateConditions() {
		activeConditions.setTarget(currentMonster);
		if (currentMonster == null) {
			hideConditionsButton();
			return;
		}
		if (currentMonster.conditions.size()+currentMonster.immunities.size() > 0) {
			if (currentMonster.conditions.size() > 0) world.tileManager.setImageViewTile(getContext(), monsterConditionsButton, currentMonster.conditions.get(0).conditionType, false, Integer.toString(currentMonster.conditions.size()+currentMonster.immunities.size()), null);
			else world.tileManager.setImageViewTile(getContext(), monsterConditionsButton, currentMonster.immunities.get(0).conditionType, true, Integer.toString(currentMonster.conditions.size()+currentMonster.immunities.size()), null);
			showConditionsButton();
			if (conditionsBarToggled) showConditionsBar();
		} else {
			hideConditionsButton();
			hideConditionsBar();
		}
	}
	
	private void showConditionsButton() {
		if (monsterConditionsButton.getVisibility() != View.VISIBLE) {
			monsterConditionsButton.setVisibility(View.VISIBLE);
			if (preferences.enableUiAnimations) {
				monsterConditionsButton.startAnimation(displayConditionsButtonAnimation);
			}
		}
	}

	private void hideConditionsButton() {
		if (monsterConditionsButton.getVisibility() == View.VISIBLE) {
			if (preferences.enableUiAnimations) {
				monsterConditionsButton.startAnimation(hideConditionsButtonAnimation);
			} else {
				monsterConditionsButton.setVisibility(View.GONE);
			}
		}
	}
	
	@SuppressLint("NewApi")
	private void showConditionsBar() {
		if (activeConditionsBar.getVisibility() != View.VISIBLE) {
			activeConditionsBar.setVisibility(View.VISIBLE);
			if (preferences.enableUiAnimations) {
				activeConditionsBar.startAnimation(displayConditionsBarAnimation);
			}
		}
	}

	
	@SuppressLint("NewApi")
	private void hideConditionsBar() {
		if (activeConditionsBar.getVisibility() == View.VISIBLE) {
			if (preferences.enableUiAnimations) {
				activeConditionsBar.startAnimation(hideConditionsBarAnimation);
			} else {
				activeConditionsBar.setVisibility(View.GONE);
			}
		}
	}
	
	private void updateAttackMoveButtonText() {
		updateAttackMoveButtonText(world.model.uiSelections.selectedMonster != null);
	}
	private void updateAttackMoveButtonText(boolean hasSelectedMonster) {
		if (hasSelectedMonster) {
			attackMoveButton.setText(res.getString(R.string.combat_attack, player.getAttackCost()));
		} else {
			attackMoveButton.setText(res.getString(R.string.combat_move, player.getMoveCost()));
		}
	}

	public void updateStatus() {
		updatePlayerAP();
		updateSelectedMonster(world.model.uiSelections.selectedMonster);
	}

	private void show() {
		updateStatus();
		setVisibility(View.VISIBLE);
		bringToFront();
		if (preferences.enableUiAnimations) {
			startAnimation(displayAnimation);
		}
	}

	private void hide() {
		if (preferences.enableUiAnimations) {
			startAnimation(hideAnimation);
		} else {
			setVisibility(View.GONE);
		}
	}

	public void subscribe() {
		controllers.combatController.combatSelectionListeners.add(this);
		controllers.combatController.combatTurnListeners.add(this);
		controllers.actorStatsController.actorStatsListeners.add(this);
		controllers.actorStatsController.actorConditionListeners.add(this);
		activeConditions.subscribe();
	}
	public void unsubscribe() {
		controllers.actorStatsController.actorStatsListeners.remove(this);
		controllers.combatController.combatTurnListeners.remove(this);
		controllers.combatController.combatSelectionListeners.remove(this);
		controllers.actorStatsController.actorConditionListeners.remove(this);
		activeConditions.unsubscribe();
	}

	@Override
	public void onMonsterSelected(Monster m, Coord selectedPosition, Coord previousSelection) {
		updateSelectedMonster(m);
	}

	@Override
	public void onMovementDestinationSelected(Coord selectedPosition, Coord previousSelection) {
		updateSelectedMonster(null);
	}

	@Override
	public void onCombatSelectionCleared(Coord previousSelection) {
		updateSelectedMonster(null);
	}

	@Override
	public void onCombatStarted() {
		show();
		updateTurnInfo(null);
	}

	@Override
	public void onCombatEnded() {
		hide();
	}

	@Override
	public void onNewPlayerTurn() {
		updateTurnInfo(null);
	}

	@Override
	public void onMonsterIsAttacking(Monster m) {
		updateTurnInfo(m);
	}

	@Override
	public void onActorHealthChanged(Actor actor) {
		if (actor == currentMonster) updateMonsterHealth(currentMonster);
	}

	@Override
	public void onActorAPChanged(Actor actor) {
		if (actor == player) updatePlayerAP();
	}

	@Override
	public void onActorAttackCostChanged(Actor actor, int newAttackCost) {
		if (actor == player) updateAttackMoveButtonText();
	}

	@Override
	public void onActorMoveCostChanged(Actor actor, int newMoveCost) {
		if (actor == player) updateAttackMoveButtonText();
	}

	@Override
	public void onPlayerReequipCostChanged(Player actor, int newAttackCost) {}

	@Override
	public void onPlayerUseCostChanged(Player actor, int newMoveCost) {}

	@Override
	public void onActorConditionAdded(Actor actor, ActorCondition condition) {
		if (actor == currentMonster) updateConditions();
	}

	@Override
	public void onActorConditionRemoved(Actor actor, ActorCondition condition) {
		if (actor == currentMonster) updateConditions();
	}

	@Override
	public void onActorConditionDurationChanged(Actor actor, ActorCondition condition) {
	}

	@Override
	public void onActorConditionMagnitudeChanged(Actor actor, ActorCondition condition) {
	}

	@Override
	public void onActorConditionRoundEffectApplied(Actor actor, ActorCondition condition) {
	}

	@Override
	public void onActorConditionImmunityAdded(Actor actor, ActorCondition condition) {
		if (actor == currentMonster) updateConditions();
	}

	@Override
	public void onActorConditionImmunityRemoved(Actor actor, ActorCondition condition) {
		if (actor == currentMonster) updateConditions();
	}

	@Override
	public void onActorConditionImmunityDurationChanged(Actor actor, ActorCondition condition) {
	}
}
