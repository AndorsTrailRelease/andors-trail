package com.gpl.rpg.AndorsTrail.controller;

import java.util.ArrayList;

import android.os.Handler;
import android.os.Message;

import com.gpl.rpg.AndorsTrail.AndorsTrailPreferences;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.VisualEffectController.VisualEffectCompletedCallback;
import com.gpl.rpg.AndorsTrail.controller.listeners.CombatActionListeners;
import com.gpl.rpg.AndorsTrail.controller.listeners.CombatSelectionListeners;
import com.gpl.rpg.AndorsTrail.controller.listeners.CombatTurnListeners;
import com.gpl.rpg.AndorsTrail.model.ability.SkillCollection;
import com.gpl.rpg.AndorsTrail.model.actor.Actor;
import com.gpl.rpg.AndorsTrail.model.actor.Monster;
import com.gpl.rpg.AndorsTrail.model.actor.MonsterType;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.model.item.ItemContainer;
import com.gpl.rpg.AndorsTrail.model.item.ItemTraits_OnHitReceived;
import com.gpl.rpg.AndorsTrail.model.item.ItemTraits_OnUse;
import com.gpl.rpg.AndorsTrail.model.item.Loot;
import com.gpl.rpg.AndorsTrail.model.map.MonsterSpawnArea;
import com.gpl.rpg.AndorsTrail.resource.VisualEffectCollection;
import com.gpl.rpg.AndorsTrail.util.Coord;

import static java.lang.Math.max;

public final class CombatController implements VisualEffectCompletedCallback {
	private final ControllerContext controllers;
	private final WorldContext world;
	public final CombatSelectionListeners combatSelectionListeners = new CombatSelectionListeners();
	public final CombatActionListeners combatActionListeners = new CombatActionListeners();
	public final CombatTurnListeners combatTurnListeners = new CombatTurnListeners();

	private Monster currentActiveMonster = null;
	private final ArrayList<Loot> killedMonsterBags = new ArrayList<Loot>();
	private int totalExpThisFight = 0;

	public CombatController(ControllerContext controllers, WorldContext world) {
		this.controllers = controllers;
		this.world = world;
	}

	public static enum BeginTurnAs {
		player, monsters, continueLastTurn
	}

	public void enterCombat(BeginTurnAs whoseTurn) {
		world.model.uiSelections.isInCombat = true;
		resetCombatState();
		combatTurnListeners.onCombatStarted();
		if (whoseTurn == BeginTurnAs.player) newPlayerTurn(true);
		else if (whoseTurn == BeginTurnAs.monsters) beginMonsterTurn(true);
		else continueTurn();
	}
	public void exitCombat(boolean pickupLootBags) {
		setCombatSelection(null, null);
		world.model.uiSelections.isInCombat = false;
		if (pickupLootBags) {
			recordLootInCombatLog();
		}
		combatTurnListeners.onCombatEnded();
		controllers.actorStatsController.setActorMaxAP(world.model.player);
		world.model.uiSelections.selectedPosition = null;
		world.model.uiSelections.selectedMonster = null;
		if (world.model.player.isDead()) {
			controllers.gameRoundController.resetRoundTimers();
		} else {
			endOfCombatRound();
		}
		if (pickupLootBags && totalExpThisFight > 0) {
			controllers.itemController.lootMonsterBags(killedMonsterBags, totalExpThisFight);
		} else {
			controllers.gameRoundController.resume();
		}
		resetCombatState();
	}

	private void recordLootInCombatLog() {
		Loot combinedLoot = Loot.combine(killedMonsterBags);
		if (combinedLoot.gold > 0) {
			world.model.combatLog.append(controllers.getResources().getString(R.string.dialog_loot_foundgold, combinedLoot.gold));
		}
		int itemCount = combinedLoot.items.countItems();
		if (itemCount > 0) {
			StringBuilder itemMessage = new StringBuilder();
			if (itemCount == 1) {
				itemMessage.append(controllers.getResources().getString(R.string.combat_log_item_single));
			} else {
				itemMessage.append(controllers.getResources().getString(R.string.combat_log_item_plural, itemCount));
			}
			boolean firstItem = true;
			for (ItemContainer.ItemEntry entry : combinedLoot.items.items) {
				if (!firstItem) {
					itemMessage.append(";");
				}
				itemMessage.append(" " + entry.itemType.getName(world.model.player) + " (" + entry.quantity + ")");
				firstItem = false;
			}
			world.model.combatLog.append(itemMessage.toString());
		}
	}

	private void resetCombatState() {
		killedMonsterBags.clear();
		totalExpThisFight = 0;
		currentActiveMonster = null;
	}

	public void setCombatSelection(Monster selectedMonster) {
		Coord p = selectedMonster.rectPosition.findPositionAdjacentTo(world.model.player.position);
		setCombatSelection(selectedMonster, p);
	}
	public void setCombatSelection(Monster selectedMonster, Coord selectedPosition) {
		if (selectedMonster != null) {
			if (!selectedMonster.isAgressive(world.model.player)) return;
		}
		Coord previousSelection = world.model.uiSelections.selectedPosition;
		if (previousSelection != null) {
			world.model.uiSelections.selectedPosition = null;
			if (selectedPosition == null || !selectedPosition.equals(previousSelection)) {
			} else {
				previousSelection = null;
			}
		}
		world.model.uiSelections.selectedMonster = selectedMonster;
		if (selectedPosition != null) {
			world.model.uiSelections.selectedPosition = new Coord(selectedPosition);
			world.model.uiSelections.isInCombat = true;
		} else {
			world.model.uiSelections.selectedPosition = null;
		}

		if (selectedMonster != null) combatSelectionListeners.onMonsterSelected(selectedMonster, selectedPosition, previousSelection);
		else if (selectedPosition != null) combatSelectionListeners.onMovementDestinationSelected(selectedPosition, previousSelection);
		else if (previousSelection != null) combatSelectionListeners.onCombatSelectionCleared(previousSelection);
	}

	public void setCombatSelection(Coord p) {
		Monster m = world.model.currentMaps.map.getMonsterAt(p);
		if (m != null) {
			setCombatSelection(m, p);
		} else if (world.model.currentMaps.tileMap.isWalkable(p)) {
			setCombatSelection(null, p);
		}
	}

	private boolean useAPs(int cost) {
		if (controllers.actorStatsController.useAPs(world.model.player, cost)) {
			return true;
		} else {
			combatActionListeners.onPlayerDoesNotHaveEnoughAP();
			return false;
		}
	}

	public boolean canExitCombat() { return getAdjacentAggressiveMonster() == null; }
	private Monster getAdjacentAggressiveMonster() {
		return MovementController.getAdjacentAggressiveMonster(world.model.currentMaps.map, world.model.player);
	}

	public void executeMoveAttack(int dx, int dy) {
		if (!world.model.uiSelections.isPlayersCombatTurn) return;

		if (world.model.uiSelections.selectedMonster != null) {
			executePlayerAttack();
		} else if (world.model.uiSelections.selectedPosition != null) {
			executeCombatMove(world.model.uiSelections.selectedPosition);
		} else if (controllers.effectController.isRunningVisualEffect()) {
			return;
		} else if (canExitCombat()) {
			exitCombat(true);
		} else if (dx != 0 || dy != 0) {
			executeFlee(dx, dy);
		} else {
			Monster m = getAdjacentAggressiveMonster();
			if (m == null) return;
			setCombatSelection(m);
			executePlayerAttack();
		}
	}

	private void executeFlee(int dx, int dy) {
		// avoid monster fields when fleeing
		if (!controllers.movementController.findWalkablePosition(dx, dy, AndorsTrailPreferences.MOVEMENTAGGRESSIVENESS_DEFENSIVE)) return;
		Monster m = world.model.currentMaps.map.getMonsterAt(world.model.player.nextPosition);
		if (m != null) return;
		executeCombatMove(world.model.player.nextPosition);
	}

	private AttackResult lastAttackResult;
	private void executePlayerAttack() {
		if (controllers.effectController.isRunningVisualEffect()) return;
		if (!useAPs(world.model.player.getAttackCost())) return;
		final Monster target = world.model.uiSelections.selectedMonster;
		final Coord attackPosition = world.model.uiSelections.selectedPosition;

		final AttackResult attack = playerAttacks(target);
		this.lastAttackResult = attack;

		if (attack.isHit) {
			combatActionListeners.onPlayerAttackSuccess(target, attack);

			if (attack.targetDied) {
				playerKilledMonster(target);
			}

			controllers.skillController.applySkillEffectsFromPlayerAttack(attack, target);
			startAttackEffect(attack, attackPosition, this, CALLBACK_PLAYERATTACK);
		} else {
			combatActionListeners.onPlayerAttackMissed(target, attack);
			controllers.skillController.applySkillEffectsFromPlayerAttack(attack, target);
			startMissedEffect(attack, attackPosition, this, CALLBACK_PLAYERATTACK);
		}
		
	}

	private void playerAttackCompleted() {
		if (world.model.uiSelections.selectedMonster == null) {
			selectNextAggressiveMonster();
		}

		playerActionCompleted();
	}

	public void playerKilledMonster(Monster killedMonster) {
		final Player player = world.model.player;

		Loot loot = world.model.currentMaps.map.getBagOrCreateAt(killedMonster.position);
		killedMonster.createLoot(loot, player);

		controllers.monsterSpawnController.remove(world.model.currentMaps.map, killedMonster);
		controllers.effectController.addSplatter(world.model.currentMaps.map, killedMonster);

		controllers.actorStatsController.addActorAP(player, player.getSkillLevel(SkillCollection.SkillID.cleave) * SkillCollection.PER_SKILLPOINT_INCREASE_CLEAVE_AP);
		controllers.actorStatsController.addActorHealth(player, player.getSkillLevel(SkillCollection.SkillID.eater) * SkillCollection.PER_SKILLPOINT_INCREASE_EATER_HEALTH);

		world.model.statistics.addMonsterKill(killedMonster.monsterType);
		controllers.actorStatsController.addExperience(loot.exp);
		world.model.combatLog.append(controllers.getResources().getString(R.string.dialog_monsterloot_gainedexp, loot.exp));

		totalExpThisFight += loot.exp;
		loot.exp = 0;
		controllers.actorStatsController.applyKillEffectsToPlayer(player);
		controllers.actorStatsController.applyOnDeathEffectsToPlayer(player, killedMonster);

		if (!loot.hasItemsOrGold()) {
			world.model.currentMaps.map.removeGroundLoot(loot);
		} else if (world.model.uiSelections.isInCombat) {
			killedMonsterBags.add(loot);
		}

		combatActionListeners.onPlayerKilledMonster(killedMonster);

		if (world.model.uiSelections.selectedMonster == killedMonster) {
			selectNextAggressiveMonster();
		}
	}

	private boolean selectNextAggressiveMonster() {
		Monster nextMonster = getAdjacentAggressiveMonster();
		if (nextMonster == null) {
			setCombatSelection(null, null);
			return false;
		}
		setCombatSelection(nextMonster);
		return true;
	}

	public boolean playerHasApLeft() {
		final Player player = world.model.player;
		if (player.hasAPs(player.getUseItemCost())) return true;
		if (player.hasAPs(player.getAttackCost())) return true;
		if (player.hasAPs(player.getMoveCost())) return true;
		return false;
	}
	private void playerActionCompleted() {
		if (!world.model.uiSelections.isInCombat) return;
		if (canExitCombat()) {
			exitCombat(true);
			return;
		}
		if (!playerHasApLeft()) endPlayerTurn();
	}
	private void continueTurn() {
		if (world.model.uiSelections.isPlayersCombatTurn) return;
		if (playerHasApLeft()) {
			world.model.uiSelections.isPlayersCombatTurn = true;
			return;
		}
		handleNextMonsterAction();
	}

	private void executeCombatMove(final Coord dest) {
		if (world.model.uiSelections.selectedMonster != null) return;
		if (dest == null) return;
		if (!useAPs(world.model.player.getMoveCost())) return;

		int fleeChanceBias = world.model.player.getSkillLevel(SkillCollection.SkillID.evasion) * SkillCollection.PER_SKILLPOINT_INCREASE_EVASION_FLEE_CHANCE_PERCENTAGE;
		if (Constants.roll100(Constants.FLEE_FAIL_CHANCE_PERCENT - fleeChanceBias)) {
			fleeingFailed();
			return;
		}

		world.model.player.nextPosition.set(dest);
		controllers.movementController.moveToNextIfPossible();

		playerActionCompleted();
	}

	private void fleeingFailed() {
		combatActionListeners.onPlayerFailedFleeing();
		endPlayerTurn();
	}

	private final Handler monsterTurnHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			removeMessages(0);
			CombatController.this.handleNextMonsterAction();
		}
	};
	private void waitForNextMonsterAction() {
		if (controllers.preferences.attackspeed_milliseconds <= 0) {
			handleNextMonsterAction();
		} else {
			monsterTurnHandler.sendEmptyMessageDelayed(0, controllers.preferences.attackspeed_milliseconds);
		}
	}

	public void endPlayerTurn() {
		beginMonsterTurn(false);
	}
	private void beginMonsterTurn(boolean isFirstRound) {
		controllers.actorStatsController.setActorMinAP(world.model.player);
		world.model.uiSelections.isPlayersCombatTurn = false;
		for (MonsterSpawnArea a : world.model.currentMaps.map.spawnAreas) {
			for (Monster m : a.monsters) {
				controllers.actorStatsController.setActorMaxAP(m);
			}
		}
		currentActiveMonster = null;
		if (!isFirstRound) controllers.gameRoundController.onNewMonsterRound();
		handleNextMonsterAction();
	}

	private static enum MonsterAction {
		none, attack, move
	}
	private MonsterAction determineNextMonsterAction(Coord playerPosition) {
		if (currentActiveMonster != null) {
			if (shouldAttackWithMonsterInCombat(currentActiveMonster, playerPosition)) return MonsterAction.attack;
		}

		for (MonsterSpawnArea a : world.model.currentMaps.map.spawnAreas) {
			for (Monster m : a.monsters) {
				if (!m.isAgressive(world.model.player)) continue;

				if (shouldAttackWithMonsterInCombat(m, playerPosition)) {
					currentActiveMonster = m;
					return MonsterAction.attack;
				} else if (shouldMoveMonsterInCombat(m, a, world.model.player, playerPosition)) {
					currentActiveMonster = m;
					return MonsterAction.move;
				}
			}
		}
		return MonsterAction.none;
	}

	private static boolean shouldAttackWithMonsterInCombat(Monster m, Coord playerPosition) {
		if (!m.hasAPs(m.getAttackCost())) return false;
		if (!m.rectPosition.isAdjacentTo(playerPosition)) return false;
		return true;
	}
	private static boolean shouldMoveMonsterInCombat(Monster m, MonsterSpawnArea a, Player p, Coord playerPosition) {
		final MonsterType.AggressionType movementAggressionType = m.getMovementAggressionType();
		if (movementAggressionType == MonsterType.AggressionType.none) return false;

		if (!m.hasAPs(m.getMoveCost())) return false;
		if (m.position.isAdjacentTo(playerPosition)) return false;
		if (!m.isAgressive(p)) return false;

		if (movementAggressionType == MonsterType.AggressionType.protectSpawn) {
			if (a.area.contains(playerPosition)) return true;
		} else if (movementAggressionType == MonsterType.AggressionType.helpOthers) {
			for (Monster o : a.monsters) {
				if (o == m) continue;
				if (o.rectPosition.isAdjacentTo(playerPosition)) return true;
			}
		} else if (movementAggressionType == MonsterType.AggressionType.wholeMap) {
			return true;
		}
		return false;
	}

	private void handleNextMonsterAction() {
		if (!world.model.uiSelections.isMainActivityVisible) return;

		MonsterAction nextMonsterAction = determineNextMonsterAction(world.model.player.position);
		if (nextMonsterAction == MonsterAction.none) {
			endMonsterTurn();
		} else if (nextMonsterAction == MonsterAction.attack) {
			attackWithCurrentMonster();
		} else if (nextMonsterAction == MonsterAction.move) {
			moveCurrentMonster();
		}
	}

	private void moveCurrentMonster() {
		controllers.actorStatsController.useAPs(currentActiveMonster, currentActiveMonster.getMoveCost());
		if (!controllers.monsterMovementController.findPathFor(currentActiveMonster, world.model.player.position)) {
			// Couldn't find a path to move on.
			handleNextMonsterAction();
			return;
		}
		
		final Monster movingMonster = currentActiveMonster;
		controllers.monsterMovementController.moveMonsterToNextPositionDuringCombat(currentActiveMonster, world.model.currentMaps.map, new VisualEffectController.VisualEffectCompletedCallback(){
			@Override
			public void onVisualEffectCompleted(int callbackValue) {
				combatActionListeners.onMonsterMovedDuringCombat(movingMonster);
				handleNextMonsterAction();
			}
		});
		
	}

	private void attackWithCurrentMonster() {
		controllers.actorStatsController.useAPs(currentActiveMonster, currentActiveMonster.getAttackCost());

		combatTurnListeners.onMonsterIsAttacking(currentActiveMonster);
		AttackResult attack = monsterAttacks(currentActiveMonster);
		this.lastAttackResult = attack;

		if (attack.isHit) {
			combatActionListeners.onMonsterAttackSuccess(currentActiveMonster, attack);
			controllers.skillController.applySkillEffectsFromMonsterAttack(attack, currentActiveMonster);
			startAttackEffect(attack, world.model.player.position, this, CALLBACK_MONSTERATTACK);
		} else {
			combatActionListeners.onMonsterAttackMissed(currentActiveMonster, attack);
			controllers.skillController.applySkillEffectsFromMonsterAttack(attack, currentActiveMonster);
			startMissedEffect(attack, world.model.player.position, this, CALLBACK_MONSTERATTACK);
		}
	}

	private static final int CALLBACK_MONSTERATTACK = 0;
	private static final int CALLBACK_PLAYERATTACK = 1;

	@Override
	public void onVisualEffectCompleted(int callbackValue) {
		if (!world.model.uiSelections.isInCombat) return;
		if (callbackValue == CALLBACK_MONSTERATTACK) {
			monsterAttackCompleted();
		} else if (callbackValue == CALLBACK_PLAYERATTACK) {
			playerAttackCompleted();
		}
	}

	private void monsterAttackCompleted() {
		if (lastAttackResult.targetDied) {
			controllers.mapController.handlePlayerDeath();
			return;
		}
		handleNextMonsterAction();
	}

	private void startAttackEffect(AttackResult attack, final Coord position, VisualEffectCompletedCallback callback, int callbackValue) {
		if (controllers.preferences.attackspeed_milliseconds <= 0) {
			callback.onVisualEffectCompleted(callbackValue);
			return;
		}
		controllers.effectController.startEffect(
				position
				, VisualEffectCollection.VisualEffectID.redSplash
				, (attack.damage == 0) ? null : String.valueOf(attack.damage)
				, callback
				, callbackValue);
	}
	
	private void startMissedEffect(AttackResult attack, final Coord position, VisualEffectCompletedCallback callback, int callbackValue) {
		if (controllers.preferences.attackspeed_milliseconds <= 0) {
			callback.onVisualEffectCompleted(callbackValue);
			return;
		}
		controllers.effectController.startEffect(
				position
				, VisualEffectCollection.VisualEffectID.miss
				, controllers.getResources().getString(R.string.combat_miss_animation_message)
				, callback
				, callbackValue);
	}
	
	
	private void endMonsterTurn() {
		currentActiveMonster = null;
		newPlayerTurn(false);
	}

	private void newPlayerTurn(boolean isFirstRound) {
		if (canExitCombat()) {
			exitCombat(true);
			return;
		}
		controllers.actorStatsController.setActorMaxAP(world.model.player);
		if (!isFirstRound) controllers.gameRoundController.onNewPlayerRound();
		world.model.uiSelections.isPlayersCombatTurn = true;
		combatTurnListeners.onNewPlayerTurn();
	}

	private static boolean hasCriticalAttack(Actor attacker, Actor target) {
		if (!attacker.hasCriticalAttacks()) return false;
		if (target.isImmuneToCriticalHits()) return false;
		return true;
	}

	// see this post for explenations about the calculation: https://andorstrail.com/viewtopic.php?f=3&t=6661
	// if you change code here make sure to run the tests in CombatControllerTest.java
	public static float getAverageDamagePerHit(final Actor attacker, final Actor target) {
		final int numPossibleOutcomes =  attacker.getDamagePotential().max - attacker.getDamagePotential().current + 1;
		float avgNonCriticalDamage = 0;
		for (int n = 0; n < numPossibleOutcomes; n++) {
			avgNonCriticalDamage += max(0, (float) n + attacker.getDamagePotential().current - target.getDamageResistance()) / numPossibleOutcomes;
		}

		float avgCriticalDamage = 0;
		float effectiveCriticalChance = 0;
		if (hasCriticalAttack(attacker, target)) {
			effectiveCriticalChance = attacker.getEffectiveCriticalChance();
		}
		if (effectiveCriticalChance > 0) {
			for (int n = 0; n < numPossibleOutcomes; n++) {
				avgCriticalDamage += max(0, Math.floor((n + attacker.getDamagePotential().current) * attacker.getCriticalMultiplier()) - target.getDamageResistance()) / numPossibleOutcomes;
			}
		}

		float avgDamagePerSuccessfulStrike = (1 - effectiveCriticalChance / 100) * avgNonCriticalDamage + effectiveCriticalChance * avgCriticalDamage / 100;
		return (float)getAttackHitChance(attacker, target) * avgDamagePerSuccessfulStrike / 100;
	}
	private static float getAverageDamagePerTurn(Actor attacker, Actor target) {
		return getAverageDamagePerHit(attacker, target) * attacker.getAttacksPerTurn();
	}
	private static int getTurnsToKillTarget(Actor attacker, Actor target) {
		if (hasCriticalAttack(attacker, target)) {
			if (attacker.getDamagePotential().max * attacker.getCriticalMultiplier() <= target.getDamageResistance()) return 999;
		} else {
			if (attacker.getDamagePotential().max <= target.getDamageResistance()) return 999;
		}

		float averageDamagePerTurn = getAverageDamagePerTurn(attacker, target);
		if (averageDamagePerTurn <= 0) return 100;
		return (int) Math.ceil(target.getMaxHP() / averageDamagePerTurn);
	}
	public int getMonsterDifficulty(Monster monster) {
		// returns [0..100) . 100 == easy.
		int turnsToKillMonster = getTurnsToKillTarget(world.model.player, monster);
		if (turnsToKillMonster >= 999) return 0;
		int turnsToKillPlayer = getTurnsToKillTarget(monster, world.model.player);
		int result = 50 + (turnsToKillPlayer - turnsToKillMonster) * 2;
		if (result <= 1) return 1;
		if (result > 100) return 100;
		return result;
	}

	private AttackResult playerAttacks(Monster currentMonster) {
		AttackResult result = attack(world.model.player, currentMonster);
		return result;
	}

	private AttackResult monsterAttacks(Monster currentMonster) {
		AttackResult result = attack(currentMonster, world.model.player);
		return result;
	}


	private static final int n = 50;
	private static final int F = 40;
	private static final float two_divided_by_PI = (float) (2f / Math.PI);
	private static int getAttackHitChance(final Actor attacker, final Actor target) {
		final int c = attacker.getAttackChance() - target.getBlockChance();
		// (2/pi)*atan(..) will vary from -1 to +1 .
		return (int) (50 * (1 + two_divided_by_PI * (float)Math.atan((float)(c-n) / F)));
	}

	private AttackResult attack(final Actor attacker, final Actor target) {
		int hitChance = getAttackHitChance(attacker, target);
		if (!Constants.roll100(hitChance)) return AttackResult.MISS;

		int damage = Constants.rollValue(attacker.getDamagePotential());
		boolean isCriticalHit = false;
		if (hasCriticalAttack(attacker, target)) {
			isCriticalHit = Constants.roll100(attacker.getEffectiveCriticalChance());
			if (isCriticalHit) {
				damage *= attacker.getCriticalMultiplier();
			}
		}
		damage -= target.getDamageResistance();
		if (damage < 0) damage = 0;
		controllers.actorStatsController.removeActorHealth(target, damage);

		applyAttackHitStatusEffects(attacker, target);

		return new AttackResult(true, isCriticalHit, damage, target.isDead());
	}

	private void applyAttackHitStatusEffects(Actor attacker, Actor target) {
		ItemTraits_OnUse[] onHitEffects = attacker.getOnHitEffects();
		ItemTraits_OnHitReceived[] onHitReceivedEffects = target.getOnHitReceivedEffects();
		if (onHitEffects != null) {
			for (ItemTraits_OnUse e : onHitEffects) {
				controllers.actorStatsController.applyUseEffect(attacker, target, e);
			}
		}
		if (onHitReceivedEffects != null) {
			for (ItemTraits_OnHitReceived e : onHitReceivedEffects) {
				controllers.actorStatsController.applyHitReceivedEffect(target, attacker, e);
			}
		}
	}

	public void endOfCombatRound() {
		world.model.worldData.tickWorldTime();
		controllers.gameRoundController.resetRoundTimers();
		controllers.actorStatsController.applyConditionsToPlayer(world.model.player, false);
		controllers.actorStatsController.applyConditionsToMonsters(world.model.currentMaps.map, true);
	}

	public void monsterSteppedOnPlayer(Monster m) {
		setCombatSelection(m);
		enterCombat(BeginTurnAs.monsters);
	}

	public void startFlee() {
		setCombatSelection(null, null);
		combatActionListeners.onPlayerStartedFleeing();
	}
}
