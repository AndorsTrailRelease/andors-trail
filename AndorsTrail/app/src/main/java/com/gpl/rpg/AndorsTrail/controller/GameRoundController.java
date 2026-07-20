package com.gpl.rpg.AndorsTrail.controller;

import java.util.EnumSet;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.listeners.GameRoundListeners;
import com.gpl.rpg.AndorsTrail.model.map.MapObject;
import com.gpl.rpg.AndorsTrail.util.L;
import com.gpl.rpg.AndorsTrail.util.TimedMessageTask;

public final class GameRoundController implements TimedMessageTask.Callback {

	public enum PauseReason {
		ACTIVITY_HIDDEN,
		BLOCKING_DIALOG,
		BLOCKING_ACTIVITY,
		MAP_TRANSITION
	}

	private final ControllerContext controllers;
	private final WorldContext world;
	private final TimedMessageTask roundTimer;
	private final EnumSet<PauseReason> activePauses = EnumSet.noneOf(PauseReason.class);
	public final GameRoundListeners gameRoundListeners = new GameRoundListeners();

	public GameRoundController(ControllerContext controllers, WorldContext world) {
		this.controllers = controllers;
		this.world = world;
		this.roundTimer = new TimedMessageTask(this, Constants.TICK_DELAY, true);
		activePauses.add(PauseReason.ACTIVITY_HIDDEN);
		updateTimerState();
	}

	private int ticksUntilNextRound = Constants.TICKS_PER_ROUND;
	private int ticksUntilNextFullRound = Constants.TICKS_PER_FULLROUND;

	@Override
	public boolean onTick(TimedMessageTask task) {
		if (!hasLoadedModel()) return false;
		if (hasPauseReasons()) return false;
		if (!world.model.uiSelections.isMainActivityVisible) return false;
		if (world.model.uiSelections.isInCombat) return false;

		onNewTick();

		--ticksUntilNextRound;
		if (ticksUntilNextRound <= 0) {
			onNewRound();
			restartWaitForNextRound();
		}

		--ticksUntilNextFullRound;
		if (ticksUntilNextFullRound <= 0) {
			onNewFullRound();
			restartWaitForNextFullRound();
		}

		return true;
	}

	public void resetRoundTimers() {
		restartWaitForNextRound();
		restartWaitForNextFullRound();
	}

	/**
	 * Marks the supplied pause reason as active.
	 * In development builds, acquiring the same reason twice is treated as a bug.
	 */
	public void acquirePause(PauseReason reason) {
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) {
			if (activePauses.contains(reason)) {
				String message = "GameRoundController: duplicate acquire for " + reason;
				L.error(message);
				throw new AssertionError(message);
			}
		}
		activePauses.add(reason);
		updateTimerState();
	}

	/**
	 * Clears the supplied pause reason.
	 * Unmatched releases are logged and ignored so the timer cannot be
	 * restarted by accident.
	 */
	public void releasePause(PauseReason reason) {
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) {
			if (!activePauses.contains(reason)) {
				String message = "GameRoundController: unmatched release for " + reason;
				L.error(message);
				throw new AssertionError(message);
			}
		}
		activePauses.remove(reason);
		updateTimerState();
	}

	/**
	 * Marks the gameplay activity as hidden and stops the round timer until the
	 * activity becomes visible again.  This is called when Android pauses the main activity,
	 * such as when the user switches to another app or the device goes to sleep.
	 */
	public void onMainActivityPaused() {
		acquirePause(PauseReason.ACTIVITY_HIDDEN);
	}

	/**
	 * Clears the activity-hidden pause and restores combat state if the player
	 * is returning to an in-progress combat session.
	 */
	public void onMainActivityResumed() {
		releasePause(PauseReason.ACTIVITY_HIDDEN);
		controllers.combatController.resumeCombatIfNeeded();
	}

	/**
	 * Recomputes whether the round timer should run after combat state changes.
	 */
	public void onCombatStateChanged() {
		updateTimerState();
	}

	private void restartWaitForNextFullRound() {
		ticksUntilNextFullRound = Constants.TICKS_PER_FULLROUND;
	}

	private void restartWaitForNextRound() {
		ticksUntilNextRound = Constants.TICKS_PER_ROUND;
	}

	/**
	 * Returns whether any pause reason is currently holding the round timer.
	 */
	private boolean hasPauseReasons() {
		return !activePauses.isEmpty();
	}

	/**
	 * Returns whether the supplied pause reason is currently active.
	 */
	private boolean isPausedFor(PauseReason reason) {
		return activePauses.contains(reason);
	}

	private boolean hasLoadedModel() {
		return world.model != null && world.model.uiSelections != null;
	}

	/**
	 * Synchronizes the derived visibility flag and the round timer with the
	 * current pause reasons and combat state.
	 */
	private void updateTimerState() {
		if (!hasLoadedModel()) {
			roundTimer.stop();
			return;
		}
		world.model.uiSelections.isMainActivityVisible = !isPausedFor(PauseReason.ACTIVITY_HIDDEN);
		if (hasPauseReasons() || world.model.uiSelections.isInCombat) {
			roundTimer.stop();
		} else {
			roundTimer.start();
		}
	}

	public void onNewFullRound() {
		controllers.mapController.resetMapsNotRecentlyVisited();
		controllers.actorStatsController.applyConditionsToMonsters(world.model.currentMaps.map, true);
		controllers.actorStatsController.applyConditionsToPlayer(world.model.player, true);
		gameRoundListeners.onNewFullRound();
	}

	private void onNewRound() {
		onNewMonsterRound();
		onNewPlayerRound();
		gameRoundListeners.onNewRound();
	}
	public void onNewPlayerRound() {
		world.model.worldData.tickWorldTime();
		controllers.actorStatsController.applyConditionsToPlayer(world.model.player, false);
		controllers.actorStatsController.applySkillEffectsForNewRound(world.model.player, world.model.currentMaps.map);
		controllers.mapController.handleMapEvents(world.model.currentMaps.map, world.model.player.position, MapObject.MapObjectEvaluationType.afterEveryRound);
	}
	public void onNewMonsterRound() {
		controllers.actorStatsController.applyConditionsToMonsters(world.model.currentMaps.map, false);
	}

	private void onNewTick() {
		controllers.monsterMovementController.moveMonsters();
		controllers.monsterSpawnController.maybeSpawn(world.model.currentMaps.map, world.model.currentMaps.tileMap);
		controllers.monsterMovementController.attackWithAgressiveMonsters();
		controllers.effectController.updateSplatters(world.model.currentMaps.map);
		controllers.mapController.handleMapEvents(world.model.currentMaps.map, world.model.player.position, MapObject.MapObjectEvaluationType.continuously);
		gameRoundListeners.onNewTick();
	}
}
