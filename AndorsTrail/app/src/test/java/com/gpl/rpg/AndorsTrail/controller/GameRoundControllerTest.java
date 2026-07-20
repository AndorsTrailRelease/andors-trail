package com.gpl.rpg.AndorsTrail.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.model.ModelContainer;

public final class GameRoundControllerTest {

	private static final class FakeRoundTimer implements GameRoundController.RoundTimer {
		private int startCount = 0;
		private int stopCount = 0;
		private boolean running = false;

		@Override
		public void start() {
			startCount++;
			running = true;
		}

		@Override
		public void stop() {
			stopCount++;
			running = false;
		}
	}

	private static GameRoundController createController(WorldContext world, FakeRoundTimer timer) {
		return new GameRoundController(null, world, timer);
	}

	private static WorldContext createLoadedWorld() {
		WorldContext world = new WorldContext();
		world.model = new ModelContainer(1, true);
		return world;
	}

	@Test
	public void constructorIsSafeBeforeModelLoads() {
		WorldContext world = new WorldContext();
		FakeRoundTimer timer = new FakeRoundTimer();

		GameRoundController controller = createController(world, timer);

		assertFalse(controller.onTick(null));
		assertEquals(0, timer.startCount);
		assertEquals(1, timer.stopCount);
		assertFalse(timer.running);
	}

	@Test
	public void releasingActivityHiddenStartsTimerWhenNoOtherPauseExists() {
		WorldContext world = createLoadedWorld();
		FakeRoundTimer timer = new FakeRoundTimer();
		GameRoundController controller = createController(world, timer);

		controller.releasePause(GameRoundController.PauseReason.ACTIVITY_HIDDEN);

		assertTrue(world.model.uiSelections.isMainActivityVisible);
		assertEquals(1, timer.startCount);
		assertTrue(timer.running);
	}

	@Test
	public void mapTransitionReleaseDoesNotRestartTimerWhileBlockingActivityIsActive() {
		WorldContext world = createLoadedWorld();
		FakeRoundTimer timer = new FakeRoundTimer();
		GameRoundController controller = createController(world, timer);
		controller.releasePause(GameRoundController.PauseReason.ACTIVITY_HIDDEN);

		controller.acquirePause(GameRoundController.PauseReason.BLOCKING_ACTIVITY);
		controller.acquirePause(GameRoundController.PauseReason.MAP_TRANSITION);
		controller.releasePause(GameRoundController.PauseReason.MAP_TRANSITION);

		assertFalse(timer.running);
		assertEquals(1, timer.startCount);
		assertEquals(4, timer.stopCount);

		controller.releasePause(GameRoundController.PauseReason.BLOCKING_ACTIVITY);

		assertTrue(timer.running);
		assertEquals(2, timer.startCount);
	}

	@Test
	public void combatStateChangeStopsAndRestartsTimer() {
		WorldContext world = createLoadedWorld();
		FakeRoundTimer timer = new FakeRoundTimer();
		GameRoundController controller = createController(world, timer);
		controller.releasePause(GameRoundController.PauseReason.ACTIVITY_HIDDEN);

		world.model.uiSelections.isInCombat = true;
		controller.onCombatStateChanged();
		assertFalse(timer.running);

		world.model.uiSelections.isInCombat = false;
		controller.onCombatStateChanged();
		assertTrue(timer.running);
		assertEquals(2, timer.startCount);
	}

	@Test(expected = AssertionError.class)
	public void duplicateAcquireFailsFastInDebugBuilds() {
		WorldContext world = createLoadedWorld();
		FakeRoundTimer timer = new FakeRoundTimer();
		GameRoundController controller = createController(world, timer);
		controller.releasePause(GameRoundController.PauseReason.ACTIVITY_HIDDEN);

		controller.acquirePause(GameRoundController.PauseReason.BLOCKING_DIALOG);
		controller.acquirePause(GameRoundController.PauseReason.BLOCKING_DIALOG);
	}

	@Test(expected = AssertionError.class)
	public void unmatchedReleaseFailsFastInDebugBuilds() {
		WorldContext world = createLoadedWorld();
		FakeRoundTimer timer = new FakeRoundTimer();
		GameRoundController controller = createController(world, timer);
		controller.releasePause(GameRoundController.PauseReason.ACTIVITY_HIDDEN);

		controller.releasePause(GameRoundController.PauseReason.MAP_TRANSITION);
	}
}
