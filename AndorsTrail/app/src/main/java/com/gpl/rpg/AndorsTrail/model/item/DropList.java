package com.gpl.rpg.AndorsTrail.model.item;

import com.gpl.rpg.AndorsTrail.controller.Constants;
import com.gpl.rpg.AndorsTrail.controller.SkillController;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.util.ConstRange;

public final class DropList {
	private final DropItem[] items;

	private final ItemTypeCollection itemTypeCollection;
	private final ItemFilterCollection itemFilterCollection;

	public DropList(
			DropItem[] items
			, ItemTypeCollection itemTypeCollection
			, ItemFilterCollection itemFilterCollection
	) {
		this.items = items;
		this.itemTypeCollection = itemTypeCollection;
		this.itemFilterCollection = itemFilterCollection;
	}

	public Integer getMarketCostMarkupPercent(String itemTypeID) {
		for (DropItem item : items) {
			if (item.marketCostMarkupPercent == null) continue;
			if (item.itemTypeID.equals(itemTypeID)) return item.marketCostMarkupPercent;
			if (!ItemTypeCollection.isItemFilter(item.itemTypeID)) continue;

			ItemFilter filter = itemFilterCollection.getItemFilter(item.itemTypeID);
			if (filter == null) continue;
			for (ItemType filteredItem : filter.getItemTypes()) {
				if (filteredItem.id.equals(itemTypeID)) return item.marketCostMarkupPercent;
			}
		}
		return null;
	}

	public void createRandomLoot(Loot loot, Player player) {
		for (DropItem item : items) {

			if (ItemTypeCollection.isItemFilter(item.itemTypeID)) {
				ItemFilter itemFilter = itemFilterCollection.getItemFilter(item.itemTypeID);
				item = new DropItem(
						itemFilter.getRandomItem().id
						, item.chance
						, item.quantity
						, item.marketCostMarkupPercent
				);
			}

			final int chanceRollBias = SkillController.getDropChanceRollBias(item, itemTypeCollection.getItemType(item.itemTypeID), player);
			if (Constants.rollResult(item.chance, chanceRollBias)) {

				final int quantityRollBias = SkillController.getDropQuantityRollBias(item, player);
				int quantity = Constants.rollValue(item.quantity, quantityRollBias);

				ItemType addItem = itemTypeCollection.getItemType(item.itemTypeID);
				if (addItem != null) {
					loot.add(addItem, quantity);
				}
			}
		}
	}

	// Unit test method. Not part of the game logic.
	public DropItem[] UNITTEST_getAllDropItems() {
		return items;
	}

	public static class DropItem {
		public final String itemTypeID;
		public final ConstRange chance;
		public final ConstRange quantity;
		public final Integer marketCostMarkupPercent;
		public DropItem(String itemTypeID, ConstRange chance, ConstRange quantity, Integer marketCostMarkupPercent) {
			this.itemTypeID = itemTypeID;
			this.chance = chance;
			this.quantity = quantity;
			this.marketCostMarkupPercent = marketCostMarkupPercent;
		}
	}
}
