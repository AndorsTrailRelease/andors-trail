package com.gpl.rpg.AndorsTrail.resource.parsers;

import android.util.Pair;

import com.gpl.rpg.AndorsTrail.model.item.ItemFilter;
import com.gpl.rpg.AndorsTrail.model.item.ItemType;
import com.gpl.rpg.AndorsTrail.model.item.ItemTypeCollection;
import com.gpl.rpg.AndorsTrail.resource.parsers.json.JsonCollectionParserFor;
import com.gpl.rpg.AndorsTrail.resource.parsers.json.JsonFieldNames;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ItemFilterParser extends JsonCollectionParserFor<ItemFilter> {

    private final ItemTypeCollection itemTypeCollection;

    public ItemFilterParser(final ItemTypeCollection itemTypeCollection) {
        this.itemTypeCollection = itemTypeCollection;
    }

    @Override
    public Pair<String, ItemFilter> parseObject(JSONObject o) throws JSONException {
        final String id = o.getString(JsonFieldNames.ItemFilter.itemFilterID);
        final JSONArray includeJson = o.optJSONArray(JsonFieldNames.ItemFilter.include);

        List<ItemType> include = new ArrayList<>();

        if (includeJson != null) {
            for (int i = 0; i < includeJson.length(); i++) {
                include.add(itemTypeCollection.getItemType(includeJson.getString(i)));
            }
        }

        final ItemFilter itemFilter = new ItemFilter(
                id
                , include
        );
        return new Pair<>(id, itemFilter);
    }
}