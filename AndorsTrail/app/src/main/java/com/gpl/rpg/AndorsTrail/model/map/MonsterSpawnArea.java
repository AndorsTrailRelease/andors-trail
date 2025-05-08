package com.gpl.rpg.AndorsTrail.model.map;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.Constants;
import com.gpl.rpg.AndorsTrail.model.ChecksumBuilder;
import com.gpl.rpg.AndorsTrail.model.actor.Monster;
import com.gpl.rpg.AndorsTrail.model.actor.MonsterType;
import com.gpl.rpg.AndorsTrail.util.Coord;
import com.gpl.rpg.AndorsTrail.util.CoordRect;
import com.gpl.rpg.AndorsTrail.util.Range;

public final class MonsterSpawnArea {
	public final CoordRect area;
	public final Range quantity;
	private final Range respawnspeed;
	public final String areaID;
	public final String[] monsterTypeIDs;
	public final List<Monster> monsters = new CopyOnWriteArrayList<Monster>();
	public final boolean isUnique; // unique == non-respawnable
	public final boolean ignoreAreas; //Can spawn on other game objects area.
	private final String group;
	public boolean isSpawning;
	public final boolean isSpawningForNewGame;

	public MonsterSpawnArea(
			CoordRect area
			, Range quantity
			, Range respawnspeed
			, String areaID
			, String[] monsterTypeIDs
			, boolean isUnique
			, boolean ignoreAreas
			, String group
			, boolean isSpawningForNewGame
	) {
		this.area = area;
		this.quantity = quantity;
		this.respawnspeed = respawnspeed;
		this.areaID = areaID;
		this.monsterTypeIDs = monsterTypeIDs;
		this.isUnique = isUnique;
		this.ignoreAreas = ignoreAreas;
		this.group = group;
		this.isSpawningForNewGame = isSpawningForNewGame;
		this.isSpawning = isSpawningForNewGame;
	}

	public Monster getMonsterAt(final Coord p) { return getMonsterAt(p.x, p.y); }
	public Monster getMonsterAt(final int x, final int y) {
		for (Monster m : monsters) {
			if (m.rectPosition.contains(x, y)) return m;
		}
		return null;
	}
	public Monster getMonsterAt(final CoordRect p) {
		for (Monster m : monsters) {
			if (m.rectPosition.intersects(p)) return m;
		}
		return null;
	}

	public Monster findSpawnedMonster(String monsterTypeID) {
		for (Monster m : monsters) {
			if (m.getMonsterTypeID().equals(monsterTypeID)) return m;
		}
		return null;
	}

	public void spawn(Coord p, WorldContext context) {
		final String monsterTypeID = monsterTypeIDs[Constants.rnd.nextInt(monsterTypeIDs.length)];
		spawn(p, monsterTypeID, context);
	}
	public MonsterType getRandomMonsterType(WorldContext context) {
		final String monsterTypeID = monsterTypeIDs[Constants.rnd.nextInt(monsterTypeIDs.length)];
		return context.monsterTypes.getMonsterType(monsterTypeID);
	}
	public void spawn(Coord p, String monsterTypeID, WorldContext context) {
		spawn(p, context.monsterTypes.getMonsterType(monsterTypeID));
	}
	public Monster spawn(Coord p, MonsterType type) {
		Monster m = new Monster(type, this);
		m.position.set(p);
		monsters.add(m);
		quantity.current++;
		return m;
	}

	public void remove(Monster m) {
		if (monsters.remove(m)) quantity.current--;
	}

	public boolean isSpawnable(boolean includeUniqueMonsters) {
		if (!isSpawning) return false;
		if (isUnique && !includeUniqueMonsters) return false;
		return quantity.current < quantity.max;
	}

	public boolean rollShouldSpawn() {
		return Constants.rollResult(respawnspeed);
	}

	public void removeAllMonsters() {
		monsters.clear();
		quantity.current = 0;
	}

	public void resetShops() {
		for (Monster m : monsters) {
			m.resetShopItems();
		}
	}

	public void resetForNewGame() {
		removeAllMonsters();
		isSpawning = isSpawningForNewGame;
	}


	// ====== PARCELABLE ===================================================================

	public void readFromParcel(DataInputStream src, WorldContext world, int fileversion) throws IOException {
		monsters.clear();
		isSpawning = isSpawningForNewGame;
		if (fileversion >= 41) isSpawning = src.readBoolean();
		quantity.current = src.readInt();
		for(int i = 0; i < quantity.current; ++i) {
			monsters.add(Monster.newFromParcel(src, world, fileversion, this));
		}
	}

	public void writeToParcel(DataOutputStream dest) throws IOException {
		dest.writeBoolean(isSpawning);
		dest.writeInt(monsters.size());
		for (Monster m : monsters) {
			m.writeToParcel(dest);
		}
	}

	public void addToChecksum(ChecksumBuilder builder) {
		builder.add(isSpawning);
		builder.add(monsters.size());
		for (Monster m : monsters) {
			m.addToChecksum(builder);
		}
	}
}
