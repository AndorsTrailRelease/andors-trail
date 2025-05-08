package com.gpl.rpg.AndorsTrail.model.map;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.Constants;
import com.gpl.rpg.AndorsTrail.controller.VisualEffectController.BloodSplatter;
import com.gpl.rpg.AndorsTrail.model.ChecksumBuilder;
import com.gpl.rpg.AndorsTrail.model.actor.Monster;
import com.gpl.rpg.AndorsTrail.model.item.ItemType;
import com.gpl.rpg.AndorsTrail.model.item.Loot;
import com.gpl.rpg.AndorsTrail.model.map.MapObject.MapObjectType;
import com.gpl.rpg.AndorsTrail.util.Coord;
import com.gpl.rpg.AndorsTrail.util.CoordRect;
import com.gpl.rpg.AndorsTrail.util.L;
import com.gpl.rpg.AndorsTrail.util.Size;

public final class PredefinedMap {
	private static final long VISIT_RESET = 0;

	public final int xmlResourceId;
	public final String name;
	public final Size size;
	public final MapObject[] eventObjects;
	public final MonsterSpawnArea[] spawnAreas;
	public final List<String> initiallyActiveMapObjectGroups;
	public final List<String> activeMapObjectGroups;
	public final ArrayList<Loot> groundBags = new ArrayList<Loot>();
	public final String initialColorFilter;
	public boolean visited = false;
	public long lastVisitTime = VISIT_RESET;
	public String lastSeenLayoutHash = "";
	public final boolean isOutdoors;
	public String currentColorFilter = null;

	public final ArrayList<BloodSplatter> splatters = new ArrayList<BloodSplatter>();

	public PredefinedMap(
			int xmlResourceId
			, String name
			, Size size
			, MapObject[] eventObjects
			, MonsterSpawnArea[] spawnAreas
			, List<String> initiallyActiveMapObjectGroups
			, boolean isOutdoors
			, String colorFilter
	) {
		this.xmlResourceId = xmlResourceId;
		this.name = name;
		this.size = size;
		this.eventObjects = eventObjects;
		this.spawnAreas = spawnAreas;
		this.initiallyActiveMapObjectGroups = initiallyActiveMapObjectGroups;
		this.activeMapObjectGroups = new LinkedList<String>();
		this.activeMapObjectGroups.addAll(this.initiallyActiveMapObjectGroups);
		activateMapObjects();
		assert(size.width > 0);
		assert(size.height > 0);
		this.isOutdoors = isOutdoors;
		this.initialColorFilter = colorFilter;

		if (AndorsTrailApplication.DEVELOPMENT_VALIDATEDATA) {
			for (int i = 0; i < spawnAreas.length; i++) {
				for (int j = i + 1; j < spawnAreas.length; j++) {
					if (spawnAreas[i].areaID.equals(spawnAreas[j].areaID)) {
						L.log("WARNING: duplicate areaID " + spawnAreas[i].areaID + " in map " + this.name);
					}
				}
			}
		}
	}

	public final boolean isOutside(final Coord p) { return isOutside(p.x, p.y); }
	public final boolean isOutside(final int x, final int y) {
		if (x < 0) return true;
		if (y < 0) return true;
		if (x >= size.width) return true;
		if (y >= size.height) return true;
		return false;
	}
	public final boolean isOutside(final CoordRect area) {
		if (isOutside(area.topLeft)) return true;
		if (area.topLeft.x + area.size.width > size.width) return true;
		if (area.topLeft.y + area.size.height > size.height) return true;
		return false;
	}
	public boolean intersects(CoordRect area) {
		return new CoordRect(new Coord(0,0), size).intersects(area);
	}
	
	public MapObject findEventObject(MapObject.MapObjectType objectType, String name) {
		for (MapObject o : eventObjects) {
			if (o.type != objectType) continue;
			if (!name.equals(o.id)) continue;
			return o;
		}
		return null;
	}
	public List<MapObject> getActiveEventObjectsAt(final Coord p) {
		List<MapObject> result = null;
		for (MapObject o : eventObjects) {
			if (!o.isActive) continue;
			if (!o.position.contains(p)) continue;
			//if (!activeMapObjectGroups.contains(o.group)) continue;
			if (result == null) result = new ArrayList<MapObject>();
			result.add(o);
		}
		return result;
	}
	public boolean hasContainerAt(final Coord p) {
		for (MapObject o : eventObjects) {
			if (!o.isActive) continue;
			if (o.type != MapObject.MapObjectType.container) continue;
			if (!o.position.contains(p)) continue;
			return true;
		}
		return false;
	}
	public Monster getMonsterAt(final CoordRect p) {
		return getMonsterAt(p, null);
	}
	public Monster getMonsterAt(final CoordRect p, Monster exceptMe) {
		for (MonsterSpawnArea a : spawnAreas) {
			Monster m = a.getMonsterAt(p);
			if (m != null && (exceptMe == null || exceptMe != m)) return m;
		}
		return null;
	}
	public Monster getMonsterAt(final Coord p) { return getMonsterAt(p.x, p.y); }
	public Monster getMonsterAt(final int x, final int y) {
		for (MonsterSpawnArea a : spawnAreas) {
			Monster m = a.getMonsterAt(x, y);
			if (m != null) return m;
		}
		return null;
	}

	public Monster findSpawnedMonster(final String monsterTypeID) {
		for (MonsterSpawnArea a : spawnAreas) {
			Monster m = a.findSpawnedMonster(monsterTypeID);
			if (m != null) return m;
		}
		return null;
	}

	public Loot getBagAt(final Coord p) {
		for (Loot l : groundBags) {
			if (l.position.equals(p)) return l;
		}
		return null;
	}
	public Loot getBagOrCreateAt(final Coord position) {
		Loot b = getBagAt(position);
		if (b != null) return b;
		boolean isContainer = hasContainerAt(position);
		b = new Loot(!isContainer);
		b.position.set(position);
		if (isOutside(position)) {
			if (AndorsTrailApplication.DEVELOPMENT_VALIDATEDATA) {
				L.log("WARNING: trying to place bag outside map. Map is " + size.toString() + ", bag tried to place at " + position.toString());
			}
			return b;
		}
		groundBags.add(b);
		return b;
	}
	public void itemDropped(ItemType itemType, int quantity, Coord position) {
		Loot l = getBagOrCreateAt(position);
		l.items.addItem(itemType, quantity);
	}
	public void removeGroundLoot(Loot loot) {
		groundBags.remove(loot);
	}
	public void resetForNewGame() {
		for (MonsterSpawnArea a : spawnAreas) {
			a.resetForNewGame();
		}
		activeMapObjectGroups.clear();
		activeMapObjectGroups.addAll(initiallyActiveMapObjectGroups);
		activateMapObjects();
		resetTemporaryData();
		groundBags.clear();
		visited = false;
		currentColorFilter = initialColorFilter;
		lastSeenLayoutHash = "";
	}

	public boolean isRecentlyVisited() {
		if (lastVisitTime == VISIT_RESET) return false;
		return (System.currentTimeMillis() - lastVisitTime) < Constants.MAP_UNVISITED_RESPAWN_DURATION_MS;
	}
	public void updateLastVisitTime() {
		lastVisitTime = System.currentTimeMillis();
	}
	public void resetTemporaryData() {
		for(MonsterSpawnArea a : spawnAreas) {
			if (a.isUnique) a.resetShops();
			else a.removeAllMonsters();
		}
		splatters.clear();
		lastVisitTime = VISIT_RESET;
	}
	public boolean hasResetTemporaryData() {
		return lastVisitTime == VISIT_RESET;
	}

	public void createAllContainerLoot() {
		for (MapObject o : eventObjects) {
			if (!o.isActive) continue;
			if (o.type != MapObject.MapObjectType.container) continue;
			createContainerLoot(o);
		}
	}

	public void createContainerLoot(MapObject container) {
		Loot bag = getBagOrCreateAt(container.position.topLeft);
		container.dropList.createRandomLoot(bag, null);
	}


	private void activateMapObjects() {
		for (MapObject o : eventObjects) {
			o.isActive = activeMapObjectGroups.contains(o.group);
		}
	}
	
	public void activateMapObjectGroup(String group) {
		if (!activeMapObjectGroups.contains(group)) {
			activeMapObjectGroups.add(group);
			for (MapObject o : eventObjects) {
				if (o.group.equals(group)) {
					o.isActive = true;
					if (o.type == MapObjectType.container) createContainerLoot(o);
				}
			}
		}
	}
	
	public void deactivateMapObjectGroup(String group) {
		if (activeMapObjectGroups.contains(group)) {
			activeMapObjectGroups.remove(group);
			for (MapObject o : eventObjects) {
				if (o.group.equals(group)) {
					o.isActive = false;
				}
			}
		}
	}


	// ====== PARCELABLE ===================================================================

	public void readFromParcel(DataInputStream src, WorldContext world, ControllerContext controllers, int fileversion) throws IOException {
		boolean shouldLoadMapData = true;
		if (fileversion >= 37) shouldLoadMapData = src.readBoolean();

		int loadedSpawnAreas = 0;
		if (shouldLoadMapData) {
			loadedSpawnAreas = src.readInt();
			for(int i = 0; i < loadedSpawnAreas; ++i) {
				if (AndorsTrailApplication.DEVELOPMENT_VALIDATEDATA) {
					if (i >= this.spawnAreas.length) {
						L.log("WARNING: Trying to load monsters from savegame in map " + this.name + " for spawn #" + i + ". This will totally fail.");
					}
				}
				if(fileversion >= 43) {
					//Spawn areas now have unique IDs. Need to check as maps can change.
					String id = src.readUTF();
					int j = i;
					boolean found = false;
					do {
						if (this.spawnAreas[j].areaID.equals(id)) {
							this.spawnAreas[j].readFromParcel(src, world, fileversion);
							found = true;
							break;
						} 
						j = (j+1)%spawnAreas.length;
					} while (j != i);
					if (AndorsTrailApplication.DEVELOPMENT_VALIDATEDATA) {
						if (!found) {
							L.log("WARNING: Trying to load monsters from savegame in map " + this.name + " for spawn #" + id + " but this area cannot be found. This will totally fail.");
						}
					}
				} else {
					this.spawnAreas[i].readFromParcel(src, world, fileversion);
				}
			}
			
			activeMapObjectGroups.clear();
			if(fileversion >= 43) {
				int activeListLength = src.readInt();
				for (int i = 0; i < activeListLength; i++) {
					String activeGroupID = src.readUTF();
					activeMapObjectGroups.add(activeGroupID);
				}
			} else {
				activeMapObjectGroups.addAll(initiallyActiveMapObjectGroups);
			}
			activateMapObjects();

			groundBags.clear();
			if (fileversion <= 5) return;

			final int size2 = src.readInt();
			for(int i = 0; i < size2; ++i) {
				groundBags.add(new Loot(src, world, fileversion));
			}

			if (fileversion <= 11) return;

			if (fileversion < 37) visited = src.readBoolean();

			if (fileversion <= 15) {
				if (visited) {
					lastVisitTime = System.currentTimeMillis();
					createAllContainerLoot();
				}
				return;
			}
			
			if (fileversion >= 43) {
				if (src.readBoolean()) {
					currentColorFilter = src.readUTF();
				} else {
					currentColorFilter = null;
				}
			}
			
			lastVisitTime = src.readLong();

			if (visited) {
				if (fileversion > 30 && fileversion < 36) {
					/*int lastVisitVersion = */src.readInt();
				}
			}
		} else {
			activeMapObjectGroups.clear();
			activeMapObjectGroups.addAll(initiallyActiveMapObjectGroups);
			activateMapObjects();
		}
		if (fileversion >= 37) {
			if (fileversion < 41) visited = true;
			else visited = src.readBoolean();
		}

		if (fileversion < 36) lastSeenLayoutHash = "";
		else lastSeenLayoutHash = src.readUTF();

		for(int i = loadedSpawnAreas; i < spawnAreas.length; ++i) {
			MonsterSpawnArea area = this.spawnAreas[i];
			if (area.isUnique && visited) controllers.monsterSpawnController.spawnAllInArea(this, null, area, true);
			else area.resetForNewGame();
		}
	}

	public boolean shouldSaveMapData(WorldContext world) {
		if (!hasResetTemporaryData()) return true;
		if (this == world.model.currentMaps.map) return true;
		if (!groundBags.isEmpty()) return true;
		for (MonsterSpawnArea a : spawnAreas) {
			if (this.visited && a.isUnique) return true;
			if (a.isSpawning != a.isSpawningForNewGame) return true;
		}
		if (!activeMapObjectGroups.containsAll(initiallyActiveMapObjectGroups) 
				|| !initiallyActiveMapObjectGroups.containsAll(activeMapObjectGroups)) return true;
		if (currentColorFilter != null) return true;
		return false;
	}

	public void writeToParcel(DataOutputStream dest, WorldContext world) throws IOException {
		if (shouldSaveMapData(world)) {
			dest.writeBoolean(true);
			dest.writeInt(spawnAreas.length);
			for(MonsterSpawnArea a : spawnAreas) {
				dest.writeUTF(a.areaID);
				a.writeToParcel(dest);
			}
			dest.writeInt(activeMapObjectGroups.size());
			for(String s : activeMapObjectGroups) {
				dest.writeUTF(s);
			}
			dest.writeInt(groundBags.size());
			for(Loot l : groundBags) {
				l.writeToParcel(dest);
			}
			dest.writeBoolean(currentColorFilter != null);
			if (currentColorFilter != null) dest.writeUTF(currentColorFilter);
			dest.writeLong(lastVisitTime);
		} else {
			dest.writeBoolean(false);
		}
		dest.writeBoolean(visited);
		dest.writeUTF(lastSeenLayoutHash);
	}

	public void addToChecksum(ChecksumBuilder builder, WorldContext world) {
		if (shouldSaveMapData(world)) {
			builder.add(true);
			builder.add(spawnAreas.length);
			for(MonsterSpawnArea a : spawnAreas) {
				builder.add(a.areaID);
				a.addToChecksum(builder);
			}
			builder.add(activeMapObjectGroups.size());
			for(String s : activeMapObjectGroups) {
				builder.add(s);
			}
			builder.add(groundBags.size());
			for(Loot l : groundBags) {
				l.addToChecksum(builder);
			}
			builder.add(currentColorFilter != null);
			if (currentColorFilter != null) builder.add(currentColorFilter);
			builder.add(lastVisitTime);
		} else {
			builder.add(false);
		}
		builder.add(visited);
		builder.add(lastSeenLayoutHash);
	}
}
