package com.gpl.rpg.AndorsTrail.model.item;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.resource.parsers.ItemFilterParser;
import com.gpl.rpg.AndorsTrail.util.L;

import java.util.HashMap;

public class ItemFilterCollection {
    private final HashMap<String, ItemFilter> itemFilters = new HashMap<String, ItemFilter>();


    public ItemFilter getItemFilter(String id) {
        id = ItemTypeCollection.getItemFilterID(id);

        if (AndorsTrailApplication.DEVELOPMENT_VALIDATEDATA) {
            if (!itemFilters.containsKey(id)) {
                L.log("WARNING: Cannot find ItemFilter for id \"" + id + "\".");
                return null;
            }
        }
        return itemFilters.get(id);
    }

    public void initialize(final ItemFilterParser parser, String input) {
        parser.parseRows(input, itemFilters);
    }
}