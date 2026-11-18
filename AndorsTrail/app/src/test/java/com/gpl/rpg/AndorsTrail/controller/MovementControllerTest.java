package com.gpl.rpg.AndorsTrail.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gpl.rpg.AndorsTrail.util.Coord;
import com.gpl.rpg.AndorsTrail.util.CoordRect;
import com.gpl.rpg.AndorsTrail.util.Size;

public final class MovementControllerTest {

	@Test
	public void findNearestWalkablePositionUsesClosestWalkableTile() {
		boolean[][] walkable = new boolean[3][3];
		walkable[0][2] = true;
		walkable[2][1] = true;

		Coord result = MovementController.findNearestWalkablePosition(walkable, new Coord(0, 1));

		assertEquals(0, result.x);
		assertEquals(2, result.y);
	}

	@Test
	public void findNearestWalkablePositionPrefersArrivalArea() {
		boolean[][] walkable = new boolean[3][2];
		walkable[0][0] = true;
		walkable[2][1] = true;

		Coord result = MovementController.findNearestWalkablePosition(
				walkable, new Coord(1, 1), new CoordRect(new Coord(0, 0), new Size(2, 2)));

		assertEquals(0, result.x);
		assertEquals(0, result.y);
	}

	@Test
	public void findNearestWalkablePositionFallsBackToWholeMapWhenArrivalAreaHasNoWalkableTile() {
		boolean[][] walkable = new boolean[2][1];
		walkable[1][0] = true;

		Coord result = MovementController.findNearestWalkablePosition(
				walkable, new Coord(0, 0), new CoordRect(new Coord(0, 0), new Size(1, 1)));

		assertEquals(1, result.x);
		assertEquals(0, result.y);
	}

	@Test
	public void findNearestWalkablePositionReturnsNullWhenNoTileIsWalkable() {
		boolean[][] walkable = new boolean[2][2];

		assertNull(MovementController.findNearestWalkablePosition(walkable, new Coord(0, 0)));
	}

	@Test
	public void findNearestWalkablePositionReturnsNullForEmptyMap() {
		assertNull(MovementController.findNearestWalkablePosition(new boolean[0][0], new Coord(0, 0)));
	}
}
