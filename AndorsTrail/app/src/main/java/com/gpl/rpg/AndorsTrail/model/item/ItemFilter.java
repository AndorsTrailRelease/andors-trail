package com.gpl.rpg.AndorsTrail.model.item;

import com.gpl.rpg.AndorsTrail.controller.Constants;
import com.gpl.rpg.AndorsTrail.util.Range;

import java.util.Collections;
import java.util.List;

public final class ItemFilter {

    public final String id;
    private final List<ItemType> include;


    public ItemFilter(
            String id
            , List<ItemType> include
    ) {
        this.id = id;
        this.include = include;
    }

    public List<ItemType> getItemTypes() {
        if (include == null || include.isEmpty())
            return Collections.emptyList();
        return include;
    }

    public ItemType getRandomItem() {
        List<ItemType> allItemsInFilter = getItemTypes();
        int ran = Constants.rollValue(new Range(allItemsInFilter.size()-1, 0));
        return allItemsInFilter.get(ran);
    }
}