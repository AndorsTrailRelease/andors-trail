package com.gpl.rpg.AndorsTrail.controller;

import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.gpl.rpg.AndorsTrail.AndorsTrailPreferences;
import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.GameRoundController.PauseReason;
import com.gpl.rpg.AndorsTrail.controller.listeners.PlayerMovementListeners;
import com.gpl.rpg.AndorsTrail.model.MapBundle;
import com.gpl.rpg.AndorsTrail.model.ModelContainer;
import com.gpl.rpg.AndorsTrail.model.actor.Monster;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.model.item.Loot;
import com.gpl.rpg.AndorsTrail.model.map.LayeredTileMap;
import com.gpl.rpg.AndorsTrail.model.map.MapObject;
import com.gpl.rpg.AndorsTrail.model.map.MonsterSpawnArea;
import com.gpl.rpg.AndorsTrail.model.map.PredefinedMap;
import com.gpl.rpg.AndorsTrail.model.map.TMXMapTranslator;
import com.gpl.rpg.AndorsTrail.resource.tiles.TileCollection;
import com.gpl.rpg.AndorsTrail.util.Coord;
import com.gpl.rpg.AndorsTrail.util.L;
import com.gpl.rpg.AndorsTrail.util.TimedMessageTask;

public final class MovementController implements TimedMessageTask.Callback {
	private final ControllerContext controllers;
	private final WorldContext world;
	//TODO restore final modifier before release
	private TimedMessageTask movementHandler;
	private static final ExecutorService executor = Executors.newSingleThreadExecutor();
	private static final Handler mainHandler = new Handler(Looper.getMainLooper());

	public final PlayerMovementListeners playerMovementListeners = new PlayerMovementListeners();

	public MovementController(ControllerContext controllers, WorldContext world) {
		this.controllers = controllers;
		this.world = world;
		this.movementHandler = new TimedMessageTask(this, Constants.MINIMUM_INPUT_INTERVAL, false);
	}
	
	//TODO remove this method before release
	public void resetMovementHandler() {
		this.movementHandler.stop();
		this.movementHandler = new TimedMessageTask(this, Constants.MINIMUM_INPUT_INTERVAL, false);
	}

	public void placePlayerAsyncAt(final MapObject.MapObjectType objectType, final String mapName, final String placeName, final int offset_x, final int offset_y) {
		controllers.gameRoundController.pause();
		// This should run pretty quickly, so we won't worry about canceling if activity closes
		executor.execute(() -> {
            stopMovement();
            placePlayerAt(controllers.getResources(), objectType, mapName, placeName, offset_x, offset_y);
            mainHandler.post(() -> {
                stopMovement();
                playerMovementListeners.onPlayerEnteredNewMap(world.model.currentMaps.map, world.model.player.position);
                controllers.gameRoundController.resume();
            });
        });
	}

	public void placePlayerAt(final Resources res, MapObject.MapObjectType objectType, String mapName, String placeName, int offset_x, int offset_y) {
		if (mapName == null || placeName == null) return;
		PredefinedMap newMap = world.maps.findPredefinedMap(mapName);
		if (newMap == null) {
			L.log("Cannot find map " + mapName);
			return;
		}
		MapObject place = newMap.findEventObject(objectType, placeName);
		if (place == null) {
			L.log("Cannot find place " + placeName + " of type " + objectType + " in map " + mapName);
			return;
		}
		if (!place.isActive) {
			L.log("Place " + placeName + " of type " + objectType + " in map " + mapName + " cannot be used as it is inactive");
			return;
		}
		final ModelContainer model = world.model;

		if (model.currentMaps.map != null) model.currentMaps.map.updateLastVisitTime();
		model.player.position.set(place.position.topLeft);
		model.player.position.x += Math.min(offset_x, place.position.size.width-1);
		model.player.position.y += Math.min(offset_y, place.position.size.height-1);
		model.player.lastPosition.set(model.player.position);

		if (!newMap.visited) {
			playerVisitsMapFirstTime(newMap);
		}

		prepareMapAsCurrentMap(newMap, res, true);
	}

	private void playerVisitsMapFirstTime(PredefinedMap m) {
		m.createAllContainerLoot();
		world.maps.worldMapRequiresUpdate = true;
	}

	public void prepareMapAsCurrentMap(PredefinedMap newMap, Resources res, boolean spawnMonsters) {
		final ModelContainer model = world.model;
		MapBundle newMaps = new MapBundle();
		newMaps.map = newMap;

		LayeredTileMap mapTiles = TMXMapTranslator.readLayeredTileMap(res, world.tileManager.tileCache, newMaps.map);
		mapTiles.changeColorFilter(newMaps.map.currentColorFilter);
		TileCollection cachedTiles = world.tileManager.loadTilesFor(newMaps.map, mapTiles, world, res);
		newMaps.tileMap = mapTiles;
		newMaps.tiles = cachedTiles;
		world.tileManager.cacheAdjacentMaps(res, world, newMaps.map);
		world.model.currentMaps = newMaps;

		//Apply replacements before spawning, so that MonsterSpawnArea's isActive variable is up to date.
		controllers.mapController.applyCurrentMapReplacements(res, false);
		if (spawnMonsters) {
			if (!newMap.isRecentlyVisited()) {
				controllers.monsterSpawnController.spawnAll(newMap, model.currentMaps.tileMap);
			}
		}
		controllers.mapController.prepareScriptsOnCurrentMap();
		newMap.visited = true;
		newMap.updateLastVisitTime();
		moveBlockedActors(newMap, model.currentMaps.tileMap);
		refreshMonsterAggressiveness(newMap, model.player);
		controllers.effectController.updateSplatters(newMap);
		WorldMapController.updateWorldMap(controllers.getContext(), world, res);
	}

	private boolean mayMovePlayer() {
		return !world.model.uiSelections.isInCombat;
	}

	private void movePlayer(int dx, int dy) {
		if (dx == 0 && dy == 0) return;
		if (!mayMovePlayer()) return;

		if (!findWalkablePosition(dx, dy)) return;

		Monster m = world.model.currentMaps.map.getMonsterAt(world.model.player.nextPosition);
		if (m != null) {
			controllers.mapController.steppedOnMonster(m, world.model.player.nextPosition);
			return;
		}

		moveToNextIfPossible();
	}

	private boolean findWalkablePosition(int dx, int dy) {
		// try to move with movementAggresiveness, if that fails fall back to MOVEMENTAGGRESSIVENESS_NORMAL
		if (findWalkablePosition(dx, dy, controllers.preferences.movementAggressiveness)) return true;

		if (controllers.preferences.movementAggressiveness == AndorsTrailPreferences.MOVEMENTAGGRESSIVENESS_NORMAL) return false;

		return findWalkablePosition(dx, dy, AndorsTrailPreferences.MOVEMENTAGGRESSIVENESS_NORMAL);
	}

	public boolean findWalkablePosition(int dx, int dy, int aggressiveness) {
		if (controllers.preferences.movementMethod == AndorsTrailPreferences.MOVEMENTMETHOD_STRAIGHT) {
			return findWalkablePosition_straight(dx, dy, aggressiveness);
		} else {
			return findWalkablePosition_directional(dx, dy, aggressiveness);
		}
	}
	private boolean findWalkablePosition_straight(int dx, int dy, int aggressiveness) {
		if (tryWalkablePosition(sgn(dx), sgn(dy), aggressiveness)) return true;				// try moving into the direction player is pointing at
		if (dx == 0 || dy == 0) return false;												// if moving purely east, west, north or south failed - do nothing
		if (abs(dx) == abs(dy) && tryWalkablePosition(sgn(dx), 0, aggressiveness)) return true; // try moving horizontally or vertically otherwise (prefer the direction where he is pointing more)
		if (abs(dx) > abs(dy)) return tryWalkablePosition(sgn(dx), 0, aggressiveness);
		return tryWalkablePosition(0, sgn(dy), aggressiveness);
	}
	private boolean findWalkablePosition_directional(int dx, int dy, int aggressiveness) {
		if (tryWalkablePosition(sgn(dx), sgn(dy), aggressiveness)) return true; // try moving into the direction player is pointing at

		if (dx == 0) { // player wants to move north or south but there is an obstacle
			if (tryWalkablePosition( 1, sgn(dy), aggressiveness)) return true; // try moving north-east (or south-east)
			if (tryWalkablePosition(-1, sgn(dy), aggressiveness)) return true; // try moving north-west (or south-west)
			return false;
		}

		if (dy == 0) { // player wants to move east or west but there is an obstacle
			if (tryWalkablePosition(sgn(dx), 1, aggressiveness)) return true; // try moving north-east (or north-west)
			if (tryWalkablePosition(sgn(dx),-1, aggressiveness)) return true; // try moving south-east (or south-west)
			return false;
		}

		if (abs(dx) >= abs(dy)) { // player wants to move more horizontally
			if (tryWalkablePosition(sgn(dx), 0, aggressiveness)) return true; // try moving horizontally
			if (tryWalkablePosition(0, sgn(dy), aggressiveness)) return true; // try moving vertically
			return false;
		} else { // player wants to move more vertically
			if (tryWalkablePosition(0, sgn(dy), aggressiveness)) return true; // try moving vertically
			if (tryWalkablePosition(sgn(dx), 0, aggressiveness)) return true; // try moving horizontally
			return false;
		}
	}

	private boolean tryWalkablePosition(int dx, int dy, int aggressiveness) {
		final Player player = world.model.player;
		player.nextPosition.set(
				player.position.x + dx
				,player.position.y + dy
			);

		if (!world.model.currentMaps.tileMap.isWalkable(player.nextPosition)) return false;

		// allow player to enter every field when he is NORMAL
		// prevent player from entering "non-monster-fields" when he is AGGRESSIVE
		// prevent player from entering "monster-fields" when he is DEFENSIVE
		if (aggressiveness == AndorsTrailPreferences.MOVEMENTAGGRESSIVENESS_NORMAL) return true;

		Monster m = world.model.currentMaps.map.getMonsterAt(player.nextPosition);
		if (m != null && !m.isAgressive(player)) return true; // avoid MOVEMENTAGGRESSIVENESS settings for NPCs

		if (aggressiveness == AndorsTrailPreferences.MOVEMENTAGGRESSIVENESS_AGGRESSIVE && m == null) return false;
		if (aggressiveness == AndorsTrailPreferences.MOVEMENTAGGRESSIVENESS_DEFENSIVE && m != null) return false;

		return true;
	}

	private static int sgn(final int v) {
		if (v == 0) return 0;
		else if (v > 0) return 1;
		else return -1;
	}
	private static int abs(final int v) {
		if (v == 0) return 0;
		else if (v > 0) return v;
		else return -v;
	}

	public void moveToNextIfPossible() {
		final Player player = world.model.player;
		final PredefinedMap currentMap = world.model.currentMaps.map;
		final Coord newPosition = player.nextPosition;

		for (MapObject o : currentMap.eventObjects) {
			if (!o.isActive) continue;
			if (o.type == MapObject.MapObjectType.keyarea) {
				if (o.position.contains(newPosition)) {
					if (!controllers.mapController.canEnterKeyArea(o)) return;
				}
			}
		}

		player.lastPosition.set(player.position);
		player.position.set(newPosition);
		controllers.combatController.setCombatSelection(null, null);
		
		controllers.effectController.startActorMoveEffect(player, currentMap, player.lastPosition, newPosition, (int) (Constants.MINIMUM_INPUT_INTERVAL / 2), new VisualEffectController.VisualEffectCompletedCallback() {
			
			@Override
			public void onVisualEffectCompleted(int callbackValue) {

				playerMovementListeners.onPlayerMoved(currentMap, newPosition, player.lastPosition);
				controllers.mapController.handleMapEventsAfterMovement(currentMap, newPosition, player.lastPosition);

				if (!world.model.uiSelections.isInCombat) {
					//currentMap can be outdated due to mapchange events processed above.
					Loot loot = world.model.currentMaps.map.getBagAt(newPosition);
					if (loot != null) controllers.itemController.playerSteppedOnLootBag(loot);
				}
			}
		}, 0);
		
		
	}

	public void respawnPlayer(Resources res) {
		placePlayerAt(res, MapObject.MapObjectType.rest, world.model.player.getSpawnMap(), world.model.player.getSpawnPlace(), 0, 0);
		playerMovementListeners.onPlayerEnteredNewMap(world.model.currentMaps.map, world.model.player.position);
	}
	public void respawnPlayerAsync() {
		placePlayerAsyncAt(MapObject.MapObjectType.rest, world.model.player.getSpawnMap(), world.model.player.getSpawnPlace(), 0, 0);
	}

	public void moveBlockedActors(PredefinedMap map, LayeredTileMap tileMap) {
		final ModelContainer model = world.model;

		// If the player somehow spawned on an unwalkable tile, we move the player to the first mapchange area.
		// This could happen if we change some tile to non-walkable in a future version.
		if (!tileMap.isWalkable(model.player.position)) {
			Coord p = getFirstMapChangeAreaPosition(map);
			if (p != null) model.player.position.set(p);
		}

		// If any monsters somehow spawned on an unwalkable tile, we move the monster to a new position on the spawnarea
		// This could happen if we change some tile to non-walkable in a future version.
		Coord playerPosition = model.player.position;
		for (MonsterSpawnArea a : map.spawnAreas) {
			for (Monster m : a.monsters) {
				if (tileMap.isWalkable(m.rectPosition)) continue;
				Coord p = MonsterSpawningController.getRandomFreePosition(map, tileMap, a, m.tileSize, playerPosition);
				if (p == null) continue;
				m.position.set(p);
			}
		}

		// Move ground bags that are are placed on unwalkable tiles.
		// This could happen if we change some tile to non-walkable in a future version.
		for (Loot bag : map.groundBags) {
			if (tileMap.isWalkable(bag.position)) continue;
			Coord p = getFirstMapChangeAreaPosition(map);
			if (p == null) continue;
			if (tileMap.isWalkable(new Coord(p.x+1, p.y  ))) bag.position.set(p.x+1, p.y  );
			else if (tileMap.isWalkable(new Coord(p.x  , p.y+1))) bag.position.set(p.x  , p.y+1);
			else if (tileMap.isWalkable(new Coord(p.x-1, p.y  ))) bag.position.set(p.x-1, p.y  );
			else if (tileMap.isWalkable(new Coord(p.x  , p.y-1))) bag.position.set(p.x  , p.y-1);
		}
	}

	private static Coord getFirstMapChangeAreaPosition(PredefinedMap map) {
		for (MapObject o : map.eventObjects) {
			if (!o.isActive) continue;
			if (o.type == MapObject.MapObjectType.newmap) return o.position.topLeft;
		}
		return null;
	}

	private int movementDx;
	private int movementDy;
	public void startMovement(int dx, int dy, Coord destination) {
		if (!mayMovePlayer()) return;
		if (dx == 0 && dy == 0) return;

		movementDx = dx;
		movementDy = dy;
		movementHandler.start();
	}

	public void stopMovement() {
		movementHandler.stop();
	}

	@Override
	public boolean onTick(TimedMessageTask task) {
		if (!world.model.uiSelections.isMainActivityVisible) return false;
		if (world.model.uiSelections.isInCombat) return false;

		movePlayer(movementDx, movementDy);

		return true;
	}

	public static void refreshMonsterAggressiveness(final PredefinedMap map, final Player player) {
//	Faction-related agressiveness now dynamic, and unrelated to "forceAgressive".
//		for(MonsterSpawnArea a : map.spawnAreas) {
//			for (Monster m : a.monsters) {
//				String faction = m.getFaction();
//				if (faction == null) continue;
//				if (player.getAlignment(faction) < 0) m.forceAggressive();
//			}
//		}
	}

	public static boolean hasAdjacentAggressiveMonster(PredefinedMap map, Player player) {
		return getAdjacentAggressiveMonster(map, player) != null;
	}
	public static Monster getAdjacentAggressiveMonster(PredefinedMap map, Player player) {
		for (MonsterSpawnArea a : map.spawnAreas) {
			for (Monster m : a.monsters) {
				if (!m.isAgressive(player)) continue;
				if (m.isAdjacentTo(player)) return m;
			}
		}
		return null;
	}
}
