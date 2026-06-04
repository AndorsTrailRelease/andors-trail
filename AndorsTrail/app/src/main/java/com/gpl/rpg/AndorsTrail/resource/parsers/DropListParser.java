package com.gpl.rpg.AndorsTrail.resource.parsers;

import org.json.JSONException;
import org.json.JSONObject;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.model.item.DropList;
import com.gpl.rpg.AndorsTrail.model.item.DropList.DropItem;
import com.gpl.rpg.AndorsTrail.model.item.ItemFilterCollection;
import com.gpl.rpg.AndorsTrail.model.item.ItemTypeCollection;
import com.gpl.rpg.AndorsTrail.resource.parsers.json.JsonArrayParserFor;
import com.gpl.rpg.AndorsTrail.resource.parsers.json.JsonCollectionParserFor;
import com.gpl.rpg.AndorsTrail.resource.parsers.json.JsonFieldNames;
import com.gpl.rpg.AndorsTrail.util.L;
import android.util.Pair;

public final class DropListParser extends JsonCollectionParserFor<DropList> {

	private final JsonArrayParserFor<DropItem> dropItemParser;
	private final ItemTypeCollection itemTypeCollection;
	private final ItemFilterCollection itemFilterCollection;

	public DropListParser(final ItemTypeCollection itemTypeCollection, final ItemFilterCollection itemFilterCollection) {
		this.itemTypeCollection = itemTypeCollection;
		this.itemFilterCollection = itemFilterCollection;
		this.dropItemParser = new JsonArrayParserFor<DropItem>(DropItem.class) {
			@Override
			protected DropItem parseObject(JSONObject o) throws JSONException {
				return new DropItem(
						o.getString(JsonFieldNames.DropItem.itemID)
						,ResourceParserUtils.parseChance(o.getString(JsonFieldNames.DropItem.chance))
						,ResourceParserUtils.parseQuantity(o.getJSONObject(JsonFieldNames.DropItem.quantity))
				);
			}
		};
	}

	@Override
	protected Pair<String, DropList> parseObject(JSONObject o) throws JSONException {
		String droplistID = o.getString(JsonFieldNames.DropList.dropListID);
		DropItem[] items = dropItemParser.parseArray(o.getJSONArray(JsonFieldNames.DropList.items));

		if (AndorsTrailApplication.DEVELOPMENT_VALIDATEDATA) {
			if (items == null) {
				L.log("OPTIMIZE: Droplist \"" + droplistID + "\" has no dropped items.");
			}
			for (int i = 0; i < items.length; i++) {
				DropItem item = items[i];
				if (item.itemTypeID == null) {
					L.log("Item at index " + i + " in droplist " + droplistID + " was null");
				}
			}
		}

		return new Pair<>(droplistID, new DropList(
                items
                , itemTypeCollection
                , itemFilterCollection
        ));
	}
}
