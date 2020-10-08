package com.gpl.rpg.AndorsTrail.controller;

import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.util.Coord;

public final class InputController implements OnClickListener, OnLongClickListener{
	private final ControllerContext controllers;
	private final WorldContext world;

	private final Coord lastTouchPosition_tileCoords = new Coord();
	private int lastTouchPosition_dx = 0;
	private int lastTouchPosition_dy = 0;
	private long lastTouchEventTime = 0;
	private boolean isDpadActive = false;

	public InputController(ControllerContext controllers, WorldContext world) {
		this.controllers = controllers;
		this.world = world;
	}

	public boolean onKeyboardAction(int keyCode) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_UP:
		case KeyEvent.KEYCODE_W:
			onRelativeMovement(0, -1);
			return true;
		case KeyEvent.KEYCODE_DPAD_DOWN:
		case KeyEvent.KEYCODE_S:
			onRelativeMovement(0, 1);
			return true;
		case KeyEvent.KEYCODE_DPAD_LEFT:
		case KeyEvent.KEYCODE_A:
			onRelativeMovement(-1, 0);
			return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
		case KeyEvent.KEYCODE_D:
			onRelativeMovement(1, 0);
			return true;
		case KeyEvent.KEYCODE_DPAD_CENTER:
		case KeyEvent.KEYCODE_SPACE:
			onRelativeMovement(0, 0);
			return true;
		default:
			return false;
		}
	}

	public void onRelativeMovement(int dx, int dy) {
		if (!allowInputInterval()) return;
		if (world.model.uiSelections.isInCombat) {
			controllers.combatController.executeMoveAttack(dx, dy);
		} else {
			controllers.movementController.startMovement(dx, dy, null);
		}
	}

	public void onKeyboardCancel() {
		controllers.movementController.stopMovement();
	}

	@Override
	public void onClick(View arg0) {
		if (!world.model.uiSelections.isInCombat) return;
		onRelativeMovement(lastTouchPosition_dx, lastTouchPosition_dy);
	}

	@Override
	public boolean onLongClick(View arg0) {
		if (world.model.uiSelections.isInCombat) {
			//TODO: Should be able to mark positions far away (mapwalk / ranged combat)
			if (lastTouchPosition_dx == 0 && lastTouchPosition_dy == 0) return false;
			if (Math.abs(lastTouchPosition_dx) > 1) return false;
			if (Math.abs(lastTouchPosition_dy) > 1) return false;

			controllers.combatController.setCombatSelection(lastTouchPosition_tileCoords);
			return true;
		}
		return false;
	}

	private boolean allowInputInterval() {
		final long now = System.currentTimeMillis();
		if ((now - lastTouchEventTime) < Constants.MINIMUM_INPUT_INTERVAL) return false;
		lastTouchEventTime = now;
		return true;
	}

	public void setDpadActive(boolean isDpadActive) {
		this.isDpadActive = isDpadActive;
	}

	public void onTouchCancel() {
		controllers.movementController.stopMovement();
	}

	public boolean onTouchedTile(int tile_x, int tile_y) {
		lastTouchPosition_tileCoords.set(tile_x, tile_y);
		lastTouchPosition_dx = tile_x - world.model.player.position.x;
		lastTouchPosition_dy = tile_y - world.model.player.position.y;

		if (world.model.uiSelections.isInCombat || isDpadActive) return false;

		controllers.movementController.startMovement(lastTouchPosition_dx, lastTouchPosition_dy, lastTouchPosition_tileCoords);
		return true;
	}
}
