package com.gpl.rpg.AndorsTrail.model.item;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.model.ChecksumBuilder;
import com.gpl.rpg.AndorsTrail.model.actor.Player;

public class ItemContainer {
	public final ArrayList<ItemEntry> items = new ArrayList<ItemEntry>();

	public ItemContainer() {}

	public int countItems() {
		int result = 0;
		for (ItemEntry i : items) {
			result += i.quantity;
		}
		return result;
	}

	public static final class ItemEntry {
		public final ItemType itemType;
		public int quantity;
		public ItemEntry(ItemType itemType, int initialQuantity) {
			this.itemType = itemType;
			this.quantity = initialQuantity;
		}

		// ====== PARCELABLE ===================================================================

		public ItemEntry(DataInputStream src, WorldContext world, int fileversion) throws IOException {
			this.itemType = world.itemTypes.getItemType(src.readUTF());
			this.quantity = src.readInt();
		}

		public void writeToParcel(DataOutputStream dest) throws IOException {
			dest.writeUTF(itemType.id);
			dest.writeInt(quantity);
		}
		public void addToChecksum(ChecksumBuilder builder) {
			builder.add(itemType.id);
			builder.add(quantity);
		}
	}

	public void addItem(ItemType itemType, int quantity) {
		if (quantity == 0) return;

		ItemEntry e = findItem(itemType.id);
		if (e != null) {
			e.quantity += quantity;
		} else {
			items.add(new ItemEntry(itemType, quantity));
		}
	}
	public void addItem(ItemType itemType) { addItem(itemType, 1); }
	public void add(final ItemContainer items) {
		for (ItemEntry e : items.items) {
			addItem(e.itemType, e.quantity);
		}
	}
	public boolean isEmpty() { return items.isEmpty(); }

	public boolean removeItem(String itemTypeID) { return removeItem(itemTypeID, 1); }
	public boolean removeItem(String itemTypeID, int quantity) {
		int index = -1;
		ItemEntry e = null;
		for (int i = 0; i < items.size(); ++i) {
			e = items.get(i);
			if (e.itemType.id.equals(itemTypeID)) {
				index = i;
				break;
			}
		}
		if (index < 0) return false;
		if (e.quantity == quantity) {
			items.remove(index);
		} else if (e.quantity > quantity) {
			e.quantity -= quantity;
		} else {
			return false;
		}
		return true;
	}

	public ItemEntry findItem(String itemTypeID) {
		for (ItemEntry e : items) {
			if (e.itemType.id.equals(itemTypeID)) return e;
		}
		return null;
	}
	public int findItemIndex(String itemTypeID) {
		for (int i = 0; i < items.size(); ++i) {
			if (items.get(i).itemType.id.equals(itemTypeID)) return i;
		}
		return -1;
	}
	public boolean hasItem(String itemTypeID) { return findItem(itemTypeID) != null; }
	public boolean hasItem(String itemTypeID, int minimumQuantity) {
		return getItemQuantity(itemTypeID) >= minimumQuantity;
	}

	public int getItemQuantity(String itemTypeID) {
		ItemEntry e = findItem(itemTypeID);
		if (e == null) return 0;
		return e.quantity;
	}

	public void sortToTop(String itemTypeID) {
		int i = findItemIndex(itemTypeID);
		if (i <= 0) return;
		items.add(0, items.remove(i));
	}

	public void sortToBottom(String itemTypeID) {
		int i = findItemIndex(itemTypeID);
		if (i < 0) return;
		items.add(items.remove(i));
	}
	
	public ItemContainer usableItems() {
		ItemContainer usableContainer = new ItemContainer();
		for (ItemEntry item : items) {
			if (item.itemType.isUsable()) {
				usableContainer.addItem(item.itemType, item.quantity);
			}
		}
		return usableContainer;
	}


	public void sortByName(Player p) {
		final Player q = p;
		Comparator<ItemEntry> comparatorName = new Comparator<ItemEntry>() {
			@Override
			public int compare(ItemEntry item1, ItemEntry item2) {
				return item1.itemType.getName(q).compareTo(item2.itemType.getName(q));
			}
		};
		Collections.sort(this.items, comparatorName);

	}


	public void sortByPrice(Player p) {
		final Player q = p;
		Comparator<ItemEntry> comparatorPrice = new Comparator<ItemEntry>() {
			@Override
			public int compare(ItemEntry item1, ItemEntry item2) {
				// More expensive items go to top
				if (item1.itemType.baseMarketCost < item2.itemType.baseMarketCost) {
					return 1;
				} else if (item1.itemType.baseMarketCost > item2.itemType.baseMarketCost) {
					return -1;
				} else { // compares the names if rarity is the same
					return item1.itemType.getName(q).compareTo(item2.itemType.getName(q));
				}
			}
		};
		Collections.sort(this.items, comparatorPrice);

	}

	public void sortByQuantity(Player p) {
		final Player q = p;
		Comparator<ItemEntry> comparatorQuantity = new Comparator<ItemEntry>() {
			@Override
			public int compare(ItemEntry item1, ItemEntry item2) {
				// Bigger quantity is put first
				if (item1.quantity > item2.quantity) {
					return -1;
				} else if (item1.quantity < item2.quantity) {
					return 1;
				} else { // compares the names if quantity is the same
					return item1.itemType.getName(q).compareTo(item2.itemType.getName(q));
				}
			}
		};
		Collections.sort(this.items, comparatorQuantity);

	}


	public void sortByRarity(Player p) {
		final Player q = p;
		Comparator<ItemEntry> comparatorRarity = new Comparator<ItemEntry>() {
			@Override
			public int compare(ItemEntry item1, ItemEntry item2) {
				// More rare items go to top
				if (item1.itemType.displayType.compareTo(item2.itemType.displayType) != 0 ) {
					return (-1) * item1.itemType.displayType.compareTo(item2.itemType.displayType);
					// ^ More rare goes on top
				} else { // compares the names if rarity is the same
					return item1.itemType.getName(q).compareTo(item2.itemType.getName(q));
				}
			}
		};
		Collections.sort(this.items, comparatorRarity);

	}


	public void sortByType(Player p) {
		final Player q = p;
		Comparator<ItemEntry> comparatorType = new Comparator<ItemEntry>() {
			@Override
			public int compare(ItemEntry item1, ItemEntry item2) {
				if (determineType(item1) > determineType(item2)) {
					return 1;
				} else if (determineType(item1) < determineType(item2)) {
					return -666;
				} else { // compares the names if type is the same
					return item1.itemType.getName(q).compareTo(item2.itemType.getName(q));
				}
			}
		};
		Collections.sort(this.items, comparatorType);

	}

	public int determineType(ItemEntry item) {
		if (item.itemType.isEquippable()) {
			switch (item.itemType.category.inventorySlot) {
				case weapon:
					if (item.itemType.isTwohandWeapon()) {
						return 100;
					} else {
						return 110;
					}
				case shield: return 200;
				case head: return 210;
				case body: return 220;
				case hand: return 230;
				case feet: return 240;
				case neck: return 250;
				case leftring: return 260;
				case rightring: return 260; // Note: not used - all rings are leftring by category
				default: return 270;
			}
		} else if (item.itemType.isUsable()) {
			if ("pot".equals(item.itemType.category.id) || "healing".equals(item.itemType.category.id)) {
				return 300;
			} else {
				return 310;
			}
		} else if (item.itemType.isQuestItem()) {
			return 400;
		} else {
			return 500;
		}
	}

	public void sortByReverse() {
		Collections.reverse(this.items);
	}



	// ====== PARCELABLE ===================================================================

	public static ItemContainer newFromParcel(DataInputStream src, WorldContext world, int fileversion) throws IOException {
		ItemContainer result = new ItemContainer();
		result.readFromParcel(src, world, fileversion);
		return result;
	}

	protected void readFromParcel(DataInputStream src, WorldContext world, int fileversion) throws IOException {
		items.clear();
		final int size = src.readInt();
		for(int i = 0; i < size; ++i) {
			ItemEntry entry = new ItemEntry(src, world, fileversion);
			if (entry.itemType != null) items.add(entry);
		}
	}

	public void writeToParcel(DataOutputStream dest) throws IOException {
		dest.writeInt(items.size());
		for (ItemEntry e : items) {
			e.writeToParcel(dest);
		}
	}
	public void addToChecksum(ChecksumBuilder builder) {
		builder.add(items.size());
		for (ItemEntry e : items) {
			e.addToChecksum(builder);
		}
	}
}
