package com.gpl.rpg.AndorsTrail.model.item;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.model.ChecksumBuilder;
import com.gpl.rpg.AndorsTrail.savegames.LegacySavegameFormatReaderForItemContainer;
import com.gpl.rpg.AndorsTrail.util.Coord;

public final class Loot {
	public int exp = 0;
	public int gold = 0;
	public final ItemContainer items;
	public final Coord position;
	public final boolean isVisible;

	public Loot() {
		this(true);
	}
	public Loot(boolean isVisible) {
		this.items = new ItemContainer();
		this.position = new Coord();
		this.isVisible = isVisible;
	}

	private void add(Loot l) {
		this.exp += l.exp;
		this.gold += l.gold;
		this.items.add(l.items);
	}

	public void add(ItemType itemType, int quantity) {
		if (ItemTypeCollection.isGoldItemType(itemType.id)) {
			gold += quantity;
		} else {
			items.addItem(itemType, quantity);
		}
	}

	public boolean hasItemsOrExp() {
		return exp != 0 || hasItemsOrGold();
	}
	public boolean hasItemsOrGold() {
		return gold != 0 || hasItems();
	}
	public boolean hasItems() {
		return !items.isEmpty();
	}
	public boolean isContainer() {
		return !isVisible;
	}
	public static Loot combine(Iterable<Loot> loot) {
		Loot result = new Loot();
		for (Loot l : loot) {
			result.add(l);
		}
		return result;
	}

	public void clear() {
		exp = 0;
		gold = 0;
		items.items.clear();
	}


	// ====== PARCELABLE ===================================================================

	public Loot(DataInputStream src, WorldContext world, int fileversion) throws IOException {
		this.exp = src.readInt();
		this.gold = src.readInt();
		this.items = ItemContainer.newFromParcel(src, world, fileversion);
		if (fileversion < 23) LegacySavegameFormatReaderForItemContainer.refundUpgradedItems(this);

		this.position = new Coord(src, fileversion);
		if (fileversion <= 15) {
			this.isVisible = true;
			return;
		}
		this.isVisible = src.readBoolean();
	}

	public void writeToParcel(DataOutputStream dest) throws IOException {
		dest.writeInt(exp);
		dest.writeInt(gold);
		items.writeToParcel(dest);
		position.writeToParcel(dest);
		dest.writeBoolean(isVisible);
	}

	public void addToChecksum(ChecksumBuilder builder) {
		builder.add(exp);
		builder.add(gold);
		items.addToChecksum(builder);
		position.addToChecksum(builder);
		builder.add(isVisible);
	}
}
