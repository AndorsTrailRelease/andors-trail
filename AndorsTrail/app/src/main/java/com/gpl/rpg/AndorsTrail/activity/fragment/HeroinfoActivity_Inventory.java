package com.gpl.rpg.AndorsTrail.activity.fragment;

import java.util.Arrays;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.Dialogs;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.activity.ItemInfoActivity;
import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.ItemController;
import com.gpl.rpg.AndorsTrail.model.actor.HeroCollection;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.model.item.Inventory;
import com.gpl.rpg.AndorsTrail.model.item.ItemContainer;
import com.gpl.rpg.AndorsTrail.model.item.ItemType;
import com.gpl.rpg.AndorsTrail.resource.tiles.TileCollection;
import com.gpl.rpg.AndorsTrail.view.CustomMenuInflater;
import com.gpl.rpg.AndorsTrail.view.ItemContainerAdapter;
import com.gpl.rpg.AndorsTrail.view.SpinnerEmulator;

public final class HeroinfoActivity_Inventory extends Fragment implements CustomMenuInflater.MenuItemSelectedListener {

	private static final int INTENTREQUEST_ITEMINFO = 3;
	private static final int INTENTREQUEST_BULKSELECT_DROP = 11;

	private WorldContext world;
	private ControllerContext controllers;
	private TileCollection wornTiles;

	private Player player;
	private ListView inventoryList;
	private ItemContainerAdapter inventoryListAdapter;
	private ItemContainerAdapter inventoryWeaponsListAdapter;
	private ItemContainerAdapter inventoryArmorListAdapter;
	private ItemContainerAdapter inventoryJewelryListAdapter;
	private ItemContainerAdapter inventoryPotionListAdapter;
	private ItemContainerAdapter inventoryFoodListAdapter;
	private ItemContainerAdapter inventoryQuestListAdapter;
	private ItemContainerAdapter inventoryOtherListAdapter;

	private TextView heroinfo_stats_gold;
	private TextView heroinfo_stats_attack;
	private TextView heroinfo_stats_defense;

	private ItemType lastSelectedItem; // Workaround android bug #7139

	private final ImageView[] wornItemImage = new ImageView[Inventory.WearSlot.values().length];
	private final int[] defaultWornItemImageResourceIDs = new int[Inventory.WearSlot.values().length];

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this.getActivity());
		if (!app.isInitialized()) return;
		this.world = app.getWorld();
		this.controllers = app.getControllerContext();
		this.player = world.model.player;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.heroinfo_inventory, container, false);

		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this.getActivity());
		if (!app.isInitialized()) return v;
		
		inventoryList = (ListView) v.findViewById(R.id.inventorylist_root);
		ImageView heroicon = (ImageView) v.findViewById(R.id.heroinfo_inventory_heroicon);
		heroinfo_stats_gold = (TextView) v.findViewById(R.id.heroinfo_stats_gold);
		heroinfo_stats_attack = (TextView) v.findViewById(R.id.heroinfo_stats_attack);
		heroinfo_stats_defense = (TextView) v.findViewById(R.id.heroinfo_stats_defense);

		registerForContextMenu(inventoryList);
		inventoryList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				// Move this code to separate function? -- Done
				ItemType itemType = getSelectedItemType(position);
				showInventoryItemInfo(itemType.id);
			}
		});
		inventoryList.setOnItemLongClickListener(new OnItemLongClickListener() {
			
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				showContextMenuForItem(getSelectedItemType(position));
				return true;
			}
		});

		new SpinnerEmulator(v, R.id.inventorylist_category_filters_button, R.array.inventorylist_category_filters, R.string.heroinfo_inventory_categories) {
			@Override
			public void setValue(int value) {
				world.model.uiSelections.selectedInventoryCategory = value;
			}
			@Override
			public void selectionChanged(int value) {
				reloadShownCategory(value);
			}
			@Override
			public int getValue() {
				return world.model.uiSelections.selectedInventoryCategory;
			}
		};
		new SpinnerEmulator(v, R.id.inventorylist_sort_filters_button, R.array.inventorylist_sort_filters, R.string.heroinfo_inventory_sort) {
			@Override
			public void setValue(int value) {
				world.model.uiSelections.selectedInventorySort = value;
			}
			@Override
			public void selectionChanged(int value) {
				reloadShownSort(player.inventory);
			}
			@Override
			public int getValue() {
				return world.model.uiSelections.selectedInventorySort;
			}
		};
		
		ItemContainer inv = player.inventory;
		wornTiles = world.tileManager.loadTilesFor(player.inventory, getResources());
		inventoryListAdapter = new ItemContainerAdapter(getActivity(), world.tileManager, inv, player, wornTiles);
		inventoryList.setAdapter(inventoryListAdapter);

		
		heroicon.setImageResource(HeroCollection.getHeroLargeSprite(player.iconID));

		setWearSlot(v, Inventory.WearSlot.weapon, R.id.heroinfo_worn_weapon, R.drawable.equip_weapon);
		setWearSlot(v, Inventory.WearSlot.shield, R.id.heroinfo_worn_shield, R.drawable.equip_shield);
		setWearSlot(v, Inventory.WearSlot.head, R.id.heroinfo_worn_head, R.drawable.equip_head);
		setWearSlot(v, Inventory.WearSlot.body, R.id.heroinfo_worn_body, R.drawable.equip_body);
		setWearSlot(v, Inventory.WearSlot.feet, R.id.heroinfo_worn_feet, R.drawable.equip_feet);
		setWearSlot(v, Inventory.WearSlot.neck, R.id.heroinfo_worn_neck, R.drawable.equip_neck);
		setWearSlot(v, Inventory.WearSlot.hand, R.id.heroinfo_worn_hand, R.drawable.equip_hand);
		setWearSlot(v, Inventory.WearSlot.leftring, R.id.heroinfo_worn_ringleft, R.drawable.equip_ring);
		setWearSlot(v, Inventory.WearSlot.rightring, R.id.heroinfo_worn_ringright, R.drawable.equip_ring);

		return v;
	}

	@Override
	public void onStart() {
		super.onStart();
		update();
	}

	private void setWearSlot(final View v, final Inventory.WearSlot inventorySlot, int viewId, int resourceId) {
		final ImageView imageView = (ImageView) v.findViewById(viewId);
		final RelativeLayout layout = (RelativeLayout) imageView.getParent();
		wornItemImage[inventorySlot.ordinal()] = imageView;
		defaultWornItemImageResourceIDs[inventorySlot.ordinal()] = resourceId;
		// Both the image and the layout will trigger the same click listener.  Layout needed for dpad support, and it's a larger target for touch as well.
		imageView.setOnClickListener((View view) -> {
			if (player.inventory.isEmptySlot(inventorySlot)) return;
			imageView.setClickable(false); // Will be enabled again on update()
			showEquippedItemInfo(player.inventory.getItemTypeInWearSlot(inventorySlot), inventorySlot);
		});
		layout.setOnClickListener((View view) -> imageView.performClick());
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case INTENTREQUEST_ITEMINFO:
			if (resultCode == ItemInfoActivity.RESULT_MORE_ACTIONS) {
				showContextMenuForItem( world.itemTypes.getItemType(data.getExtras().getString("itemTypeID")));
				break;
			}
			if (resultCode != Activity.RESULT_OK) break;

			ItemType itemType = world.itemTypes.getItemType(data.getExtras().getString("itemTypeID"));
			ItemInfoActivity.ItemInfoAction actionType = ItemInfoActivity.ItemInfoAction.valueOf(data.getExtras().getString("actionType"));
			if (actionType == ItemInfoActivity.ItemInfoAction.unequip) {
				Inventory.WearSlot slot = Inventory.WearSlot.valueOf(data.getExtras().getString("inventorySlot"));
				controllers.itemController.unequipSlot(itemType, slot);
			} else if (actionType == ItemInfoActivity.ItemInfoAction.equip) {
				Inventory.WearSlot slot = suggestInventorySlot(itemType);
				controllers.itemController.equipItem(itemType, slot);
			} else if (actionType == ItemInfoActivity.ItemInfoAction.use) {
				controllers.itemController.useItem(itemType);
			}
			break;
		case INTENTREQUEST_BULKSELECT_DROP:
			if (resultCode != Activity.RESULT_OK) break;

			int quantity = data.getExtras().getInt("selectedAmount");
			String itemTypeID = data.getExtras().getString("itemTypeID");
			dropItem(itemTypeID, quantity);
			break;
		}
		update();
	}

	private Inventory.WearSlot suggestInventorySlot(ItemType itemType) {
		Inventory.WearSlot slot = itemType.category.inventorySlot;
		if (player.inventory.isEmptySlot(slot)) return slot;

		if (slot == Inventory.WearSlot.leftring) return Inventory.WearSlot.rightring;
		if (itemType.isOffhandCapableWeapon()) {
			ItemType mainWeapon = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon);
			if (mainWeapon != null && mainWeapon.isTwohandWeapon()) return slot;
			else if (player.inventory.isEmptySlot(Inventory.WearSlot.shield)) return Inventory.WearSlot.shield;
		}
		return slot;
	}

	private void dropItem(String itemTypeID, int quantity) {
		ItemType itemType = world.itemTypes.getItemType(itemTypeID);
		controllers.itemController.dropItem(itemType, quantity);
	}

	private void update() {
		updateTraits();
		updateWorn();
		updateItemList();
	}

	private void updateTraits() {
		heroinfo_stats_gold.setText(getResources().getString(R.string.heroinfo_gold, player.inventory.gold));

		StringBuilder sb = new StringBuilder(10);
		ItemController.describeAttackEffect(
				player.getAttackChance(),
				player.getDamagePotential().current,
				player.getDamagePotential().max,
				player.getCriticalSkill(),
				player.getCriticalMultiplier(),
				sb);
		heroinfo_stats_attack.setText(sb.toString());

		sb = new StringBuilder(10);
		ItemController.describeBlockEffect(player.getBlockChance(), player.getDamageResistance(), sb);
		heroinfo_stats_defense.setText(sb.toString());
	}

	private void updateWorn() {
		for(Inventory.WearSlot slot : Inventory.WearSlot.values()) {
			updateWornImage(wornItemImage[slot.ordinal()], defaultWornItemImageResourceIDs[slot.ordinal()], player.inventory.getItemTypeInWearSlot(slot));
		}
	}

	private void updateWornImage(ImageView imageView, int resourceIDEmptyImage, ItemType type) {
		RelativeLayout layout = (RelativeLayout) imageView.getParent();
		if (type != null) {
			world.tileManager.setImageViewTile(getResources(), imageView, type, wornTiles);
			imageView.setClickable(true);
			layout.setFocusable(true);
		} else {
			imageView.setImageResource(resourceIDEmptyImage);
			imageView.setClickable(false);
			layout.setFocusable(false);
		}
	}

	private void updateItemList() {
		int currentScreen = world.model.uiSelections.selectedInventoryCategory;
		if (currentScreen == 0)
			inventoryListAdapter.notifyDataSetChanged();
		else
			reloadShownCategory(world.model.uiSelections.selectedInventoryCategory);
	}

//	@Override
//	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {}
//		ItemType type = getSelectedItemType((AdapterContextMenuInfo) menuInfo);
	
	
	public void showContextMenuForItem(ItemType type) {
		MenuInflater inflater = getActivity().getMenuInflater();
		Menu menu = CustomMenuInflater.newMenuInstance(getActivity());
		inflater.inflate(R.menu.inventoryitem, menu);
		if (type.isUsable()){
			menu.findItem(R.id.inv_menu_use).setVisible(true);
			menu.findItem(R.id.inv_menu_assign).setVisible(true);
		}
		if (type.isEquippable()) {
			menu.findItem(R.id.inv_menu_equip).setVisible(true);
			if (type.isOffhandCapableWeapon()) menu.findItem(R.id.inv_menu_equip_offhand).setVisible(true);
			else if (type.category.inventorySlot == Inventory.WearSlot.leftring) menu.findItem(R.id.inv_menu_equip_offhand).setVisible(true);
		}
		lastSelectedItem = null;
		CustomMenuInflater.showMenuInDialog(getActivity(), menu, world.tileManager.getDrawableForItem(getResources(), type.iconID, world.tileManager.loadTilesFor(Arrays.asList(new Integer[] { type.iconID}), getResources())), type.getName(player), type, this);
	}

	private ItemType getSelectedItemType(int position) {
		int v = world.model.uiSelections.selectedInventoryCategory;

		if (v == 0) { //All items
			return inventoryListAdapter.getItem(position).itemType;
		}else if (v == 1) { //Weapon items
			return inventoryWeaponsListAdapter.getItem(position).itemType;
		} else if (v == 2) { //Armor items
			return inventoryArmorListAdapter.getItem(position).itemType;
		} else if (v == 3) { //Jewelry items
			return inventoryJewelryListAdapter.getItem(position).itemType;
		} else if (v == 4) { //Potion items
			return inventoryPotionListAdapter.getItem(position).itemType;
		} else if (v == 5) { //Food items
			return inventoryFoodListAdapter.getItem(position).itemType;
		} else if (v == 6) { //Quest items
			return inventoryQuestListAdapter.getItem(position).itemType;
		} else if (v == 7) { //Other items
			return inventoryOtherListAdapter.getItem(position).itemType;
		}

		// Better than crashing...
		return inventoryListAdapter.getItem(position).itemType;

	}


	private ItemType getSelectedItemType(AdapterContextMenuInfo info) {
		return getSelectedItemType(info.position);
	}
	
	@Override
	public void onMenuItemSelected(MenuItem item, Object data) {
		ItemType itemType = (ItemType) data;
		int id = item.getItemId();
		if (id == R.id.inv_menu_info) {
			showInventoryItemInfo(itemType);
		} else if (id == R.id.inv_menu_drop) {
			String itemTypeID = itemType.id;
			int quantity = player.inventory.getItemQuantity(itemTypeID);
			if (quantity > 1) {
				Intent intent = Dialogs.getIntentForBulkDroppingInterface(getActivity(), itemTypeID, quantity);
				startActivityForResult(intent, INTENTREQUEST_BULKSELECT_DROP);
			} else {
				dropItem(itemTypeID, quantity);
			}
		} else if (id == R.id.inv_menu_equip) {
			controllers.itemController.equipItem(itemType, itemType.category.inventorySlot);
		} else if (id == R.id.inv_menu_equip_offhand) {
			if (itemType.category.inventorySlot == Inventory.WearSlot.weapon) {
				controllers.itemController.equipItem(itemType, Inventory.WearSlot.shield);
			} else if (itemType.category.inventorySlot == Inventory.WearSlot.leftring) {
				controllers.itemController.equipItem(itemType, Inventory.WearSlot.rightring);
			}
		} else if (id == R.id.inv_menu_use) {
			controllers.itemController.useItem(itemType);
		} else if (id == R.id.inv_menu_assign) {
			//lastSelectedItem = itemType;
		} else if (id == R.id.inv_assign_slot1) {
			controllers.itemController.setQuickItem(itemType, 0);
		} else if (id == R.id.inv_assign_slot2) {
			controllers.itemController.setQuickItem(itemType, 1);
		} else if (id == R.id.inv_assign_slot3) {
			controllers.itemController.setQuickItem(itemType, 2);
		} else if (id == R.id.inv_menu_movetop) {
			player.inventory.sortToTop(itemType.id);
		} else if (id == R.id.inv_menu_movebottom) {
			player.inventory.sortToBottom(itemType.id);
		}
		update();
	}

	private void showEquippedItemInfo(ItemType itemType, Inventory.WearSlot inventorySlot) {
		String text;
		boolean enabled = true;

		if (world.model.uiSelections.isInCombat) {
			int ap = world.model.player.getReequipCost();
			text = getResources().getString(R.string.iteminfo_action_unequip_ap, ap);
			if (ap > 0) {
				enabled = world.model.player.hasAPs(ap);
			}
		} else {
			text = getResources().getString(R.string.iteminfo_action_unequip);
		}
		Intent intent = Dialogs.getIntentForItemInfo(getActivity(), itemType.id, ItemInfoActivity.ItemInfoAction.unequip, text, enabled, inventorySlot);
		startActivityForResult(intent, INTENTREQUEST_ITEMINFO);
	}
	private void showInventoryItemInfo(String itemTypeID) {
		showInventoryItemInfo(world.itemTypes.getItemType(itemTypeID));
	}
	private void showInventoryItemInfo(ItemType itemType) {
		String text = "";
		int ap = 0;
		boolean enabled = true;
		ItemInfoActivity.ItemInfoAction action = ItemInfoActivity.ItemInfoAction.none;
		final boolean isInCombat = world.model.uiSelections.isInCombat;
		if (itemType.isEquippable()) {
			if (isInCombat) {
				ap = world.model.player.getReequipCost();
				text = getResources().getString(R.string.iteminfo_action_equip_ap, ap);
			} else {
				text = getResources().getString(R.string.iteminfo_action_equip);
			}
			action = ItemInfoActivity.ItemInfoAction.equip;
		} else if (itemType.isUsable()) {
			if (isInCombat) {
				ap = world.model.player.getUseItemCost();
				text = getResources().getString(R.string.iteminfo_action_use_ap, ap);
			} else {
				text = getResources().getString(R.string.iteminfo_action_use);
			}
			action = ItemInfoActivity.ItemInfoAction.use;
		}
		if (isInCombat && ap > 0) {
			enabled = world.model.player.hasAPs(ap);
		}

		Intent intent = Dialogs.getIntentForItemInfo(getActivity(), itemType.id, action, text, enabled, null);
		startActivityForResult(intent, INTENTREQUEST_ITEMINFO);
	}

	private void reloadShownCategory(int v) { // Apologies about the code duplication,
		// just didn't seem to make sense as an array, although I did create a nice array for skill category adapters.

		// Decide which category to show
		if (v == 0) { //All items
			inventoryList.setAdapter(inventoryListAdapter);
			inventoryListAdapter.notifyDataSetChanged();
		} else if (v == 1) { //Weapon items
			inventoryWeaponsListAdapter = new ItemContainerAdapter(getActivity(), world.tileManager, player.inventory.buildWeaponItems(), player, wornTiles);
			inventoryList.setAdapter(inventoryWeaponsListAdapter);
			inventoryWeaponsListAdapter.notifyDataSetChanged();
		} else if (v == 2) { //Armor items
			inventoryArmorListAdapter = new ItemContainerAdapter(getActivity(), world.tileManager, player.inventory.buildArmorItems(), player, wornTiles);
			inventoryList.setAdapter(inventoryArmorListAdapter);
			inventoryArmorListAdapter.notifyDataSetChanged();
		} else if (v == 3) { //Jewelry items
			inventoryJewelryListAdapter = new ItemContainerAdapter(getActivity(), world.tileManager, player.inventory.buildJewelryItems(), player, wornTiles);
			inventoryList.setAdapter(inventoryJewelryListAdapter);
			inventoryJewelryListAdapter.notifyDataSetChanged();
		} else if (v == 4) { //Potion items
			inventoryPotionListAdapter = new ItemContainerAdapter(getActivity(), world.tileManager, player.inventory.buildPotionItems(), player, wornTiles);
			inventoryList.setAdapter(inventoryPotionListAdapter);
			inventoryPotionListAdapter.notifyDataSetChanged();
		} else if (v == 5) { //Food items
			inventoryFoodListAdapter = new ItemContainerAdapter(getActivity(), world.tileManager, player.inventory.buildFoodItems(), player, wornTiles);
			inventoryList.setAdapter(inventoryFoodListAdapter);
			inventoryFoodListAdapter.notifyDataSetChanged();
		} else if (v == 6) { //Quest items
			inventoryQuestListAdapter = new ItemContainerAdapter(getActivity(), world.tileManager, player.inventory.buildQuestItems(), player, wornTiles);
			inventoryList.setAdapter(inventoryQuestListAdapter);
			inventoryQuestListAdapter.notifyDataSetChanged();
		} else if (v == 7) { //Other items
			inventoryOtherListAdapter = new ItemContainerAdapter(getActivity(), world.tileManager, player.inventory.buildOtherItems(), player, wornTiles);
			inventoryList.setAdapter(inventoryOtherListAdapter);
			inventoryOtherListAdapter.notifyDataSetChanged();
		}
		//updateItemList();
	}

	private void reloadShownSort(Inventory inv) {
		int selected = world.model.uiSelections.selectedInventorySort;

		inventoryListAdapter.reloadShownSort(selected, world.model.uiSelections.oldSortSelection, player.inventory, player);

		// Currently not functional, perhaps because selection only updates when changed.
		if (world.model.uiSelections.oldSortSelection == selected)
			world.model.uiSelections.oldSortSelection = 0;
		else world.model.uiSelections.oldSortSelection = selected;
		updateItemList();
	}

}
