package com.gpl.rpg.AndorsTrail.model.map;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.model.ChecksumBuilder;
import com.gpl.rpg.AndorsTrail.savegames.LegacySavegameFormatReaderForMap;
import com.gpl.rpg.AndorsTrail.util.L;
import com.gpl.rpg.AndorsTrail.util.Size;

public final class MapCollection {
	private final HashMap<String, PredefinedMap> predefinedMaps = new HashMap<String, PredefinedMap>();
	public final HashMap<String, WorldMapSegment> worldMapSegments = new HashMap<String, WorldMapSegment>();
	public boolean worldMapRequiresUpdate = true;

	public MapCollection() {}

	public void addAll(ArrayList<PredefinedMap> mapsToAdd) {
		for (PredefinedMap map : mapsToAdd) {
			predefinedMaps.put(map.name, map);
		}
	}

	public Collection<PredefinedMap> getAllMaps() {
		return predefinedMaps.values();
	}

	public PredefinedMap findPredefinedMap(String name) {
		if (AndorsTrailApplication.DEVELOPMENT_VALIDATEDATA) {
			if (!predefinedMaps.containsKey(name)) {
				L.log("WARNING: Cannot find PredefinedMap for name \"" + name + "\".");
			}
		}
		return predefinedMaps.get(name);
	}

	public void resetForNewGame() {
		for (PredefinedMap m : getAllMaps()) {
			m.resetForNewGame();
		}
		worldMapRequiresUpdate = true;
	}

	public String getWorldMapSegmentNameForMap(String mapName) {
		for (WorldMapSegment segment : worldMapSegments.values()) {
			if (segment.containsMap(mapName)) return segment.name;
		}
		return null;
	}


	// ====== PARCELABLE ===================================================================

	public void readFromParcel(DataInputStream src, WorldContext world, ControllerContext controllers, int fileversion) throws IOException {
		int size;
		if (fileversion == 5) size = 11;
		else size = src.readInt();
		for(int i = 0; i < size; ++i) {
			String name;
			if (fileversion >= 35) {
				L.debug("MapCollection.readFromParcel: reading map name for entry " + i + " of " + size + ".");
				name = src.readUTF();
			} else {
				name = LegacySavegameFormatReaderForMap.getMapnameFromIndex(i);
			}
			L.debug("MapCollection.readFromParcel: loading map \"" + name + "\" (" + (i + 1) + "/" + size + ").");
			PredefinedMap map = predefinedMaps.get(name);
			if (map == null) {
				// Map not found.  In production mode only, read and discard the missing map.  This should never happen,
				// because these content bugs should be caught before it goes to prod, but just in case we want
				// to make sure that the user can still use their savefile if it happens anyway.
				if (AndorsTrailApplication.DEVELOPMENT_VALIDATEDATA) {
					throw new IOException("Savegame contains unknown map \"" + name + "\".");
				} else {
					PredefinedMap placeholder = new PredefinedMap(-1, name, new Size(1, 1), new MapObject[0], new MonsterSpawnArea[0], Collections.emptyList(), false, null);
					placeholder.readFromParcel(src, world, controllers, fileversion);
				}
			} else {
				map.readFromParcel(src, world, controllers, fileversion);
				L.debug("MapCollection.readFromParcel: loaded map \"" + name + "\" (" + (i + 1) + "/" + size + ").");
				if (i >= 40) {
					if (fileversion < 15) map.visited = false;
				}
			}
		}
	}

	public static boolean shouldSaveMap(WorldContext world, PredefinedMap map) {
		if (map.visited) return true;
        return map.shouldSaveMapData(world);
    }

	public void writeToParcel(DataOutputStream dest, WorldContext world) throws IOException {
		List<PredefinedMap> mapsToExport = new ArrayList<PredefinedMap>();
		for(PredefinedMap map : getAllMaps()) {
			if (shouldSaveMap(world, map)) mapsToExport.add(map);
		}
		dest.writeInt(mapsToExport.size());
		for(PredefinedMap map : mapsToExport) {
			dest.writeUTF(map.name);
			map.writeToParcel(dest, world);
		}
	}

	public void addToChecksum(ChecksumBuilder checksumBuilder, WorldContext world) {
		List<PredefinedMap> mapsToExport = new ArrayList<PredefinedMap>();
		for(PredefinedMap map : getAllMaps()) {
			if (shouldSaveMap(world, map)) mapsToExport.add(map);
		}
		checksumBuilder.add(mapsToExport.size());
		for(PredefinedMap map : mapsToExport) {
			checksumBuilder.add(map.name);
			map.addToChecksum(checksumBuilder, world);
		}
	}
}
