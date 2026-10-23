package com.gpl.rpg.AndorsTrail.model.item;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.model.ChecksumBuilder;
import com.gpl.rpg.AndorsTrail.savegames.LegacySavegameFormatReaderForItemContainer;

public final class Inventory extends ItemContainer {
	public static final int NUM_EQUIPMENT_PRESETS = 5;

	public static enum WearSlot {
		weapon
		,shield
		,head
		,body
		,hand
		,feet
		,neck
		,leftring
		,rightring;
		public static WearSlot fromString(String s, WearSlot default_) {
			if (s == null) return default_;
			return valueOf(s);
		}
	}

	public int gold = 0;
	private static final int NUM_WORN_SLOTS = WearSlot.values().length;
	public static final int NUM_QUICK_SLOTS = 3;
	private final ItemType[] wear = new ItemType[NUM_WORN_SLOTS];
	private static final class EquipmentPreset {
		boolean saved;
		String presetName;
		final String[] itemTypeIDs = new String[NUM_WORN_SLOTS];
	}
	private final EquipmentPreset[] equipmentPresets = new EquipmentPreset[NUM_EQUIPMENT_PRESETS];
	public final ItemType[] quickitem = new ItemType[NUM_QUICK_SLOTS];

	public Inventory() {
		for (int preset = 0; preset < NUM_EQUIPMENT_PRESETS; ++preset) equipmentPresets[preset] = new EquipmentPreset();
	}

	public void clear() {
		for(int i = 0; i < NUM_WORN_SLOTS; ++i) wear[i] = null;
		for(int i = 0; i < NUM_QUICK_SLOTS; ++i) quickitem[i] = null;
		gold = 0;
		items.clear();
	}

	public void add(final Loot loot) {
		this.gold += loot.gold;
		this.add(loot.items);
	}

	public boolean isEmptySlot(WearSlot slot) {
		return wear[slot.ordinal()] == null;
	}

	public ItemType getItemTypeInWearSlot(WearSlot slot) {
		return wear[slot.ordinal()];
	}
	public void setItemTypeInWearSlot(WearSlot slot, ItemType type) {
		wear[slot.ordinal()] = type;
	}

	public void saveEquipmentPreset(int preset) {
		checkPreset(preset);
		equipmentPresets[preset].saved = true;
		for (WearSlot slot : WearSlot.values()) {
			ItemType type = getItemTypeInWearSlot(slot);
			equipmentPresets[preset].itemTypeIDs[slot.ordinal()] = type == null ? null : type.id;
		}
	}

	public boolean isEquipmentPresetSaved(int preset) {
		checkPreset(preset);
		return equipmentPresets[preset].saved;
	}

	public String getEquipmentPresetItemTypeID(int preset, WearSlot slot) {
		checkPreset(preset);
		return equipmentPresets[preset].itemTypeIDs[slot.ordinal()];
	}

	public String getEquipmentPresetName(int preset) {
		checkPreset(preset);
		return equipmentPresets[preset].presetName;
	}

	public void setEquipmentPresetName(int preset, String presetName) {
		checkPreset(preset);
		equipmentPresets[preset].presetName = presetName;
	}

	private static void checkPreset(int preset) {
		if (preset < 0 || preset >= NUM_EQUIPMENT_PRESETS) throw new IllegalArgumentException("Invalid equipment preset: " + preset);
	}

	public boolean isWearing(String itemTypeID, int minNumber) {
		for(int i = 0; i < NUM_WORN_SLOTS; ++i) {
			if (wear[i] == null) continue;
			if (wear[i].id.equals(itemTypeID)) minNumber--;
		}
		return minNumber <= 0;
	}

	public static boolean isArmorSlot(WearSlot slot) {
		if (slot == null) return false;
		switch (slot) {
			case head:
			case body:
			case hand:
			case feet:
				return true;
			default:
				return false;
		}
	}


	// Move to item container?
	public Inventory buildQuestItems() {
		Inventory questItems = new Inventory();
		for (ItemEntry i : this.items) {
			if (i == null) break;
			if (i.itemType.isQuestItem())
				questItems.items.add(i);
		}
		return questItems;
	}
	// Move to item container?
	public Inventory buildJewelryItems() {
		Inventory jewelryItems = new Inventory();
		for (ItemEntry i : this.items) {
			if (i == null) break;
			if (i.itemType.isEquippable() && !i.itemType.isWeapon() && !i.itemType.isArmor() && !i.itemType.isShield())
				jewelryItems.items.add(i);
		}
		return jewelryItems;
	}
	// Move to item container?
	public Inventory buildPotionItems() {
		Inventory potionItems = new Inventory();
		for (ItemEntry i : this.items) {
			if (i == null) break;
			if (i.itemType.isUsable() && ("pot".equals(i.itemType.category.id) || "healing".equals(i.itemType.category.id)))
				potionItems.items.add(i);
		}
		return potionItems;
	}
	// Move to item container?
	public Inventory buildFoodItems() {
		Inventory foodItems = new Inventory();
		for (ItemEntry i : this.items) {
			if (i == null) break;
			if (i.itemType.isUsable() && !("pot".equals(i.itemType.category.id) || "healing".equals(i.itemType.category.id)))
				foodItems.items.add(i);
		}
		return foodItems;
	}
	// Move to item container?
	public Inventory buildWeaponItems() {
		Inventory weaponItems = new Inventory();
		for (ItemEntry i : this.items) {
			if (i == null) break;
			if (i.itemType.isWeapon())
				weaponItems.items.add(i);
		}
		return weaponItems;
	}
	// Move to item container?
	public Inventory buildArmorItems() {
		Inventory armorItems = new Inventory();
		for (ItemEntry i : this.items) {
			if (i == null) break;
			if (i.itemType.isArmor() || i.itemType.isShield())
				armorItems.items.add(i);
		}
		return armorItems;
	}
	// Move to item container?
	public Inventory buildOtherItems() {
		Inventory otherItems = new Inventory();
		for (ItemEntry i : this.items) {
			if (i == null) break;
			if (i.itemType.isEquippable() || i.itemType.isUsable() || i.itemType.isQuestItem())
				continue;
			otherItems.items.add(i);
		}
		return otherItems;
	}

	// ====== PARCELABLE ===================================================================

	public Inventory(DataInputStream src, WorldContext world, int fileversion) throws IOException {
		this.readFromParcel(src, world, fileversion);
	}

	@Override
	public void readFromParcel(DataInputStream src, WorldContext world, int fileversion) throws IOException {
		super.readFromParcel(src, world, fileversion);
		gold = src.readInt();

		if (fileversion < 23) LegacySavegameFormatReaderForItemContainer.refundUpgradedItems(this);

		for(int i = 0; i < NUM_WORN_SLOTS; ++i) {
			wear[i] = null;
		}
		final int numWornSlots = src.readInt();
		for(int i = 0; i < numWornSlots; ++i) {
			if (src.readBoolean()) {
				wear[i] = world.itemTypes.getItemType(src.readUTF());
			}
		}
		for(int i = 0; i < NUM_QUICK_SLOTS; ++i) {
			quickitem[i] = null;
		}
		if (fileversion >= 19) {
			final int quickSlots = src.readInt();
			for(int i = 0; i < quickSlots; ++i) {
				if (src.readBoolean()) {
					quickitem[i] = world.itemTypes.getItemType(src.readUTF());
				}
			}
		}
		if (fileversion >= 90) {
			final int numPresets = src.readInt();
			final int numPresetSlots = src.readInt();
			if (numPresets < 0 || numPresetSlots < 0) throw new IOException("Invalid equipment preset dimensions");
			for (int preset = 0; preset < numPresets; ++preset) {
				final boolean presetSaved = src.readBoolean();
				final String presetName = src.readBoolean() ? src.readUTF() : null;
				if (preset < NUM_EQUIPMENT_PRESETS) {
					EquipmentPreset equipmentPreset = equipmentPresets[preset];
					equipmentPreset.saved = presetSaved;
					equipmentPreset.presetName = presetName;
				}
				for (int slot = 0; slot < numPresetSlots; ++slot) {
					final String itemTypeID = src.readBoolean() ? src.readUTF() : null;
					if (preset < NUM_EQUIPMENT_PRESETS && slot < NUM_WORN_SLOTS) equipmentPresets[preset].itemTypeIDs[slot] = itemTypeID;
				}
			}
		}
	}

	@Override
	public void writeToParcel(DataOutputStream dest) throws IOException {
		super.writeToParcel(dest);
		dest.writeInt(gold);
		dest.writeInt(NUM_WORN_SLOTS);
		for(int i = 0; i < NUM_WORN_SLOTS; ++i) {
			if (wear[i] != null) {
				dest.writeBoolean(true);
				dest.writeUTF(wear[i].id);
			} else {
				dest.writeBoolean(false);
			}
		}
		dest.writeInt(NUM_QUICK_SLOTS);
		for(int i = 0; i < NUM_QUICK_SLOTS; ++i) {
			if (quickitem[i] != null) {
				dest.writeBoolean(true);
				dest.writeUTF(quickitem[i].id);
			} else {
				dest.writeBoolean(false);
			}
		}
		dest.writeInt(NUM_EQUIPMENT_PRESETS);
		dest.writeInt(NUM_WORN_SLOTS);
		for (int preset = 0; preset < NUM_EQUIPMENT_PRESETS; ++preset) {
			dest.writeBoolean(equipmentPresets[preset].saved);
			String presetName = equipmentPresets[preset].presetName;
			dest.writeBoolean(presetName != null);
			if (presetName != null) dest.writeUTF(presetName);
			for (int slot = 0; slot < NUM_WORN_SLOTS; ++slot) { String id = equipmentPresets[preset].itemTypeIDs[slot]; dest.writeBoolean(id != null); if (id != null) dest.writeUTF(id); }
		}
	}
	public void addToChecksum(ChecksumBuilder builder) {
		addToChecksum(builder, true);
	}
	public void addToChecksum(ChecksumBuilder builder, boolean includeEquipmentPresets) {
		super.addToChecksum(builder);
		builder.add(gold);
		builder.add(NUM_WORN_SLOTS);
		for(int i = 0; i < NUM_WORN_SLOTS; ++i) {
			if (wear[i] != null) {
				builder.add(wear[i].id);
			}
		}
		builder.add(NUM_QUICK_SLOTS);
		for(int i = 0; i < NUM_QUICK_SLOTS; ++i) {
			if (quickitem[i] != null) {
				builder.add(quickitem[i].id);
			}
		}
		if (includeEquipmentPresets) {
			for (int preset = 0; preset < NUM_EQUIPMENT_PRESETS; ++preset) {
				builder.add(equipmentPresets[preset].saved);
				builder.add(equipmentPresets[preset].presetName);
				for (int slot = 0; slot < NUM_WORN_SLOTS; ++slot) builder.add(equipmentPresets[preset].itemTypeIDs[slot]);
			}
		}

	}
}
