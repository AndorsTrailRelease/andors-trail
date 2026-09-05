package com.gpl.rpg.AndorsTrail.controller;

import java.util.ArrayList;

import com.gpl.rpg.AndorsTrail.util.Format;
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
import com.gpl.rpg.AndorsTrail.model.item.ItemType;
import com.gpl.rpg.AndorsTrail.model.item.Inventory;
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

	/**
	 * Restores combat turn handling when the main activity becomes visible
	 * again while combat is still active.
	 */
	public void resumeCombatIfNeeded() {
		if (!world.model.uiSelections.isMainActivityVisible) return;
		if (!world.model.uiSelections.isInCombat) return;
		setCombatSelection(world.model.uiSelections.selectedMonster, world.model.uiSelections.selectedPosition);
		enterCombat(BeginTurnAs.continueLastTurn);
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
		exitCombat(pickupLootBags, false);
	}
	public void exitCombat(boolean pickupLootBags, boolean canceledCombat) {
		setCombatSelection(null, null);
		world.model.uiSelections.isInCombat = false;
		if (pickupLootBags) {
			recordLootInCombatLog();
		}
		combatTurnListeners.onCombatEnded();
		controllers.actorStatsController.setActorMaxAP(world.model.player);
		world.model.uiSelections.selectedPosition = null;
		world.model.uiSelections.selectedMonster = null;
		if (world.model.player.isDead() || canceledCombat) {
			controllers.gameRoundController.resetRoundTimers();
		} else {
			endOfCombatRound();
		}
		if (pickupLootBags && totalExpThisFight > 0) {
			controllers.itemController.lootMonsterBags(killedMonsterBags, totalExpThisFight);
		}
		controllers.gameRoundController.onCombatStateChanged();
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

			// This is clunky and does not support i18n well.  Should use CLDR ListFormatter when we have a minimum API level of 24, but for now this will do.
			boolean firstItem = true;
			for (ItemContainer.ItemEntry entry : combinedLoot.items.items) {
				if (!firstItem) {
					itemMessage.append(controllers.getResources().getString(R.string.combat_log_item_list_separator));
				}
				itemMessage.append(controllers.getResources().getString(R.string.combat_log_item_name_qty, entry.itemType.getName(world.model.player), entry.quantity));
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
			startAttackEffect(attack, world.model.player, attackPosition, this, CALLBACK_PLAYERATTACK);
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
			if(!killedMonsterBags.contains(loot))
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
		startAttackEffect(attack, null, position, callback, callbackValue);
	}

	private void startAttackEffect(AttackResult attack, final Actor attacker, final Coord position, VisualEffectCompletedCallback callback, int callbackValue) {
		if (controllers.preferences.attackspeed_milliseconds <= 0) {
			callback.onVisualEffectCompleted(callbackValue);
			return;
		}

		String displayValue = attack.damage == 0 ? null : Format.localizeInt(attack.damage);
		if (attacker instanceof Player && isPoleWeapon((Player) attacker)) {
			controllers.effectController.startEffect(attacker.position, VisualEffectCollection.VisualEffectID.poleAttack, null, null, 0, getPoleAttackRotation(attacker.position, position), getPoleAttackScale(attacker.position, position));
		}
		if (attacker instanceof Player) startLongswordAttack((Player) attacker, position);
		if (attacker instanceof Player) startAxeAttack((Player) attacker, position);
		if (attacker instanceof Player) startLightBladeAttack((Player) attacker, position);
		if (attacker instanceof Player) startBroadswordAttack((Player) attacker, position);
		if (attacker instanceof Player) startWhipAttack((Player) attacker, position);
		if (attacker instanceof Player) startRapierAttack((Player) attacker, position);
		if (attacker instanceof Player) startBluntWeaponAttack((Player) attacker, position);
		if (attacker instanceof Player) startQuarterstaffAttack((Player) attacker, position);
		if (attacker instanceof Player) startHeavyBluntAttack((Player) attacker, position);
		if (attacker instanceof Player) startBarehandedAttack((Player) attacker, position);
		if (attacker instanceof Player) startGauntletAttack((Player) attacker, position);
		if (attacker instanceof Player) startOtherOffhandAttack((Player) attacker, position);

		controllers.effectController.startEffect(
				position
				, VisualEffectCollection.VisualEffectID.redSplash
				, displayValue
				, callback
				, callbackValue);
	}
	
	private static boolean isPoleWeapon(Player player) {
		if (player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon) == null) return false;
		return "pole".equals(player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon).category.id);
	}

	private static float getPoleAttackRotation(Coord attacker, Coord target) {
		return (float) Math.toDegrees(Math.atan2(target.y - attacker.y, target.x - attacker.x)) + 45f;
	}

	private static float getPoleAttackScale(Coord attacker, Coord target) {
		int dx = target.x - attacker.x;
		int dy = target.y - attacker.y;
		return (float) (Math.sqrt((double) dx * dx + (double) dy * dy) / Math.sqrt(2.0));
	}

	private void startLongswordAttack(Player player, Coord target) {
		ItemType mainHand = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon);
		if (!isLongsword(mainHand)) return;

		Coord attacker = player.position;
		float distanceScale = getPoleAttackScale(attacker, target);
		controllers.effectController.startEffect(attacker, VisualEffectCollection.VisualEffectID.longswordAttack, null, null, 0, getPoleAttackRotation(attacker, target), distanceScale, false, target, getLongswordVariation());

		ItemType offHand = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.shield);
		if (isLongsword(offHand)) {
			long firstAnimationDuration = 533L * controllers.preferences.attackspeed_milliseconds / AndorsTrailPreferences.ATTACKSPEED_DEFAULT_MILLISECONDS;
			long delay = Math.max(1L, firstAnimationDuration / 2L);
			controllers.effectController.startEffectAfterDelay(attacker, VisualEffectCollection.VisualEffectID.longswordAttack, null, null, 0, getPoleAttackRotation(attacker, target), distanceScale, true, target, getLongswordVariation(), delay);
		}
	}

	private static boolean isLongsword(ItemType item) {
		return item != null && "lsword".equals(item.category.id);
	}

	private void startAxeAttack(Player player, Coord target) {
		ItemType mainHand = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon);
		if (!isAxe(mainHand)) return;

		Coord attacker = player.position;
		float rotation = getAxeAttackRotation(attacker, target);
		float distanceScale = getPoleAttackScale(attacker, target);
		boolean mirrorAcrossVerticalAxis = target.x <= attacker.x;
		ItemType offHand = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.shield);
		if (isAxe(offHand)) {
			controllers.effectController.startEffect(attacker, VisualEffectCollection.VisualEffectID.dualAxesAttack, null, null, 0, rotation, distanceScale, false, null, 0f, 0f, 0f, mirrorAcrossVerticalAxis);
			controllers.effectController.startEffect(target, VisualEffectCollection.VisualEffectID.groundShockwave, null, null, 0);
		} else {
			controllers.effectController.startEffect(attacker, VisualEffectCollection.VisualEffectID.axeAttack, null, null, 0, rotation, distanceScale, false, null, 0f, 0f, 0f, mirrorAcrossVerticalAxis);
		}

		if (isGreataxe(mainHand) && Constants.rnd.nextBoolean() && controllers.preferences.enableUiAnimations) {
			long shockwaveDelay = 8L * 400L * controllers.preferences.attackspeed_milliseconds
					/ AndorsTrailPreferences.ATTACKSPEED_DEFAULT_MILLISECONDS / 16L;
			controllers.effectController.startEffectAfterDelay(target, VisualEffectCollection.VisualEffectID.groundShockwave, null, null, 0, 0f, 1f, false, null, 0f, Math.max(1L, shockwaveDelay));
		}

	}

	private static float getAxeAttackRotation(Coord attacker, Coord target) {
		return (float) Math.toDegrees(Math.atan2(target.y - attacker.y, Math.abs(target.x - attacker.x))) + 45f;
	}

	private static boolean isAxe(ItemType item) {
		return item != null && ("axe".equals(item.category.id) || "axe2h".equals(item.category.id));
	}

	private static boolean isGreataxe(ItemType item) {
		return item != null && "axe2h".equals(item.category.id);
	}


	private void startBroadswordAttack(Player player, Coord target) {
		ItemType weapon = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon);
		if (!isBroadsword(weapon) && !isTwoHandedSword(weapon) && !isCategory(weapon, "bigtorch")) return;

		Coord attacker = player.position;
		controllers.effectController.startEffect(attacker, VisualEffectCollection.VisualEffectID.broadswordAttack, null, null, 0, getPoleAttackRotation(attacker, target), getPoleAttackScale(attacker, target));
	}

	private static boolean isBroadsword(ItemType item) {
		return item != null && "bsword".equals(item.category.id);
	}

	private static boolean isTwoHandedSword(ItemType item) {
		return item != null && "2hsword".equals(item.category.id);
	}

	private void startWhipAttack(Player player, Coord target) {
		ItemType weapon = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon);
		if (!isWhip(weapon)) return;

		Coord attacker = player.position;
		float rotation = getAxeAttackRotation(attacker, target);
		float distanceScale = getPoleAttackScale(attacker, target);
		boolean mirrorAcrossVerticalAxis = target.x <= attacker.x;
		controllers.effectController.startEffect(attacker, VisualEffectCollection.VisualEffectID.whipAttack, null, null, 0, rotation, distanceScale, false, null, 0f, 0f, 0f, mirrorAcrossVerticalAxis);
		if (controllers.preferences.enableUiAnimations) {
			long impactDelay = 10L * 400L * controllers.preferences.attackspeed_milliseconds
					/ AndorsTrailPreferences.ATTACKSPEED_DEFAULT_MILLISECONDS / 16L;
			controllers.effectController.startEffectAfterDelay(target, VisualEffectCollection.VisualEffectID.bluntImpact, null, null, 0, 0f, 1f, false, null, 0f, Math.max(1L, impactDelay));
		}
	}

	private static boolean isWhip(ItemType item) {
		return item != null && "whip".equals(item.category.id);
	}


	private void startRapierAttack(Player player, Coord target) {
		ItemType weapon = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon);
		if (!isCategory(weapon, "rapier")) return;

		Coord attacker = player.position;
		float offsetX = Constants.rnd.nextFloat() * 0.5f - 0.25f;
		float offsetY = Constants.rnd.nextFloat() * 0.5f - 0.25f;
		controllers.effectController.startEffect(attacker, VisualEffectCollection.VisualEffectID.rapierThrust, null, null, 0,
				getPoleAttackRotation(attacker, target), getPoleAttackScale(attacker, target), false, null, 0f, offsetX, offsetY);
	}

	private void startBluntWeaponAttack(Player player, Coord target) {
		ItemType weapon = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon);
		if (!isCategory(weapon, "club") && !isCategory(weapon, "mace")
				&& !isCategory(weapon, "scepter") && !isCategory(weapon, "hammer")) return;

		startAxeDirectedAttack(player, target, VisualEffectCollection.VisualEffectID.longswordAttack);
		startBluntImpactAtFrame(target, 4, 533, 0L, 1.15f);
	}

	private void startQuarterstaffAttack(Player player, Coord target) {
		ItemType weapon = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon);
		if (!isCategory(weapon, "staff")) return;

		Coord attacker = player.position;
		controllers.effectController.startEffect(attacker, VisualEffectCollection.VisualEffectID.poleAttack, null, null, 0,
				getPoleAttackRotation(attacker, target), getPoleAttackScale(attacker, target));
		startBluntImpactAtFrame(target, 6, 533);
		startBluntImpactAtFrame(target, 12, 533);
	}

	private void startHeavyBluntAttack(Player player, Coord target) {
		ItemType weapon = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon);
		if (!isCategory(weapon, "mace2h") && !isCategory(weapon, "hammer2h")) return;

		Coord attacker = player.position;
		controllers.effectController.startEffect(attacker, VisualEffectCollection.VisualEffectID.broadswordAttack, null, null, 0,
				getAxeAttackRotation(attacker, target) - 30f, getPoleAttackScale(attacker, target),
				false, null, 0f, 0f, 0f, target.x <= attacker.x);
		startBluntImpactAtFrame(target, 7, 533, 0L, 1.2f);
		if (Constants.rnd.nextBoolean()) startGroundShockwaveAtFrame(target, 9, 533);
	}

	private void startBarehandedAttack(Player player, Coord target) {
		if (player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon) != null
				|| player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.shield) != null) return;

		if (Constants.rnd.nextFloat() < 0.3f) {
			controllers.effectController.startEffect(
					target, VisualEffectCollection.VisualEffectID.footSweep, null, null, 0);
		} else {
			float offsetX = Constants.rnd.nextFloat() * 0.25f - 0.125f;
			float offsetY = Constants.rnd.nextFloat() * 0.25f - 0.125f;
			controllers.effectController.startEffect(target, VisualEffectCollection.VisualEffectID.bluntImpact, null, null, 0,
					0f, 1f, false, null, 0f, offsetX, offsetY);
		}
	}

	private void startGauntletAttack(Player player, Coord target) {
		ItemType weapon = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon);
		if (!isCategory(weapon, "gauntlet")) return;

		if (Constants.rnd.nextBoolean()) {
			startAxeDirectedAttack(player, target, VisualEffectCollection.VisualEffectID.longswordAttack);
			startBluntImpactAtFrame(target, 4, 533, 0L, 1.15f);
		} else {
			Coord attacker = player.position;
			controllers.effectController.startEffect(attacker, VisualEffectCollection.VisualEffectID.rapierThrust, null, null, 0,
					getPoleAttackRotation(attacker, target), getPoleAttackScale(attacker, target));
			startBluntImpactAtFrame(target, 8, 267);
		}
	}

	private void startAxeDirectedAttack(Player player, Coord target, VisualEffectCollection.VisualEffectID effectID) {
		Coord attacker = player.position;
		controllers.effectController.startEffect(attacker, effectID, null, null, 0,
				getAxeAttackRotation(attacker, target), getPoleAttackScale(attacker, target),
				false, null, 0f, 0f, 0f, target.x <= attacker.x);
	}

	private void startBluntImpactAtFrame(Coord target, int frame, long animationDuration) {
		startBluntImpactAtFrame(target, frame, animationDuration, 0L);
	}

	private void startBluntImpactAtFrame(Coord target, int frame, long animationDuration, long initialDelay) {
		if (!controllers.preferences.enableUiAnimations) return;
		long delay = initialDelay + frame * animationDuration * controllers.preferences.attackspeed_milliseconds
				/ AndorsTrailPreferences.ATTACKSPEED_DEFAULT_MILLISECONDS / 16L;
		controllers.effectController.startEffectAfterDelay(target, VisualEffectCollection.VisualEffectID.bluntImpact,
				null, null, 0, 0f, 1f, false, null, 0f, Math.max(1L, delay));
	}

	private void startBluntImpactAtFrame(Coord target, int frame, long animationDuration, long initialDelay, float scale) {
		if (!controllers.preferences.enableUiAnimations) return;
		long delay = initialDelay + frame * animationDuration * controllers.preferences.attackspeed_milliseconds
				/ AndorsTrailPreferences.ATTACKSPEED_DEFAULT_MILLISECONDS / 16L;
		controllers.effectController.startEffectAfterDelay(target, VisualEffectCollection.VisualEffectID.bluntImpact,
				null, null, 0, 0f, scale, false, null, 0f, Math.max(1L, delay));
	}

	private void startGroundShockwaveAtFrame(Coord target, int frame, long animationDuration) {
		if (!controllers.preferences.enableUiAnimations) return;
		long delay = frame * animationDuration * controllers.preferences.attackspeed_milliseconds
				/ AndorsTrailPreferences.ATTACKSPEED_DEFAULT_MILLISECONDS / 16L;
		controllers.effectController.startEffectAfterDelay(target, VisualEffectCollection.VisualEffectID.groundShockwave,
				null, null, 0, 0f, 1f, false, null, 0f, Math.max(1L, delay));
	}

	private static boolean isCategory(ItemType item, String categoryID) {
		return item != null && categoryID.equals(item.category.id);
	}


	private void startOtherOffhandAttack(Player player, Coord target) {
		ItemType mainHand = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon);
		ItemType offHand = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.shield);
		if (offHand == null || !offHand.category.isOffhandCapableWeapon()) return;
		if (isAxe(mainHand) && isAxe(offHand)) return;
		if (isLongsword(mainHand) && isLongsword(offHand)) return;

		long delay = getScaledHalfAnimationDuration(mainHand);
		boolean flip = !isAxeStyleWeapon(offHand) || isAxeStyleWeapon(mainHand);
		startOffhandWeaponAttack(offHand, player.position, target, flip, delay);
	}

	private void startOffhandWeaponAttack(ItemType weapon, Coord attacker, Coord target, boolean flip, long delay) {
		float distanceScale = getPoleAttackScale(attacker, target);
		if (isDagger(weapon) || isShortsword(weapon)) {
			boolean cut = isDagger(weapon) ? Constants.rnd.nextFloat() >= 0.6f : Constants.rnd.nextFloat() < 0.6f;
			if (cut) {
				startEffectWithDelay(VisualEffectCollection.VisualEffectID.lightBladeCut, attacker, target, getPoleAttackRotation(attacker, target), distanceScale, flip, target, Constants.rnd.nextInt(360), Constants.rnd.nextFloat() * 0.25f - 0.125f, Constants.rnd.nextFloat() * 0.25f - 0.125f, delay);
			} else {
				startEffectWithDelay(VisualEffectCollection.VisualEffectID.lightBladeThrust, attacker, target, getPoleAttackRotation(attacker, target), distanceScale, flip, null, 0f, 0f, 0f, delay);
			}
		} else if (isCategory(weapon, "rapier")) {
			startEffectWithDelay(VisualEffectCollection.VisualEffectID.rapierThrust, attacker, target, getPoleAttackRotation(attacker, target), distanceScale, flip, null, 0f, Constants.rnd.nextFloat() * 0.5f - 0.25f, Constants.rnd.nextFloat() * 0.5f - 0.25f, delay);
		} else if (isAxe(weapon)) {
			startEffectWithDelay(VisualEffectCollection.VisualEffectID.axeAttack, attacker, target, getAxeAttackRotation(attacker, target), distanceScale, flip, null, 0f, 0f, 0f, delay, target.x <= attacker.x);
		} else if (isLongsword(weapon)) {
			startEffectWithDelay(VisualEffectCollection.VisualEffectID.longswordAttack, attacker, target, getPoleAttackRotation(attacker, target), distanceScale, flip, target, getLongswordVariation(), 0f, 0f, delay);
		} else if (isBroadsword(weapon) || isTwoHandedSword(weapon) || isCategory(weapon, "bigtorch")) {
			startEffectWithDelay(VisualEffectCollection.VisualEffectID.broadswordAttack, attacker, target, getPoleAttackRotation(attacker, target), distanceScale, flip, null, 0f, 0f, 0f, delay);
		} else if (isCategory(weapon, "club") || isCategory(weapon, "mace") || isCategory(weapon, "scepter") || isCategory(weapon, "hammer")) {
			startEffectWithDelay(VisualEffectCollection.VisualEffectID.longswordAttack, attacker, target, getAxeAttackRotation(attacker, target), distanceScale, flip, null, 0f, 0f, 0f, delay, target.x <= attacker.x);
			startBluntImpactAtFrame(target, 4, 533, delay, 1.15f);
		} else if (isWhip(weapon)) {
			startEffectWithDelay(VisualEffectCollection.VisualEffectID.whipAttack, attacker, target, getAxeAttackRotation(attacker, target), distanceScale, flip, null, 0f, 0f, 0f, delay, target.x <= attacker.x);
			startBluntImpactAtFrame(target, 10, 400, delay);
		}
	}

	private void startEffectWithDelay(VisualEffectCollection.VisualEffectID effectID, Coord attacker, Coord target, float rotation, float distanceScale, boolean flip, Coord rotationCenter, float additionalRotation, float offsetX, float offsetY, long delay) {
		startEffectWithDelay(effectID, attacker, target, rotation, distanceScale, flip, rotationCenter, additionalRotation, offsetX, offsetY, delay, false);
	}

	private void startEffectWithDelay(VisualEffectCollection.VisualEffectID effectID, Coord attacker, Coord target, float rotation, float distanceScale, boolean flip, Coord rotationCenter, float additionalRotation, float offsetX, float offsetY, long delay, boolean mirrorAcrossVerticalAxis) {
		if (delay == 0L) {
			controllers.effectController.startEffect(attacker, effectID, null, null, 0, rotation, distanceScale, flip, rotationCenter, additionalRotation, offsetX, offsetY, mirrorAcrossVerticalAxis);
		} else {
			controllers.effectController.startEffectAfterDelay(attacker, effectID, null, null, 0, rotation, distanceScale, flip, rotationCenter, additionalRotation, offsetX, offsetY, delay, mirrorAcrossVerticalAxis);
		}
	}

	private static boolean isAxeStyleWeapon(ItemType item) {
		return isAxe(item) || isWhip(item) || isCategory(item, "club") || isCategory(item, "mace")
				|| isCategory(item, "scepter") || isCategory(item, "hammer");
	}

	private long getScaledHalfAnimationDuration(ItemType item) {
		if (item == null) return 0L;
		long duration = isDagger(item) || isShortsword(item)
				? 400L : isAxe(item) || isCategory(item, "rapier") || isWhip(item) ? 400L : 533L;
		return Math.max(1L, duration * controllers.preferences.attackspeed_milliseconds
				/ AndorsTrailPreferences.ATTACKSPEED_DEFAULT_MILLISECONDS / 2L);
	}

	private void startLightBladeAttack(Player player, Coord target) {
		ItemType weapon = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon);
		if (!isDagger(weapon) && !isShortsword(weapon)) return;

		boolean cut = isDagger(weapon) ? Constants.rnd.nextFloat() >= 0.6f : Constants.rnd.nextFloat() < 0.6f;
		if (cut) {
			float cutRotation = Constants.rnd.nextInt(360);
			float perpendicularOffset = (Constants.rnd.nextFloat() * 8f - 4f) / 32f;
			controllers.effectController.startEffect(player.position, VisualEffectCollection.VisualEffectID.lightBladeCut, null, null, 0, getPoleAttackRotation(player.position, target), getPoleAttackScale(player.position, target), false, target, cutRotation, 0f, perpendicularOffset);
		} else {
			controllers.effectController.startEffect(player.position, VisualEffectCollection.VisualEffectID.lightBladeThrust, null, null, 0, getPoleAttackRotation(player.position, target), getPoleAttackScale(player.position, target));
		}
	}

	private static boolean isDagger(ItemType item) {
		return item != null && "dagger".equals(item.category.id);
	}

	private static boolean isShortsword(ItemType item) {
		return item != null && "ssword".equals(item.category.id);
	}

	private static float getLongswordVariation() {
		return Constants.rnd.nextFloat() * 60f - 30f;
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
	/**
	 * @implNote
	 * formula: 50 * (1 + (2 / pi) * atan((attackChance - blockChance - n) / F))
	 * <br/>
	 * n = {@value n}; F = {@value F}
	 * @return [0..100] . 100 == always hit.
	 */
	private static int getAttackHitChance(final Actor attacker, final Actor target) {
		final int c = attacker.getAttackChance() - target.getBlockChance();
		// (2/pi)*atan(..) will vary from -1 to +1 .
		return (int) (50 * (1 + two_divided_by_PI * (float)Math.atan((float)(c-n) / F)));
	}

	private AttackResult attack(final Actor attacker, final Actor target) {
		int hitChance = getAttackHitChance(attacker, target);
		if (!Constants.roll100(hitChance)) {
			applyAttackMissStatusEffects(attacker, target);
			return AttackResult.MISS;
		}

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

	private void applyAttackMissStatusEffects(Actor attacker, Actor target) {
		ItemTraits_OnUse[] onMissEffects = attacker.getOnMissEffects();
		ItemTraits_OnHitReceived[] onMissReceivedEffects = target.getOnMissReceivedEffects();
		if (onMissEffects != null) {
			for (ItemTraits_OnUse e : onMissEffects) {
				controllers.actorStatsController.applyUseEffect(attacker, target, e);
			}
		}
		if (onMissReceivedEffects != null) {
			for (ItemTraits_OnHitReceived e : onMissReceivedEffects) {
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
