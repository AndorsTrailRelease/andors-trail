package com.gpl.rpg.AndorsTrail.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.model.map.PredefinedMap;

public final class ModelContainer {

	public final Player player;
	public final InterfaceData uiSelections;
	public final CombatLog combatLog = new CombatLog();
	public final GameStatistics statistics;
	public final WorldData worldData;
	public MapBundle currentMaps = new MapBundle();;

	public ModelContainer(int startLives, boolean unlimitedSaves) {
		player = new Player();
		uiSelections = new InterfaceData();
		statistics = new GameStatistics(unlimitedSaves, startLives);
		worldData = new WorldData();
	}

	// ====== PARCELABLE ===================================================================

	public ModelContainer(DataInputStream src, WorldContext world, ControllerContext controllers, int fileversion) throws IOException {
		this.player = Player.newFromParcel(src, world, controllers, fileversion);
		this.currentMaps.map = world.maps.findPredefinedMap(src.readUTF());
		this.uiSelections = new InterfaceData(src, fileversion);
		if (uiSelections.selectedPosition != null) {
			this.uiSelections.selectedMonster = currentMaps.map.getMonsterAt(uiSelections.selectedPosition);
		}
		this.statistics = new GameStatistics(src, world, fileversion);
		this.currentMaps.tileMap = null;
		if (fileversion >= 40) {
			this.worldData = new WorldData(src, fileversion);
		} else {
			this.worldData = new WorldData();
		}
	}

	public void writeToParcel(DataOutputStream dest) throws IOException {
		player.writeToParcel(dest);
		dest.writeUTF(currentMaps.map.name);
		uiSelections.writeToParcel(dest);
		statistics.writeToParcel(dest);
		worldData.writeToParcel(dest);
	}
}
