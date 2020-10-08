package com.gpl.rpg.AndorsTrail.controller;

import java.util.ArrayList;
import java.util.Collection;

import com.gpl.rpg.AndorsTrail.AndorsTrailPreferences;
import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.listeners.QuickSlotListeners;
import com.gpl.rpg.AndorsTrail.model.ModelContainer;
import com.gpl.rpg.AndorsTrail.model.ability.SkillCollection;
import com.gpl.rpg.AndorsTrail.model.ability.traits.AbilityModifierTraits;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.model.item.Inventory;
import com.gpl.rpg.AndorsTrail.model.item.ItemContainer;
import com.gpl.rpg.AndorsTrail.model.item.ItemContainer.ItemEntry;
import com.gpl.rpg.AndorsTrail.model.item.ItemTraits_OnHitReceived;
import com.gpl.rpg.AndorsTrail.model.item.ItemTraits_OnUse;
import com.gpl.rpg.AndorsTrail.model.item.ItemType;
import com.gpl.rpg.AndorsTrail.model.item.Loot;

import static java.lang.Math.min;

public final class ItemController {

	private final ControllerContext controllers;
	private final WorldContext world;
	public final QuickSlotListeners quickSlotListeners = new QuickSlotListeners();

	public ItemController(ControllerContext controllers, WorldContext world) {
		this.controllers = controllers;
		this.world = world;
	}

	public void dropItem(ItemType type, int quantity) {
		if (world.model.player.inventory.getItemQuantity(type.id) < quantity) return;
		world.model.player.inventory.removeItem(type.id, quantity);
		world.model.currentMaps.map.itemDropped(type, quantity, world.model.player.position);
	}

	public void equipItem(ItemType type, Inventory.WearSlot slot) {
		if (!type.isEquippable()) return;
		final Player player = world.model.player;
		if (world.model.uiSelections.isInCombat) {
			boolean changed = controllers.actorStatsController.useAPs(player, player.getReequipCost());
			if (!changed) return;
		}

		if (!player.inventory.removeItem(type.id, 1)) return;

		unequipSlot(player, slot);
		if (type.isTwohandWeapon()) unequipSlot(player, Inventory.WearSlot.shield);
		else if (slot == Inventory.WearSlot.shield) {
			ItemType currentWeapon = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon);
			if (currentWeapon != null && currentWeapon.isTwohandWeapon()) unequipSlot(player, Inventory.WearSlot.weapon);
		}

		player.inventory.setItemTypeInWearSlot(slot, type);
		controllers.actorStatsController.addConditionsFromEquippedItem(player, type);
		controllers.actorStatsController.recalculatePlayerStats(player);
		if (world.model.uiSelections.isInCombat && !controllers.combatController.playerHasApLeft()) {
			controllers.combatController.endPlayerTurn();
		}
	}

	public void unequipSlot(ItemType type, Inventory.WearSlot slot) {
		if (!type.isEquippable()) return;
		final Player player = world.model.player;
		if (player.inventory.isEmptySlot(slot)) return;

		if (world.model.uiSelections.isInCombat) {
			boolean changed = controllers.actorStatsController.useAPs(player, player.getReequipCost());
			if (!changed) return;
		}

		unequipSlot(player, slot);
		controllers.actorStatsController.recalculatePlayerStats(player);
		if (world.model.uiSelections.isInCombat && !controllers.combatController.playerHasApLeft()) {
			controllers.combatController.endPlayerTurn();
		}
	}

	private void unequipSlot(Player player, Inventory.WearSlot slot) {
		ItemType removedItemType = player.inventory.getItemTypeInWearSlot(slot);
		if (removedItemType == null) return;
		player.inventory.addItem(removedItemType);
		player.inventory.setItemTypeInWearSlot(slot, null);
		controllers.actorStatsController.removeConditionsFromUnequippedItem(player, removedItemType);
	}

	public void useItem(ItemType type) {
		if (!type.isUsable()) return;
		final Player player = world.model.player;
		if (world.model.uiSelections.isInCombat) {
			boolean changed = controllers.actorStatsController.useAPs(player, player.getUseItemCost());
			if (!changed) return;
		}

		if (!player.inventory.removeItem(type.id, 1)) return;

		controllers.actorStatsController.applyUseEffect(player, null, type.effects_use);
		world.model.statistics.addItemUsage(type);

		if (world.model.uiSelections.isInCombat && !controllers.combatController.playerHasApLeft()) {
			controllers.combatController.endPlayerTurn();
		}
		//TODO: provide feedback that the item has been used.
		//context.mainActivity.message(androidContext.getResources().getString(R.string.inventory_item_used, type.name));
	}

	public void playerSteppedOnLootBag(Loot loot) {
		if (pickupLootBagWithoutConfirmation(loot)) {
			controllers.mapController.worldEventListeners.onPlayerPickedUpGroundLoot(loot);
			pickupAll(loot);
			removeLootBagIfEmpty(loot);
		} else {
			controllers.mapController.worldEventListeners.onPlayerSteppedOnGroundLoot(loot);
			consumeNonItemLoot(loot);
		}
	}

	public void lootMonsterBags(Collection<Loot> killedMonsterBags, int totalExpThisFight) {
		if (pickupLootBagsWithoutConfirmation(killedMonsterBags)) {
			controllers.mapController.worldEventListeners.onPlayerPickedUpMonsterLoot(killedMonsterBags, totalExpThisFight);
			pickupAll(killedMonsterBags);
			removeLootBagIfEmpty(killedMonsterBags);
			controllers.gameRoundController.resume();
		} else {
			controllers.mapController.worldEventListeners.onPlayerFoundMonsterLoot(killedMonsterBags, totalExpThisFight);
			consumeNonItemLoot(killedMonsterBags);
		}
	}

	private boolean pickupLootBagWithoutConfirmation(Loot bag) {
		if (bag.isContainer()) return false;
		switch (controllers.preferences.displayLoot) {
			case AndorsTrailPreferences.DISPLAYLOOT_DIALOG_ALWAYS:
				return false;
			case AndorsTrailPreferences.DISPLAYLOOT_DIALOG_FOR_ITEMS:
			case AndorsTrailPreferences.DISPLAYLOOT_DIALOG_FOR_ITEMS_ELSE_TOAST:
				if (bag.hasItems()) return false;
		}
		return true;
	}

	private boolean pickupLootBagsWithoutConfirmation(Collection<Loot> bags) {
		if (controllers.preferences.displayLoot == AndorsTrailPreferences.DISPLAYLOOT_DIALOG_ALWAYS) return false;
		for (Loot bag : bags) {
			if (!pickupLootBagWithoutConfirmation(bag)) return false;
		}
		return true;
	}

	public void applyInventoryEffects(Player player) {
		ItemType weapon = getMainWeapon(player);
		if (weapon != null) {
			player.attackCost = 0;
			player.criticalMultiplier = weapon.effects_equip.stats.setCriticalMultiplier;
		}

		applyInventoryEffects(player, Inventory.WearSlot.weapon);
		applyInventoryEffects(player, Inventory.WearSlot.shield);
		SkillController.applySkillEffectsFromFightingStyles(player);
		applyInventoryEffects(player, Inventory.WearSlot.head);
		applyInventoryEffects(player, Inventory.WearSlot.body);
		applyInventoryEffects(player, Inventory.WearSlot.hand);
		applyInventoryEffects(player, Inventory.WearSlot.feet);
		applyInventoryEffects(player, Inventory.WearSlot.neck);
		applyInventoryEffects(player, Inventory.WearSlot.leftring);
		applyInventoryEffects(player, Inventory.WearSlot.rightring);
		SkillController.applySkillEffectsFromItemProficiencies(player);
	}
	public static ItemType getMainWeapon(Player player) {
		ItemType itemType = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon);
		if (itemType != null) return itemType;
		itemType = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.shield);
		if (itemType != null && itemType.isWeapon()) return itemType;
		return null;
	}

	private void applyInventoryEffects(Player player, Inventory.WearSlot slot) {
		ItemType type = player.inventory.getItemTypeInWearSlot(slot);
		if (type == null) return;
		if (slot == Inventory.WearSlot.shield) {
			ItemType mainHandItem = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon);
			// The stats for off-hand weapons will be added later in SkillController.applySkillEffectsFromFightingStyles
			if (SkillController.isDualWielding(mainHandItem, type)) return;
		}
		if (type.effects_equip != null && type.effects_equip.stats != null) {
            controllers.actorStatsController.applyAbilityEffects(player, type.effects_equip.stats, 1);
            if (type.isWeapon()) {
                controllers.actorStatsController.addPlayerWeaponDamage(player, type.effects_equip.stats.increaseMinDamage, type.effects_equip.stats.increaseMaxDamage);
            }
        }
	}

	public static void recalculateHitEffectsFromWornItems(Player player) {
		ArrayList<ItemTraits_OnUse> effects_onHit = null;
		ArrayList<ItemTraits_OnHitReceived> effects_onHitReceived = null;
		for (Inventory.WearSlot slot : Inventory.WearSlot.values()) {
			ItemType type = player.inventory.getItemTypeInWearSlot(slot);
			if (type == null) continue;
			ItemTraits_OnUse eh = type.effects_hit;
			ItemTraits_OnHitReceived ehr = type.effects_hitReceived;
			if (eh == null && ehr == null) continue;

			if (effects_onHit == null) effects_onHit = new ArrayList<ItemTraits_OnUse>();
			if (eh != null) effects_onHit.add(eh);
			
			if (effects_onHitReceived == null) effects_onHitReceived = new ArrayList<ItemTraits_OnHitReceived>();
			if (ehr != null) effects_onHitReceived.add(ehr);
		}

		if (effects_onHit != null) {
			ItemTraits_OnUse[] effects_ = new ItemTraits_OnUse[effects_onHit.size()];
			effects_ = effects_onHit.toArray(effects_);
			player.onHitEffects = effects_;
		} else {
			player.onHitEffects = null;
		}

		if (effects_onHitReceived != null) {
			ItemTraits_OnHitReceived[] effects_ = new ItemTraits_OnHitReceived[effects_onHitReceived.size()];
			effects_ = effects_onHitReceived.toArray(effects_);
			player.onHitReceivedEffects = effects_;
		} else {
			player.onHitReceivedEffects = null;
		}
	}

	public void consumeNonItemLoot(Loot loot) {
		// Experience will be given as soon as the monster is killed.
		world.model.player.inventory.gold += loot.gold;
		loot.gold = 0;
		removeLootBagIfEmpty(loot);
	}
	public void consumeNonItemLoot(Iterable<Loot> lootBags) {
		for(Loot l : lootBags) {
			consumeNonItemLoot(l);
		}
	}

	public void pickupAll(Loot loot) {
		world.model.player.inventory.add(loot.items);
		consumeNonItemLoot(loot);
		checkQuickslotItemLooted(loot.items);
		loot.clear();
	}
	public void pickupAll(Iterable<Loot> lootBags) {
		for(Loot l : lootBags) {
			pickupAll(l);
		}
	}
	public boolean removeLootBagIfEmpty(final Loot loot) {
		if (loot.hasItemsOrGold()) return false;

		world.model.currentMaps.map.removeGroundLoot(loot);
		controllers.mapController.mapLayoutListeners.onLootBagRemoved(world.model.currentMaps.map, loot.position);
		return true; // The bag was removed.
	}

	public boolean removeLootBagIfEmpty(final Iterable<Loot> lootBags) {
		boolean isEmpty = true;
		for (Loot l : lootBags) {
			if (!removeLootBagIfEmpty(l)) isEmpty = false;
		}
		return isEmpty;
	}

	private static int getMarketPriceFactor(Player player) {
		return Constants.MARKET_PRICEFACTOR_PERCENT
			- player.getSkillLevel(SkillCollection.SkillID.barter) * SkillCollection.PER_SKILLPOINT_INCREASE_BARTER_PRICEFACTOR_PERCENTAGE;
	}
	public static int getBuyingPrice(Player player, ItemType itemType) {
		return itemType.baseMarketCost + itemType.baseMarketCost * getMarketPriceFactor(player) / 100;
	}
	public static int getSellingPrice(Player player, ItemType itemType) {
		return itemType.baseMarketCost - itemType.baseMarketCost * getMarketPriceFactor(player) / 100;
	}

	public static boolean canAfford(Player player, ItemType itemType) {
		return player.inventory.gold >= getBuyingPrice(player, itemType);
	}
	public static boolean canAfford(Player player, int price) {
		return player.inventory.gold >= price;
	}
	public static boolean maySellItem(Player player, ItemType itemType) {
		if (!itemType.isSellable()) return false;
		return true;
	}
	public static boolean sell(Player player, ItemType itemType, ItemContainer merchant, int quantity) {
		int price = getSellingPrice(player, itemType) * quantity;
		if (!maySellItem(player, itemType)) return false;
		if (!player.inventory.removeItem(itemType.id, quantity)) return false;
		player.inventory.gold += price;
		merchant.addItem(itemType, quantity);
		return true;
	}
	public static boolean buy(ModelContainer model, Player player, ItemType itemType, ItemContainer merchant, int quantity) {
		int price = getBuyingPrice(player, itemType) * quantity;
		if (!canAfford(player, price)) return false;
		if (!merchant.removeItem(itemType.id, quantity)) return false;
		player.inventory.gold -= price;
		player.inventory.addItem(itemType, quantity);
		model.statistics.addGoldSpent(price);
		return true;
	}


	public static String describeItemForListView(ItemEntry item, Player player) {
		StringBuilder sb = new StringBuilder(item.itemType.getName(player));
		if (item.quantity > 1) {
			sb.append(" (");
			sb.append(item.quantity);
			sb.append(')');
		}
		if (item.itemType.effects_equip != null) {
			AbilityModifierTraits t = item.itemType.effects_equip.stats;
			if (t != null) {
				if (t.increaseAttackChance != 0
					|| t.increaseMinDamage != 0
					|| t.increaseMaxDamage != 0
					|| t.increaseCriticalSkill != 0
					|| t.setCriticalMultiplier != 0) {
					sb.append(" [");
					describeAttackEffect(t.increaseAttackChance, t.increaseMinDamage, t.increaseMaxDamage, t.increaseCriticalSkill, t.setCriticalMultiplier, sb);
					sb.append(']');
				}
				if (t.increaseBlockChance != 0
					|| t.increaseDamageResistance != 0) {
					sb.append(" [");
					describeBlockEffect(t.increaseBlockChance, t.increaseDamageResistance, sb);
					sb.append(']');
				}
			}
		}
		return sb.toString();
	}

	public static void describeAttackEffect(int attackChance, int minDamage, int maxDamage, int criticalSkill, float criticalMultiplier, StringBuilder sb) {
		boolean addSpace = false;
		if (attackChance != 0) {
			sb.append(attackChance);
			addSpace = true;
		}
		if (minDamage != 0 || maxDamage != 0) {
			if (addSpace) sb.append(' ');
			sb.append(minDamage);
			if (minDamage != maxDamage) {
				sb.append('-');
				sb.append(maxDamage);
			}
			addSpace = true;
		}
		if (criticalSkill != 0) {
			if (addSpace) sb.append(' ');
			if (criticalSkill >= 0) {
				sb.append('+');
			}
			sb.append(criticalSkill);
		}
		if (criticalMultiplier != 0 && criticalMultiplier != 1) {
			sb.append('x');
			sb.append(criticalMultiplier);
		}
	}

	public static void describeBlockEffect(int blockChance, int damageResistance, StringBuilder sb) {
		if (blockChance != 0) {
			sb.append(blockChance);
		}
		if (damageResistance != 0) {
			sb.append('/');
			sb.append(damageResistance);
		}
	}

	public void quickitemUse(int quickSlotId) {
		useItem(world.model.player.inventory.quickitem[quickSlotId]);
		quickSlotListeners.onQuickSlotUsed(quickSlotId);
	}

	public void setQuickItem(ItemType itemType, int quickSlotId) {
		world.model.player.inventory.quickitem[quickSlotId] = itemType;
		quickSlotListeners.onQuickSlotChanged(quickSlotId);
	}
	
	private void checkQuickslotItemLooted(ItemContainer items) {
		for (ItemEntry item : items.items) {
			if (item.itemType.isUsable()) {
				for (int i = 0; i < world.model.player.inventory.quickitem.length; i++) {
					if (item.itemType == world.model.player.inventory.quickitem[i]) {
						quickSlotListeners.onQuickSlotChanged(i);
						
					}
				}
			}
		}
	}

	public int removeEquippedItem(String itemTypeID, int count) {
		int removed = 0;
		final Player player = world.model.player;
		for (Inventory.WearSlot slot : Inventory.WearSlot.values()) {
			ItemType type = player.inventory.getItemTypeInWearSlot(slot);
			if (type != null && type.id.equals(itemTypeID)) {
				player.inventory.setItemTypeInWearSlot(slot, null);
				controllers.actorStatsController.removeConditionsFromUnequippedItem(player, type);
				controllers.actorStatsController.recalculatePlayerStats(player);
				removed++;
				if (removed >= count) {
					break;
				}
			}
		}
		return removed;
	}
	public static void applyDamageModifier(Player player) {
		ItemType itemType = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon);
		int modifier1 = -1;
		int modifier2 = -1;
		if (itemType != null) modifier1 = itemType.effects_equip.stats.setNonWeaponDamageModifier;
		itemType = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.shield);
		if (itemType != null && itemType.isWeapon()) modifier2 = itemType.effects_equip.stats.setNonWeaponDamageModifier;

		int modifier = 100;
		if (modifier1 >= 0 && modifier2 >= 0) modifier = Math.min(modifier1, modifier2);
		else if (modifier1 <= 0 && modifier2 >= 0) modifier = modifier2;
		else if (modifier2 <= 0 && modifier1 >= 0) modifier = modifier1;

		if (modifier != 100) {
			final int minBaseDamage = player.damagePotential.current - player.weaponDamage.current;
			final int maxBaseDamage = player.damagePotential.max - player.weaponDamage.max;
			player.damagePotential.add(Math.round(minBaseDamage * ((modifier - 100)/100f)), true);
			player.damagePotential.addToMax(Math.round(maxBaseDamage * ((modifier - 100)/100f)));
		}
	}
}
