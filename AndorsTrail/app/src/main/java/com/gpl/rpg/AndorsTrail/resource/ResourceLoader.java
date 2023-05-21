package com.gpl.rpg.AndorsTrail.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import android.content.res.Resources;
import android.content.res.TypedArray;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.model.conversation.ConversationCollection;
import com.gpl.rpg.AndorsTrail.model.map.TMXMapTranslator;
import com.gpl.rpg.AndorsTrail.resource.parsers.ActorConditionsTypeParser;
import com.gpl.rpg.AndorsTrail.resource.parsers.ConversationListParser;
import com.gpl.rpg.AndorsTrail.resource.parsers.DropListParser;
import com.gpl.rpg.AndorsTrail.resource.parsers.ItemCategoryParser;
import com.gpl.rpg.AndorsTrail.resource.parsers.ItemTypeParser;
import com.gpl.rpg.AndorsTrail.resource.parsers.MonsterTypeParser;
import com.gpl.rpg.AndorsTrail.resource.parsers.QuestParser;
import com.gpl.rpg.AndorsTrail.resource.parsers.WorldMapParser;
import com.gpl.rpg.AndorsTrail.util.L;
import com.gpl.rpg.AndorsTrail.util.Size;

public final class ResourceLoader {

	private static final int itemCategoriesResourceId = AndorsTrailApplication.DEVELOPMENT_DEBUGRESOURCES ? R.array.loadresource_itemcategories_debug : R.array.loadresource_itemcategories;
	private static final int actorConditionsResourceId = AndorsTrailApplication.DEVELOPMENT_DEBUGRESOURCES ? R.array.loadresource_actorconditions_debug : R.array.loadresource_actorconditions;
	private static final int itemsResourceId = AndorsTrailApplication.DEVELOPMENT_DEBUGRESOURCES ? R.array.loadresource_items_debug : R.array.loadresource_items;
	private static final int droplistsResourceId = AndorsTrailApplication.DEVELOPMENT_DEBUGRESOURCES ? R.array.loadresource_droplists_debug : R.array.loadresource_droplists;
	private static final int questsResourceId = AndorsTrailApplication.DEVELOPMENT_DEBUGRESOURCES ? R.array.loadresource_quests_debug : R.array.loadresource_quests;
	private static final int conversationsListsResourceId = AndorsTrailApplication.DEVELOPMENT_DEBUGRESOURCES ? R.array.loadresource_conversationlists_debug : R.array.loadresource_conversationlists;
	private static final int monstersResourceId = AndorsTrailApplication.DEVELOPMENT_DEBUGRESOURCES ? R.array.loadresource_monsters_debug : R.array.loadresource_monsters;
	private static final int mapsResourceId = AndorsTrailApplication.DEVELOPMENT_DEBUGRESOURCES ? R.array.loadresource_maps_debug : R.array.loadresource_maps;

	private static DynamicTileLoader loader;
	private static TranslationLoader translationLoader; 
	private static long taskStart;
	private static void timingCheckpoint(String loaderName) {
		long now = System.currentTimeMillis();
		long duration = now - taskStart;
		L.log(loaderName + " ran for " + duration + " ms.");
		taskStart = now;
	}
	
	public static void loadResourcesSync(WorldContext world, Resources r) {
		long start = System.currentTimeMillis();
		taskStart = start;

		final int mTileSize = world.tileManager.tileSize;


		loader = new DynamicTileLoader(world.tileManager.tileCache);
		prepareTilesets(loader, mTileSize);
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) timingCheckpoint("prepareTilesets");

		// ========================================================================
		// Load various ui icons
//		HeroCollection.prepareHeroesTileId(loader);
		/*TileManager.iconID_CHAR_HERO_0 = */loader.prepareTileID(R.drawable.char_hero, 0);
		/*TileManager.iconID_CHAR_HERO_1 = */loader.prepareTileID(R.drawable.char_hero_maksiu_girl_01, 0);
		/*TileManager.iconID_CHAR_HERO_2 = */loader.prepareTileID(R.drawable.char_hero_maksiu_boy_01, 0);
		/*TileManager.iconID_selection_red = */loader.prepareTileID(R.drawable.ui_selections, 0);
		/*TileManager.iconID_selection_yellow = */loader.prepareTileID(R.drawable.ui_selections, 1);
		/*TileManager.iconID_groundbag = */loader.prepareTileID(R.drawable.ui_icon_equipment, 0);
		/*TileManager.iconID_boxopened = */loader.prepareTileID(R.drawable.ui_quickslots, 1);
		/*TileManager.iconID_boxclosed = */loader.prepareTileID(R.drawable.ui_quickslots, 0);
		/*TileManager.iconID_selection_blue = */loader.prepareTileID(R.drawable.ui_selections, 2);
		/*TileManager.iconID_selection_purple = */loader.prepareTileID(R.drawable.ui_selections, 3);
		/*TileManager.iconID_selection_green = */loader.prepareTileID(R.drawable.ui_selections, 4);
		for(int i = 0; i < 5; ++i) {
			loader.prepareTileID(R.drawable.ui_splatters1, i);
			loader.prepareTileID(R.drawable.ui_splatters1, i+8);
		}
		loader.prepareTileID(R.drawable.ui_icon_immunity, 0);
		
		//Placeholders for dynamic map tiles
		loader.prepareTileID(R.drawable.map_dynamic_placeholders, 0);
		loader.prepareTileID(R.drawable.map_dynamic_placeholders, 1);
		loader.prepareTileID(R.drawable.map_dynamic_placeholders, 2);
		loader.prepareTileID(R.drawable.map_dynamic_placeholders, 3);
		loader.prepareTileID(R.drawable.map_dynamic_placeholders, 4);
		loader.prepareTileID(R.drawable.map_dynamic_placeholders, 5);
		loader.prepareTileID(R.drawable.map_dynamic_placeholders, 6);
		loader.prepareTileID(R.drawable.map_dynamic_placeholders, 7);
		loader.prepareTileID(R.drawable.map_dynamic_placeholders, 8);
		loader.prepareTileID(R.drawable.map_dynamic_placeholders, 9);

		// Load effects
		world.visualEffectTypes.initialize(loader);
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) timingCheckpoint("VisualEffectLoader");
		
		translationLoader = new TranslationLoader(r.getAssets(), r);
		
		
		// ========================================================================
		// Load skills
		world.skills.initialize();
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) timingCheckpoint("SkillLoader");

		// ========================================================================
		// Load item categories
		final ItemCategoryParser itemCategoryParser = new ItemCategoryParser(translationLoader);
		final TypedArray categoriesToLoad = r.obtainTypedArray(itemCategoriesResourceId);
		for (int i = 0; i < categoriesToLoad.length(); ++i) {
			world.itemCategories.initialize(itemCategoryParser, readStringFromRaw(r, categoriesToLoad, i));
		}
		categoriesToLoad.recycle();
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) timingCheckpoint("ItemCategoryParser");

		// ========================================================================
		// Load condition types
		final ActorConditionsTypeParser actorConditionsTypeParser = new ActorConditionsTypeParser(loader, translationLoader);
		final TypedArray conditionsToLoad = r.obtainTypedArray(actorConditionsResourceId);
		for (int i = 0; i < conditionsToLoad.length(); ++i) {
			world.actorConditionsTypes.initialize(actorConditionsTypeParser, readStringFromRaw(r, conditionsToLoad, i));
		}
		conditionsToLoad.recycle();
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) timingCheckpoint("ActorConditionsTypeParser");

		
		// ========================================================================
		// Load preloaded tiles
		loader.flush();
		world.tileManager.loadPreloadedTiles(r);
	}

	public static void loadResourcesAsync(WorldContext world, Resources r) {
		long start = System.currentTimeMillis();
		taskStart = start;

		// ========================================================================
		// Load items
		final ItemTypeParser itemTypeParser = new ItemTypeParser(loader, world.actorConditionsTypes, world.itemCategories, translationLoader);
		final TypedArray itemsToLoad = r.obtainTypedArray(itemsResourceId);
		for (int i = 0; i < itemsToLoad.length(); ++i) {
			world.itemTypes.initialize(itemTypeParser, readStringFromRaw(r, itemsToLoad, i));
		}
		itemsToLoad.recycle();
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) timingCheckpoint("ItemTypeParser");


		// ========================================================================
		// Load droplists
		final DropListParser dropListParser = new DropListParser(world.itemTypes);
		final TypedArray droplistsToLoad = r.obtainTypedArray(droplistsResourceId);
		for (int i = 0; i < droplistsToLoad.length(); ++i) {
			world.dropLists.initialize(dropListParser, readStringFromRaw(r, droplistsToLoad, i));
		}
		droplistsToLoad.recycle();
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) timingCheckpoint("DropListParser");


		// ========================================================================
		// Load quests
		final QuestParser questParser = new QuestParser(translationLoader);
		final TypedArray questsToLoad = r.obtainTypedArray(questsResourceId);
		for (int i = 0; i < questsToLoad.length(); ++i) {
			world.quests.initialize(questParser, readStringFromRaw(r, questsToLoad, i));
		}
		questsToLoad.recycle();
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) timingCheckpoint("QuestParser");


		// ========================================================================
		// Load conversations
		final ConversationListParser conversationListParser = new ConversationListParser(translationLoader);
		final TypedArray conversationsListsToLoad = r.obtainTypedArray(conversationsListsResourceId);
		for (int i = 0; i < conversationsListsToLoad.length(); ++i) {
			ConversationCollection conversations = new ConversationCollection();
			Collection<String> ids = conversations.initialize(conversationListParser, readStringFromRaw(r, conversationsListsToLoad, i));
			world.conversationLoader.addIDs(conversationsListsToLoad.getResourceId(i, -1), ids);
		}
		conversationsListsToLoad.recycle();
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) timingCheckpoint("ConversationListParser");


		// ========================================================================
		// Load monsters
		final MonsterTypeParser monsterTypeParser = new MonsterTypeParser(world.dropLists, world.actorConditionsTypes, loader, translationLoader);
		final TypedArray monstersToLoad = r.obtainTypedArray(monstersResourceId);
		for (int i = 0; i < monstersToLoad.length(); ++i) {
			world.monsterTypes.initialize(monsterTypeParser, readStringFromRaw(r, monstersToLoad, i));
		}
		monstersToLoad.recycle();
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) timingCheckpoint("MonsterTypeParser");


		// ========================================================================
		// Load maps
		TMXMapTranslator mapReader = new TMXMapTranslator();
		final TypedArray mapsToLoad = r.obtainTypedArray(mapsResourceId);
		for (int i = 0; i < mapsToLoad.length(); ++i) {
			final int mapResourceId = mapsToLoad.getResourceId(i, -1);
			final String mapName = r.getResourceEntryName(mapResourceId);
			mapReader.read(r, mapResourceId, mapName);
		}
		mapsToLoad.recycle();
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) timingCheckpoint("TMXMapReader");
		world.maps.addAll(mapReader.transformMaps(world.monsterTypes, world.dropLists));
		loader.prepareAllMapTiles();
		mapReader = null;
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) timingCheckpoint("mapReader.transformMaps");


		// ========================================================================
		// Load graphics resources (icons and tiles)
		loader.flush();
		loader = null;
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) timingCheckpoint("DynamicTileLoader");
		// ========================================================================


		// ========================================================================
		// Load worldmap coordinates
		WorldMapParser.read(r, R.xml.worldmap, world.maps, translationLoader);
		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) timingCheckpoint("WorldMapParser");
		// ========================================================================

		translationLoader.close();


		if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) {
			long duration = System.currentTimeMillis() - start;
			L.log("ResourceLoader ran for " + duration + " ms.");
		}
	}

	public static String readStringFromRaw(final Resources r, final TypedArray array, final int index) {
		return readStringFromRaw(r, array.getResourceId(index, -1));
	}
	public static String readStringFromRaw(final Resources r, final int resourceID) {
		InputStream is = r.openRawResource(resourceID);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder(1000);
		String line;
		try {
			while((line = br.readLine()) != null) sb.append(line);
			br.close();
			is.close();
			return sb.toString();
		} catch (IOException e) {
			L.log("ERROR: Reading from resource " + resourceID + " failed. " + e.toString());
			return "";
		}
	}

	private static void prepareTilesets(DynamicTileLoader loader, int mTileSize) {
		final Size sz1x1 = new Size(1, 1);
		final Size sz2x1 = new Size(2, 1);
		final Size sz2x2 = new Size(2, 2);
		final Size sz2x3 = new Size(2, 3);
		final Size sz3x1 = new Size(3, 1);
		final Size sz5x1 = new Size(5, 1);
		final Size sz6x1 = new Size(6, 1);
		final Size sz7x1 = new Size(7, 1);
		final Size sz8x3 = new Size(8, 3);
		final Size sz20x12 = new Size(20, 12);
		final Size mapTileSize = new Size(16, 8);
		final Size sz8x8 = new Size(8, 8);


		loader.prepareTileset(R.drawable.char_hero, "char_hero", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.char_hero_maksiu_girl_01, "char_hero_maksiu_girl_01", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.char_hero_maksiu_boy_01, "char_hero_maksiu_boy_01", sz1x1, sz1x1, mTileSize);

		loader.prepareTileset(R.drawable.ui_selections, "ui_selections", new Size(5, 1), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.ui_quickslots, "ui_quickslots", sz2x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.ui_icon_equipment, "ui_icon_equipment", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.ui_splatters1, "ui_splatters1", new Size(8, 2), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.ui_icon_immunity, "ui_icon_immunity", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_dynamic_placeholders, "map_dynamic_placeholders", new Size(10, 2), sz1x1, mTileSize);

		loader.prepareTileset(R.drawable.monsters_cyclops, "monsters_cyclops", sz1x1, sz2x3, mTileSize);
		loader.prepareTileset(R.drawable.monsters_demon1, "monsters_demon1", sz1x1, sz2x2, mTileSize);
		loader.prepareTileset(R.drawable.monsters_demon2, "monsters_demon2", sz1x1, sz2x2, mTileSize);
		loader.prepareTileset(R.drawable.monsters_eye4, "monsters_eye4", sz1x1, sz1x1, mTileSize);
        loader.prepareTileset(R.drawable.monsters_giantbasilisk, "monsters_giantbasilisk", sz1x1, sz2x2, mTileSize);
		loader.prepareTileset(R.drawable.monsters_bosses_2x2, "monsters_bosses_2x2", sz1x1, sz2x2, mTileSize);

		loader.prepareTileset(R.drawable.map_0, "map_0", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_1, "map_1", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_10, "map_10", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_11, "map_11", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_12, "map_12", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_13, "map_13", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_14, "map_14", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_15, "map_15", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_16, "map_16", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_17, "map_17", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_18, "map_18", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_19, "map_19", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_2, "map_2", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_20, "map_20", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_21, "map_21", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_22, "map_22", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_23, "map_23", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_24, "map_24", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_25, "map_25", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_26, "map_26", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_27, "map_27", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_28, "map_28", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_29, "map_29", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_3, "map_3", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_30, "map_30", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_31, "map_31", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_32, "map_32", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_33, "map_33", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_34, "map_34", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_35, "map_35", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_36, "map_36", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_37, "map_37", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_38, "map_38", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_39, "map_39", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_4, "map_4", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_40, "map_40", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_41, "map_41", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_42, "map_42", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_43, "map_43", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_44, "map_44", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_45, "map_45", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_46, "map_46", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_47, "map_47", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_48, "map_48", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_49, "map_49", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_5, "map_5", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_50, "map_50", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_51, "map_51", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_52, "map_52", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_53, "map_53", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_54, "map_54", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_55, "map_55", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_56, "map_56", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_57, "map_57", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_6, "map_6", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_7, "map_7", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_8, "map_8", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_9, "map_9", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.obj_0, "obj_0", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.obj_1, "obj_1", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.obj_10, "obj_10", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.obj_11, "obj_11", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.obj_12, "obj_12", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.obj_13, "obj_13", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.obj_14, "obj_14", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.obj_15, "obj_15", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.obj_16, "obj_16", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.obj_2, "obj_2", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.obj_3, "obj_3", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.obj_4, "obj_4", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.obj_5, "obj_5", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.obj_6, "obj_6", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.obj_7, "obj_7", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.obj_8, "obj_8", sz8x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.obj_9, "obj_9", sz8x8, sz1x1, mTileSize);

		loader.prepareTileset(R.drawable.effect_blood4, "effect_blood4", new Size(7, 2), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.effect_heal2, "effect_heal2", new Size(8, 2), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.effect_poison1, "effect_poison1", new Size(8, 2), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.effect_miss1, "effect_miss1", new Size(8, 2), sz1x1, mTileSize);
	}
}
