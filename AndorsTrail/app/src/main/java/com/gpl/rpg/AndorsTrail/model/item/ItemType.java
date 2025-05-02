package com.gpl.rpg.AndorsTrail.model.item;

import com.gpl.rpg.AndorsTrail.controller.Constants;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.resource.tiles.TileManager;

public final class ItemType {

	public static enum DisplayType {
		ordinary
		,quest
		,rare
		,extraordinary
		,legendary;

		public static DisplayType fromString(String s, DisplayType default_) {
			if (s == null) return default_;
			return valueOf(s);
		}
	}

	public final String id;
	public final int iconID;
	private final String name;
	private final String description;
	private final boolean hasPersonalizedName;
	public final ItemCategory category;
	public final boolean hasManualPrice;
	public final int baseMarketCost;
	public final int fixedBaseMarketCost;
	public final DisplayType displayType;

	public final ItemTraits_OnEquip effects_equip;
	public final ItemTraits_OnUse effects_use;
	public final ItemTraits_OnUse effects_hit;
	public final ItemTraits_OnUse effects_miss;
	public final ItemTraits_OnUse effects_kill;
	public final ItemTraits_OnHitReceived effects_hitReceived;
	public final ItemTraits_OnHitReceived effects_missReceived;

	public ItemType(
			String id
			, int iconID
			, String name
			, String description
			, ItemCategory category
			, DisplayType displayType
			, boolean hasManualPrice
			, int fixedBaseMarketCost
			, ItemTraits_OnEquip effects_equip
			, ItemTraits_OnUse effects_use
			, ItemTraits_OnUse effects_hit
			, ItemTraits_OnUse effects_miss
			, ItemTraits_OnUse effects_kill
			, ItemTraits_OnHitReceived effects_hitReceived
			, ItemTraits_OnHitReceived effects_missReceived
	) {
		this.id = id;
		this.iconID = iconID;
		this.name = name;
		this.description = description;
		this.category = category;
		this.displayType = displayType;
		this.hasManualPrice = hasManualPrice;
		this.baseMarketCost = hasManualPrice ? fixedBaseMarketCost : calculateCost(category, effects_equip, effects_use, effects_hit, effects_kill);
		this.fixedBaseMarketCost = fixedBaseMarketCost;
		this.effects_equip = effects_equip;
		this.effects_use = effects_use;
		this.effects_hit = effects_hit;
		this.effects_miss = effects_miss;
		this.effects_kill = effects_kill;
		this.effects_hitReceived = effects_hitReceived;
		this.effects_missReceived = effects_missReceived;
		this.hasPersonalizedName = name.contains(Constants.PLACEHOLDER_PLAYERNAME);
	}

	public boolean isEquippable() { return category.isEquippable(); }
	public boolean isUsable() { return category.isUsable(); }
	public boolean isQuestItem() { return displayType == DisplayType.quest; }
	public boolean isOrdinaryItem() { return displayType == DisplayType.ordinary; }
	public boolean isWeapon() { return category.isWeapon(); }
	public boolean isArmor() { return category.isArmor(); }
	public boolean isShield() { return category.isShield(); }
	public boolean isTwohandWeapon() { return category.isTwohandWeapon(); }
	public boolean isOffhandCapableWeapon() { return category.isOffhandCapableWeapon(); }
	public boolean isSellable() {
		if (isQuestItem()) return false;
		if (baseMarketCost == 0) return false;
		return true;
	}

	public String getDescription() { return description; }
	public String getName(Player p) {
		if (!hasPersonalizedName) return name;
		else return name.replace(Constants.PLACEHOLDER_PLAYERNAME, p.getName());
	}

	public int getOverlayTileID() {
		switch (displayType) {
		case quest:
			return TileManager.iconID_selection_yellow;
		case legendary:
			return TileManager.iconID_selection_green;
		case extraordinary:
			return TileManager.iconID_selection_blue;
		case rare:
			return TileManager.iconID_selection_purple;
		case ordinary:
		default:
			return -1;
		}
	}

	private static int calculateCost(ItemCategory category, ItemTraits_OnEquip effects_equip, ItemTraits_OnUse effects_use, ItemTraits_OnUse effects_hit, ItemTraits_OnUse effects_kill) {
		final int costEquipStats = effects_equip == null ? 0 : effects_equip.calculateEquipCost(category.isWeapon());
		final int costUse = effects_use == null ? 0 : effects_use.calculateUseCost();
		final int costHit = effects_hit == null ? 0 : effects_hit.calculateHitCost();
		final int costKill = effects_kill == null ? 0 : effects_kill.calculateKillCost();
		return Math.max(1, costEquipStats + costUse + costHit + costKill);
	}
}
