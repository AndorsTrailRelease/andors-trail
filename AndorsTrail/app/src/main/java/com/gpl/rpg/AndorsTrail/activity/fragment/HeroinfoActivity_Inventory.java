package com.gpl.rpg.AndorsTrail.activity.fragment;

import java.util.Arrays;
import java.util.ArrayList;

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
import android.widget.LinearLayout;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;
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
import com.gpl.rpg.AndorsTrail.util.ThemeHelper;
import com.gpl.rpg.AndorsTrail.view.CustomMenuInflater;
import com.gpl.rpg.AndorsTrail.view.CustomDialogFactory;
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

		View presetsButton = v.findViewById(R.id.equipment_presets_button);
		presetsButton.setOnClickListener(view -> showEquipmentPresets());

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
		java.util.List<Integer> brokenPresets = ItemController.getEquipmentPresetsBrokenByRemoving(player, itemTypeID, quantity);
		if (brokenPresets.isEmpty()) {
			controllers.itemController.dropItem(itemType, quantity);
			return;
		}
		String presetNames = ItemController.formatPresetNumbers(brokenPresets);
		CustomDialogFactory.CustomDialog warning = CustomDialogFactory.createDialog(getActivity(), getString(R.string.equipment_preset_removal_title), null, getString(R.string.equipment_preset_removal_warning, itemType.getName(player), presetNames, getString(R.string.inventory_drop).toLowerCase()), null, true);
		CustomDialogFactory.addButton(warning, android.R.string.yes, view -> {
			controllers.itemController.dropItem(itemType, quantity);
			update();
		});
		CustomDialogFactory.addDismissButton(warning, android.R.string.no);
		CustomDialogFactory.show(warning);
	}

	private void showEquipmentPresets() {
		if (world.model.uiSelections.isInCombat) {
			CustomDialogFactory.CustomDialog error = CustomDialogFactory.createErrorDialog(getActivity(), getString(R.string.equipment_presets), getString(R.string.equipment_preset_not_available_combat));
			CustomDialogFactory.show(error);
			return;
		}
		LinearLayout content = new LinearLayout(getActivity());
		content.setOrientation(LinearLayout.VERTICAL);
		ScrollView scroll = new ScrollView(getActivity());
		scroll.addView(content);
		final CustomDialogFactory.CustomDialog dialog = CustomDialogFactory.createDialog(getActivity(), getString(R.string.equipment_presets), null, null, scroll, true);
		for (int preset = 0; preset < Inventory.NUM_EQUIPMENT_PRESETS; ++preset) {
			final int presetIndex = preset;
			LinearLayout block = new LinearLayout(getActivity());
			block.setOrientation(LinearLayout.VERTICAL);
			block.setPadding(0, 8, 0, 8);
			TextView status = new TextView(getActivity());
			status.setText(isEquipmentPresetEmpty(preset) ? getString(R.string.equipment_preset_empty) : getString(R.string.equipment_preset_saved, preset + 1));
			block.addView(status);
			View preview = createPresetPreview(preset);
			preview.setOnClickListener(view -> showLoadEquipmentPresetConfirmation(presetIndex, dialog));
			preview.setOnLongClickListener(view -> {
				saveEquipmentPreset(presetIndex, dialog);
				return true;
			});
			preview.setLongClickable(true);
			block.addView(preview);
			content.addView(block);
		}
		CustomDialogFactory.addDismissButton(dialog, R.string.dialog_close);
		CustomDialogFactory.show(dialog);
	}

	private boolean isEquipmentPresetEmpty(int preset) {
		return !player.inventory.isEquipmentPresetSaved(preset);
	}

	private void saveEquipmentPreset(final int preset, final CustomDialogFactory.CustomDialog parentDialog) {
		boolean isOverwrite = !isEquipmentPresetEmpty(preset);
		String message = getString(R.string.equipment_preset_save_message, preset + 1);
		if (isOverwrite) message += "\n\n" + getString(R.string.equipment_preset_overwrite_message);
		String title = getString(isOverwrite ? R.string.equipment_preset_overwrite_title : R.string.equipment_preset_save_title);
		CustomDialogFactory.CustomDialog confirmation = CustomDialogFactory.createDialog(getActivity(), title, null, message, null, true);
		CustomDialogFactory.addButton(confirmation, android.R.string.yes, view -> {
			controllers.itemController.saveEquipmentPreset(preset);
			parentDialog.dismiss();
			Toast.makeText(getActivity(), getString(R.string.equipment_preset_saved_toast, preset + 1), Toast.LENGTH_SHORT).show();
		});
		CustomDialogFactory.addDismissButton(confirmation, android.R.string.no);
		CustomDialogFactory.show(confirmation);
	}

	private void showLoadEquipmentPresetConfirmation(final int preset, final CustomDialogFactory.CustomDialog parentDialog) {
		CustomDialogFactory.CustomDialog confirmation = CustomDialogFactory.createDialog(getActivity(), getString(R.string.equipment_preset_load_title), null, getString(R.string.equipment_preset_load_message, preset + 1), null, true);
		CustomDialogFactory.addButton(confirmation, android.R.string.yes, view -> { parentDialog.dismiss(); loadEquipmentPreset(preset); });
		CustomDialogFactory.addDismissButton(confirmation, android.R.string.no);
		CustomDialogFactory.show(confirmation);
	}

	private View createPresetPreview(int preset) {
		LinearLayout preview = new LinearLayout(getActivity());
		preview.setOrientation(LinearLayout.HORIZONTAL);
		int previewPadding = (int) (4 * getResources().getDisplayMetrics().density);
		preview.setPadding(previewPadding, previewPadding, previewPadding, previewPadding);
		preview.setBackgroundResource(ThemeHelper.getThemeResource(getActivity(), R.attr.ui_theme_textbutton_drawable));
		ArrayList<Integer> iconIDs = new ArrayList<Integer>();
		for (Inventory.WearSlot slot : Inventory.WearSlot.values()) {
			String id = player.inventory.getEquipmentPresetItemTypeID(preset, slot);
			if (id != null) { ItemType type = world.itemTypes.getItemType(id); if (type != null) iconIDs.add(type.iconID); }
		}
		TileCollection tiles = iconIDs.isEmpty() ? null : world.tileManager.loadTilesFor(iconIDs, getResources());
		for (Inventory.WearSlot slot : Inventory.WearSlot.values()) {
			String id = player.inventory.getEquipmentPresetItemTypeID(preset, slot);
			ItemType type = id == null ? null : world.itemTypes.getItemType(id);
			ImageView image = new ImageView(getActivity());
			int size = (int) (32 * getResources().getDisplayMetrics().density);
			image.setLayoutParams(new LinearLayout.LayoutParams(size, size));
			if (type != null) {
				world.tileManager.setImageViewTile(getResources(), image, type, tiles);
				image.setContentDescription(type.getName(player));
			} else {
				image.setImageResource(defaultWornItemImageResourceIDs[slot.ordinal()]);
			}
			preview.addView(image);
		}
		return preview;
	}

	private View createMissingItemsPreview(java.util.List<String> missing) {
		LinearLayout preview = new LinearLayout(getActivity());
		preview.setOrientation(LinearLayout.VERTICAL);
		ArrayList<Integer> iconIDs = new ArrayList<Integer>();
		for (String id : missing) {
			ItemType type = world.itemTypes.getItemType(id);
			if (type != null) iconIDs.add(type.iconID);
		}
		TileCollection tiles = world.tileManager.loadTilesFor(iconIDs, getResources());
		for (String id : missing) {
			ItemType type = world.itemTypes.getItemType(id);
			if (type == null) continue;
			LinearLayout row = new LinearLayout(getActivity());
			row.setOrientation(LinearLayout.HORIZONTAL);
			ImageView image = new ImageView(getActivity());
			int size = (int) (32 * getResources().getDisplayMetrics().density);
			image.setLayoutParams(new LinearLayout.LayoutParams(size, size));
			world.tileManager.setImageViewTile(getResources(), image, type, tiles);
			TextView name = new TextView(getActivity());
			name.setText(type.getName(player));
			row.addView(image);
			row.addView(name);
			preview.addView(row);
		}
		return preview;
	}

	private void loadEquipmentPreset(final int preset) {
		if (!player.inventory.isEquipmentPresetSaved(preset)) {
			Toast.makeText(getActivity(), R.string.equipment_preset_empty_toast, Toast.LENGTH_SHORT).show();
			return;
		}
		java.util.List<String> missing = controllers.itemController.getMissingEquipmentPresetItems(preset);
		java.util.List<String> conflicts = controllers.itemController.getEquipmentPresetConflicts(preset);
		if (missing.isEmpty() && conflicts.isEmpty()) {
			controllers.itemController.applyEquipmentPreset(preset);
			update();
			Toast.makeText(getActivity(), getString(R.string.equipment_preset_loaded_toast, preset + 1), Toast.LENGTH_SHORT).show();
			return;
		}
		StringBuilder message = new StringBuilder();
		if (!missing.isEmpty()) message.append(getString(R.string.equipment_preset_missing_message, joinItemNames(missing)));
		if (!conflicts.isEmpty()) {
			if (message.length() > 0) message.append("\n\n");
			message.append(getString(R.string.equipment_preset_conflict_message, joinItemNames(conflicts)));
		}
		message.append("\n\n").append(getString(R.string.equipment_preset_load_confirm));
		CustomDialogFactory.CustomDialog confirmation = CustomDialogFactory.createDialog(getActivity(), getString(R.string.equipment_preset_removal_title), null, message.toString(), null, true);
		CustomDialogFactory.setContent(confirmation, createMissingItemsPreview(missing));
		CustomDialogFactory.addButton(confirmation, android.R.string.yes, view -> { controllers.itemController.applyEquipmentPreset(preset); update(); Toast.makeText(getActivity(), getString(R.string.equipment_preset_loaded_toast, preset + 1), Toast.LENGTH_SHORT).show(); });
		CustomDialogFactory.addDismissButton(confirmation, android.R.string.no);
		CustomDialogFactory.show(confirmation);
	}

	private String joinItemNames(java.util.List<String> itemTypeIDs) {
		StringBuilder names = new StringBuilder();
		for (String id : itemTypeIDs) {
			ItemType type = world.itemTypes.getItemType(id);
			if (type == null) continue;
			if (names.length() > 0) names.append("\n");
			names.append(type.getName(player));
		}
		return names.toString();
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
