package com.gpl.rpg.AndorsTrail.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.junit.Test;

import com.gpl.rpg.AndorsTrail.model.actor.Monster;
import com.gpl.rpg.AndorsTrail.model.actor.MonsterType;
import com.gpl.rpg.AndorsTrail.model.map.LayeredTileMap;
import com.gpl.rpg.AndorsTrail.model.map.MapObject;
import com.gpl.rpg.AndorsTrail.model.map.MapSection;
import com.gpl.rpg.AndorsTrail.model.map.MonsterSpawnArea;
import com.gpl.rpg.AndorsTrail.model.map.PredefinedMap;
import com.gpl.rpg.AndorsTrail.util.Coord;
import com.gpl.rpg.AndorsTrail.util.CoordRect;
import com.gpl.rpg.AndorsTrail.util.Range;
import com.gpl.rpg.AndorsTrail.util.Size;

public final class MovementControllerTest {

	@Test
	public void findNearestWalkableUnoccupiedPositionUsesClosestWalkableTile() {
		boolean[][] walkable = new boolean[3][3];
		walkable[0][2] = true;
		walkable[2][1] = true;

		Coord result = MovementController.findNearestWalkableUnoccupiedPosition(
				createMap(3, 3, null), createTileMap(walkable), new Coord(0, 1));

		assertEquals(0, result.x);
		assertEquals(2, result.y);
	}

	@Test
	public void findNearestWalkableUnoccupiedPositionSkipsOccupiedTiles() {
		boolean[][] walkable = new boolean[2][3];
		walkable[0][2] = true;
		walkable[1][1] = true;

		Coord result = MovementController.findNearestWalkableUnoccupiedPosition(
				createMap(2, 3, new Coord(0, 2)), createTileMap(walkable), new Coord(0, 1));

		assertEquals(1, result.x);
		assertEquals(1, result.y);
	}

	@Test
	public void findNearestWalkableUnoccupiedPositionMovesOffOccupiedOrigin() {
		boolean[][] walkable = new boolean[2][1];
		walkable[0][0] = true;
		walkable[1][0] = true;

		Coord result = MovementController.findNearestWalkableUnoccupiedPosition(
				createMap(2, 1, new Coord(0, 0)), createTileMap(walkable), new Coord(0, 0));

		assertEquals(1, result.x);
		assertEquals(0, result.y);
	}

	@Test
	public void findNearestWalkableUnoccupiedPositionPrefersArrivalArea() {
		boolean[][] walkable = new boolean[3][2];
		walkable[0][0] = true;
		walkable[2][1] = true;

		Coord result = MovementController.findNearestWalkableUnoccupiedPosition(
				createMap(3, 2, null), createTileMap(walkable), new Coord(1, 1),
				new CoordRect(new Coord(0, 0), new Size(2, 2)));

		assertEquals(0, result.x);
		assertEquals(0, result.y);
	}

	@Test
	public void findNearestWalkableUnoccupiedPositionFallsBackToWholeMapWhenArrivalAreaHasNoCandidate() {
		boolean[][] walkable = new boolean[2][1];
		walkable[1][0] = true;

		Coord result = MovementController.findNearestWalkableUnoccupiedPosition(
				createMap(2, 1, null), createTileMap(walkable), new Coord(0, 0),
				new CoordRect(new Coord(0, 0), new Size(1, 1)));

		assertEquals(1, result.x);
		assertEquals(0, result.y);
	}

	@Test
	public void findNearestWalkableUnoccupiedPositionReturnsNullWhenNoTileIsAvailable() {
		boolean[][] walkable = new boolean[2][2];
		walkable[0][0] = true;

		assertNull(MovementController.findNearestWalkableUnoccupiedPosition(
				createMap(2, 2, new Coord(0, 0)), createTileMap(walkable), new Coord(0, 0)));
	}

	@Test
	public void findNearestWalkableUnoccupiedPositionReturnsNullWhenMapHasNoWalkableTiles() {
		boolean[][] walkable = new boolean[1][1];
		assertNull(MovementController.findNearestWalkableUnoccupiedPosition(
				createMap(1, 1, null), createTileMap(walkable), new Coord(0, 0)));
	}

	private static LayeredTileMap createTileMap(boolean[][] walkable) {
		Size size = new Size(walkable.length, walkable[0].length);
		MapSection layout = new MapSection(null, null, null, null, null, walkable, new byte[0]);
		return new LayeredTileMap(size, layout, null, LayeredTileMap.ColorFilterId.none,
				Collections.<Integer>emptyList());
	}

	private static PredefinedMap createMap(int width, int height, Coord npcPosition) {
		MonsterSpawnArea spawnArea = new MonsterSpawnArea(
				new CoordRect(new Coord(0, 0), new Size(width, height)),
				new Range(0, 0),
				new Range(0, 0),
				"test",
				new String[0],
				false,
				false,
				"",
				true);
		if (npcPosition != null) {
			MonsterType monsterType = new MonsterType(
					"npc", "NPC", "", 0, null, null, false, null,
					MonsterType.MonsterClass.humanoid,
					MonsterType.AggressionType.none,
					new Size(1, 1),
					0, 0, 1, 1, 1, 1, 0, 0, 1.0f, null, 0, 0,
					null, null, null);
			Monster npc = new Monster(monsterType, spawnArea);
			npc.position.set(npcPosition);
			spawnArea.monsters.add(npc);
		}
		return new PredefinedMap(
				0, "test", new Size(width, height), new MapObject[0],
				new MonsterSpawnArea[] { spawnArea }, Collections.<String>emptyList(), true, null);
	}
}
