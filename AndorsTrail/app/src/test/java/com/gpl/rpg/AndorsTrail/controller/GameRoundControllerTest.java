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

	private static final class FakeCombatResumeHook implements GameRoundController.CombatResumeHook {
		private int calls = 0;

		@Override
		public void resumeCombatIfNeeded() {
			calls++;
		}

		private void reset() {
			calls = 0;
		}
	}

	private static GameRoundController createController(WorldContext world, FakeRoundTimer timer, FakeCombatResumeHook hook) {
		return new GameRoundController(null, world, hook, timer);
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
		FakeCombatResumeHook hook = new FakeCombatResumeHook();

		GameRoundController controller = createController(world, timer, hook);

		assertFalse(controller.onTick(null));
		assertEquals(0, timer.startCount);
		assertEquals(1, timer.stopCount);
		assertFalse(timer.running);
	}

	@Test
	public void releasingActivityHiddenStartsTimerWhenNoOtherPauseExists() {
		WorldContext world = createLoadedWorld();
		FakeRoundTimer timer = new FakeRoundTimer();
		FakeCombatResumeHook hook = new FakeCombatResumeHook();
		GameRoundController controller = createController(world, timer, hook);
		controller.releasePause(GameRoundController.PauseReason.ACTIVITY_HIDDEN);

		assertTrue(world.model.uiSelections.isMainActivityVisible);
		assertEquals(1, timer.startCount);
		assertTrue(timer.running);
		assertEquals(1, hook.calls);
	}

	@Test
	public void mapTransitionReleaseDoesNotRestartTimerWhileBlockingActivityIsActive() {
		WorldContext world = createLoadedWorld();
		FakeRoundTimer timer = new FakeRoundTimer();
		FakeCombatResumeHook hook = new FakeCombatResumeHook();
		GameRoundController controller = createController(world, timer, hook);
		controller.releasePause(GameRoundController.PauseReason.ACTIVITY_HIDDEN);
		hook.reset();

		controller.acquirePause(GameRoundController.PauseReason.BLOCKING_ACTIVITY);
		controller.acquirePause(GameRoundController.PauseReason.MAP_TRANSITION);
		controller.releasePause(GameRoundController.PauseReason.MAP_TRANSITION);

		assertFalse(timer.running);
		assertEquals(1, timer.startCount);
		assertEquals(4, timer.stopCount);
		assertEquals(0, hook.calls);

		controller.releasePause(GameRoundController.PauseReason.BLOCKING_ACTIVITY);

		assertTrue(timer.running);
		assertEquals(2, timer.startCount);
		assertEquals(1, hook.calls);
	}

	@Test
	public void combatStateChangeStopsAndRestartsTimer() {
		WorldContext world = createLoadedWorld();
		FakeRoundTimer timer = new FakeRoundTimer();
		FakeCombatResumeHook hook = new FakeCombatResumeHook();
		GameRoundController controller = createController(world, timer, hook);
		controller.releasePause(GameRoundController.PauseReason.ACTIVITY_HIDDEN);
		hook.reset();

		world.model.uiSelections.isInCombat = true;
		controller.onCombatStateChanged();
		assertFalse(timer.running);

		world.model.uiSelections.isInCombat = false;
		controller.onCombatStateChanged();
		assertTrue(timer.running);
		assertEquals(2, timer.startCount);
	}

	@Test
	public void blockingDialogClearsMainActivityVisibilityUntilReleased() {
		WorldContext world = createLoadedWorld();
		FakeRoundTimer timer = new FakeRoundTimer();
		FakeCombatResumeHook hook = new FakeCombatResumeHook();
		GameRoundController controller = createController(world, timer, hook);
		controller.releasePause(GameRoundController.PauseReason.ACTIVITY_HIDDEN);
		hook.reset();

		controller.acquirePause(GameRoundController.PauseReason.BLOCKING_DIALOG);

		assertFalse(world.model.uiSelections.isMainActivityVisible);
		assertFalse(timer.running);

		controller.releasePause(GameRoundController.PauseReason.BLOCKING_DIALOG);

		assertTrue(world.model.uiSelections.isMainActivityVisible);
		assertTrue(timer.running);
		assertEquals(1, hook.calls);
	}

	@Test(expected = AssertionError.class)
	public void duplicateAcquireFailsFastInDebugBuilds() {
		WorldContext world = createLoadedWorld();
		FakeRoundTimer timer = new FakeRoundTimer();
		FakeCombatResumeHook hook = new FakeCombatResumeHook();
		GameRoundController controller = createController(world, timer, hook);
		controller.releasePause(GameRoundController.PauseReason.ACTIVITY_HIDDEN);
		hook.reset();

		controller.acquirePause(GameRoundController.PauseReason.BLOCKING_DIALOG);
		controller.acquirePause(GameRoundController.PauseReason.BLOCKING_DIALOG);
	}

	@Test(expected = AssertionError.class)
	public void unmatchedReleaseFailsFastInDebugBuilds() {
		WorldContext world = createLoadedWorld();
		FakeRoundTimer timer = new FakeRoundTimer();
		FakeCombatResumeHook hook = new FakeCombatResumeHook();
		GameRoundController controller = createController(world, timer, hook);
		controller.releasePause(GameRoundController.PauseReason.ACTIVITY_HIDDEN);
		hook.reset();

		controller.releasePause(GameRoundController.PauseReason.MAP_TRANSITION);
	}
}
