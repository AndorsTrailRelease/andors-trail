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

	public void createRandomLoot(Loot loot, Player player) {
		for (DropItem item : items) {

			if (ItemTypeCollection.isItemFilter(item.itemTypeID)) {
				ItemFilter itemFilter = itemFilterCollection.getItemFilter(item.itemTypeID);
				item = new DropItem(
						itemFilter.getRandomItem().id
						, item.chance
						, item.quantity
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
		public DropItem(String itemTypeID, ConstRange chance, ConstRange quantity) {
			this.itemTypeID = itemTypeID;
			this.chance = chance;
			this.quantity = quantity;
		}
	}
}
