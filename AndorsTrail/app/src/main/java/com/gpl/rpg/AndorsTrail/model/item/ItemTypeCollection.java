package com.gpl.rpg.AndorsTrail.model.item;

import java.util.HashMap;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.controller.Constants;
import com.gpl.rpg.AndorsTrail.resource.parsers.ItemTypeParser;
import com.gpl.rpg.AndorsTrail.util.L;

public final class ItemTypeCollection {
	private static final String ITEMTYPE_GOLD = "gold";

	private final HashMap<String, ItemType> itemTypes = new HashMap<String, ItemType>();

	public ItemType getItemType(String id) {
		if (AndorsTrailApplication.DEVELOPMENT_VALIDATEDATA) {
			if (!itemTypes.containsKey(id)) {
				L.log("WARNING: Cannot find ItemType for id \"" + id + "\".");
				return null;
			}
		}
		return itemTypes.get(id);
	}

	public static boolean isGoldItemType(String itemTypeID) {
		if (itemTypeID == null) return false;
		return itemTypeID.equals(ITEMTYPE_GOLD);
	}

	public static boolean isItemFilter(String itemTypeID) {
		if (itemTypeID == null) return false;
		return itemTypeID.toLowerCase().startsWith(Constants.ITEMFILTER_ID_PREFIX);
	}
	public static String getItemFilterID(String itemTypeID) {
		if (isItemFilter(itemTypeID))
			return itemTypeID.substring(Constants.ITEMFILTER_ID_PREFIX.length());
		else return itemTypeID;
	}

	public void initialize(final ItemTypeParser parser, String input) {
		parser.parseRows(input, itemTypes);
	}

	// Unit test method. Not part of the game logic.
	public HashMap<String, ItemType> UNITTEST_getAllItemTypes() {
		return itemTypes;
	}
}
