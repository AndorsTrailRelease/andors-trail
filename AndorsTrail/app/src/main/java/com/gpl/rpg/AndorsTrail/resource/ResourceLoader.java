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
			String s = readStringFromRaw(r, itemsToLoad, i);
			world.itemTypes.initialize(itemTypeParser, s);
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
		final Size sz1x2 = new Size(1, 2);
		final Size sz2x1 = new Size(2, 1);
		final Size sz2x2 = new Size(2, 2);
		final Size sz2x3 = new Size(2, 3);
		final Size sz3x1 = new Size(3, 1);
		final Size sz4x1 = new Size(4, 1);
		final Size sz5x1 = new Size(5, 1);
		final Size sz6x1 = new Size(6, 1);
		final Size sz7x1 = new Size(7, 1);
		final Size sz7x4 = new Size(7, 4);
		final Size sz8x3 = new Size(8, 3);
		final Size sz8x4 = new Size(8, 4);
		final Size sz10x2 = new Size(10, 2);
		final Size sz16x8 = new Size(16, 8);
		final Size sz16x10 = new Size(16, 10);
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

		loader.prepareTileset(R.drawable.actorconditions_1, "actorconditions_1", new Size(14, 8), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.actorconditions_2, "actorconditions_2", sz3x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.actorconditions_japozero, "actorconditions_japozero", new Size(16, 4), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.actorconditions_newb, "actorconditions_newb", new Size(20, 1), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.actorconditions_omi1, "actorconditions_omi1", sz2x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.actorconditions_omi2, "actorconditions_omi2", sz5x1, sz1x1, mTileSize);
		/*INSERT_ACTORCONDITIONS_TILESETS_HERE*/

		loader.prepareTileset(R.drawable.items_armours, "items_armours", new Size(14, 3), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_weapons, "items_weapons", new Size(14, 6), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_jewelry, "items_jewelry", new Size(14, 1), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_consumables, "items_consumables", new Size(14, 5), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_books, "items_books", new Size(11, 1), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_misc, "items_misc", new Size(14, 4), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_misc_2, "items_misc_2", sz20x12, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_misc_3, "items_misc_3", sz20x12, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_misc_4, "items_misc_4", new Size(20, 4), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_misc_5, "items_misc_5", new Size(9, 5), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_misc_6, "items_misc_6", new Size(9, 4), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_reterski_1, "items_reterski_1", new Size(3, 10), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_tometik1, "items_tometik1", new Size(6, 10), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_tometik2, "items_tometik2", new Size(10, 10), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_tometik3, "items_tometik3", new Size(8, 6), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_necklaces_1, "items_necklaces_1", new Size(10, 3), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_weapons_2, "items_weapons_2", new Size(7, 1), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_weapons_3, "items_weapons_3", new Size(13, 5), sz1x1, mTileSize);

		loader.prepareTileset(R.drawable.items_armours_2, "items_armours_2", sz7x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_armours_3, "items_armours_3", new Size(10, 4), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_consumables_omi1, "items_consumables_omi1", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_feygard1, "items_feygard1", new Size(6, 2), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_g03_package_omi1, "items_g03_package_omi1", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_japozero, "items_japozero", new Size(16, 37), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_newb, "items_newb", new Size(32, 27), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_omi2, "items_omi2", new Size(6, 5), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_omgeeky, "items_omgeeky", new Size(10, 3), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_phoenix01, "items_phoenix01", sz16x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_rijackson_1, "items_rijackson_1", new Size(5, 4), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.items_rings_1, "items_rings_1", new Size(10, 3), sz1x1, mTileSize);
		/*INSERT_ITEMS_TILESETS_HERE*/
		
		loader.prepareTileset(R.drawable.monsters_armor1, "monsters_armor1", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_cyclops, "monsters_cyclops", sz1x1, sz2x3, mTileSize);
		loader.prepareTileset(R.drawable.monsters_demon1, "monsters_demon1", sz1x1, sz2x2, mTileSize);
		loader.prepareTileset(R.drawable.monsters_demon2, "monsters_demon2", sz1x1, sz2x2, mTileSize);
		loader.prepareTileset(R.drawable.monsters_dogs, "monsters_dogs", sz7x4, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_eye1, "monsters_eye1", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_eye2, "monsters_eye2", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_eye3, "monsters_eye3", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_eye4, "monsters_eye4", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_ghost1, "monsters_ghost1", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_hydra1, "monsters_hydra1", sz1x1, sz2x2, mTileSize);
		loader.prepareTileset(R.drawable.monsters_insects, "monsters_insects", sz16x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_johny, "monsters_johny", sz20x12, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_karvis1, "monsters_karvis1", sz2x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_karvis2, "monsters_karvis2", new Size(9, 1), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_ld1, "monsters_ld1", sz20x12, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_ld2, "monsters_ld2", sz20x12, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_liches, "monsters_liches", new Size(4, 1), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_mage, "monsters_mage", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_mage2, "monsters_mage2", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_man1, "monsters_man1", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_men, "monsters_men", new Size(9, 1), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_men2, "monsters_men2", new Size(10, 1), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_misc, "monsters_misc", new Size(13, 1), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_newb_1, "monsters_newb_1", new Size(40, 34), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_newb_2, "monsters_newb_2", new Size(8, 2), sz1x2, mTileSize);
		loader.prepareTileset(R.drawable.monsters_newb_3, "monsters_newb_3", new Size(10, 10), sz2x2, mTileSize);
		loader.prepareTileset(R.drawable.monsters_newb_4, "monsters_newb_4", new Size(4, 1), sz2x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_rats, "monsters_rats", new Size(5, 1), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_redshrike1, "monsters_redshrike1", sz7x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_rltiles1, "monsters_rltiles1", new Size(20, 8), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_rltiles2, "monsters_rltiles2", new Size(20, 9), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_rltiles3, "monsters_rltiles3", new Size(10, 3), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_rltiles4, "monsters_rltiles4", new Size(12, 4), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_rogue1, "monsters_rogue1", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_skeleton1, "monsters_skeleton1", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_skeleton2, "monsters_skeleton2", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_snakes, "monsters_snakes", sz6x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_tometik1, "monsters_tometik1", new Size(10, 9), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_tometik2, "monsters_tometik2", new Size(8, 10), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_tometik3, "monsters_tometik3", new Size(6, 13), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_tometik4, "monsters_tometik4", new Size(6, 13), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_tometik5, "monsters_tometik5", new Size(6, 16), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_tometik6, "monsters_tometik6", new Size(7, 6), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_tometik7, "monsters_tometik7", new Size(8, 11), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_tometik8, "monsters_tometik8", new Size(7, 9), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_tometik9, "monsters_tometik9", new Size(8, 8), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_tometik10, "monsters_tometik10", new Size(6, 13), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_wraiths, "monsters_wraiths", sz3x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_zombie1, "monsters_zombie1", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_zombie2, "monsters_zombie2", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_guynmart, "monsters_guynmart", new Size(8, 2), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_maksiu1, "monsters_maksiu1", new Size(4, 4), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_omi1, "monsters_omi1", sz2x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_omi1_b, "monsters_omi1_b", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_unknown, "monsters_unknown", sz1x1, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_arulirs, "monsters_arulirs", new Size(8, 2), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_fatboy73, "monsters_fatboy73", sz20x12, sz1x1, mTileSize);
        loader.prepareTileset(R.drawable.monsters_giantbasilisk, "monsters_giantbasilisk", sz1x1, sz2x2, mTileSize);
        loader.prepareTileset(R.drawable.monsters_gisons, "monsters_gisons", new Size(8, 2), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_bosses_2x2, "monsters_bosses_2x2", sz8x4, sz2x2, mTileSize);
		loader.prepareTileset(R.drawable.monsters_omi2, "monsters_omi2", sz8x3, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_phoenix01, "monsters_phoenix01", sz16x8, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_cats, "monsters_cats", new Size(10, 2), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.monsters_1x2, "monsters_1x2", new Size(10, 5), sz1x2, mTileSize);
		loader.prepareTileset(R.drawable.monsters_antison, "monsters_antison", sz10x2, sz1x1, mTileSize);
		/*INSERT_NPCS_TILESETS_HERE*/
		
		loader.prepareTileset(R.drawable.map_bed_1, "map_bed_1", sz16x10, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_boats_1, "map_boats_1", new Size(16, 9), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_border_1, "map_border_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_bridge_1, "map_bridge_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_bridge_2, "map_bridge_2", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_brightport, "map_brightport", new Size(16, 7), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_broken_1, "map_broken_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_cavewall_1, "map_cavewall_1", new Size(18, 6), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_cavewall_2, "map_cavewall_2", new Size(18, 6), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_cavewall_3, "map_cavewall_3", new Size(18, 6), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_cavewall_4, "map_cavewall_4", new Size(18, 6), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_cavewall_5, "map_cavewall_5", new Size(6, 6), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_cavewall_6, "map_cavewall_6", new Size(18, 6), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_chair_table_1, "map_chair_table_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_chair_table_2, "map_chair_table_2", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_crate_1, "map_crate_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_cupboard_1, "map_cupboard_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_curtain_1, "map_curtain_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_entrance_1, "map_entrance_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_entrance_2, "map_entrance_2", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_entrance_3, "map_entrance_3", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_fence_1, "map_fence_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_fence_2, "map_fence_2", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_fence_3, "map_fence_3", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_fence_4, "map_fence_4", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_ground_1, "map_ground_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_ground_2, "map_ground_2", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_ground_3, "map_ground_3", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_ground_4, "map_ground_4", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_ground_5, "map_ground_5", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_ground_6, "map_ground_6", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_ground_7, "map_ground_7", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_ground_8, "map_ground_8", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_ground_9, "map_ground_9", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_ground_10, "map_ground_10", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_guynmart, "map_guynmart", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_house_1, "map_house_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_house_2, "map_house_2", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_indoor_1, "map_indoor_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_indoor_2, "map_indoor_2", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_items, "map_items", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_kitchen_1, "map_kitchen_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_outdoor_1, "map_outdoor_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_outdoor_2, "map_outdoor_2", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_pillar_1, "map_pillar_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_pillar_2, "map_pillar_2", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_pillar_3, "map_pillar_3", new Size(16, 10), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_plant_1, "map_plant_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_plant_2, "map_plant_2", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_plant_3, "map_plant_3", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_plateau, "map_plateau", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_plateau_2, "map_plateau_2", new Size(8, 8), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_ratdom, "map_ratdom", new Size(18, 6), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_rock_1, "map_rock_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_rock_2, "map_rock_2", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_rock_3, "map_rock_3", new Size(6, 5), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_roof_1, "map_roof_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_roof_2, "map_roof_2", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_roof_3, "map_roof_3", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_roof_4, "map_roof_4", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_roof_5, "map_roof_5", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_shop_1, "map_shop_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_sign_ladder_1, "map_sign_ladder_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_sign_ladder_omi2, "map_sign_ladder_omi2", new Size(8, 4), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_table_1, "map_table_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_table_2, "map_table_2", new Size(14, 8), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_table_3, "map_table_3", new Size(12, 10), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_temple_1, "map_temple_1", new Size(23, 16), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_trail_1, "map_trail_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_transition_1, "map_transition_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_transition_2, "map_transition_2", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_transition_3, "map_transition_3", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_transition_4, "map_transition_4", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_transition_5, "map_transition_5", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_transition_6, "map_transition_6", new Size(18, 8), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_tree_1, "map_tree_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_tree_2, "map_tree_2", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_tree_3, "map_tree_3", new Size(32, 17), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_tree_4, "map_tree_4", new Size(16, 8), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_wall_1, "map_wall_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_wall_2, "map_wall_2", new Size(15, 8), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_wall_3, "map_wall_3", new Size(15, 8), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_wall_4, "map_wall_4", new Size(15, 8), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_window_1, "map_window_1", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_window_2, "map_window_2", mapTileSize, sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.map_items_japozero, "map_items_japozero", new Size(16, 37), sz1x1, mTileSize);
		/*INSERT_MAP_TILESETS_HERE*/

		loader.prepareTileset(R.drawable.effect_blood4, "effect_blood4", new Size(7, 2), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.effect_heal2, "effect_heal2", new Size(8, 2), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.effect_poison1, "effect_poison1", new Size(8, 2), sz1x1, mTileSize);
		loader.prepareTileset(R.drawable.effect_miss1, "effect_miss1", new Size(8, 2), sz1x1, mTileSize);
	}
}
