package com.gpl.rpg.AndorsTrail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.gpl.rpg.AndorsTrail.activity.ActorConditionInfoActivity;
import com.gpl.rpg.AndorsTrail.activity.BulkSelectionInterface;
import com.gpl.rpg.AndorsTrail.activity.ConversationActivity;
import com.gpl.rpg.AndorsTrail.activity.ItemInfoActivity;
import com.gpl.rpg.AndorsTrail.activity.LevelUpActivity;
import com.gpl.rpg.AndorsTrail.activity.LoadSaveActivity;
import com.gpl.rpg.AndorsTrail.activity.MainActivity;
import com.gpl.rpg.AndorsTrail.activity.MonsterEncounterActivity;
import com.gpl.rpg.AndorsTrail.activity.MonsterInfoActivity;
import com.gpl.rpg.AndorsTrail.activity.SkillInfoActivity;
import com.gpl.rpg.AndorsTrail.activity.fragment.StartScreenActivity_MainMenu;
import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.model.ability.ActorConditionType;
import com.gpl.rpg.AndorsTrail.model.ability.SkillCollection;
import com.gpl.rpg.AndorsTrail.model.actor.Monster;
import com.gpl.rpg.AndorsTrail.model.item.Inventory;
import com.gpl.rpg.AndorsTrail.model.item.ItemType;
import com.gpl.rpg.AndorsTrail.model.item.Loot;
import com.gpl.rpg.AndorsTrail.model.map.MapObject;
import com.gpl.rpg.AndorsTrail.util.ThemeHelper;
import com.gpl.rpg.AndorsTrail.view.CustomDialogFactory;
import com.gpl.rpg.AndorsTrail.view.CustomDialogFactory.CustomDialog;
import com.gpl.rpg.AndorsTrail.view.ItemContainerAdapter;

public final class Dialogs {

	private static void showDialogAndPause(CustomDialog d, final ControllerContext context) {
		showDialogAndPause(d, context, null);
	}
	private static void showDialogAndPause(CustomDialog d, final ControllerContext context, final OnDismissListener onDismiss) {
		context.gameRoundController.pause();
		CustomDialogFactory.setDismissListener(d, new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface arg0) {
				if (onDismiss != null) onDismiss.onDismiss(arg0);
				context.gameRoundController.resume();
			}
		});
		CustomDialogFactory.show(d);
	}

	public static void showMapScriptMessage(final MainActivity currentActivity, final ControllerContext context, String phraseID) {
		showConversation(currentActivity, context, phraseID, null, false);
	}

	public static void showConversation(final MainActivity currentActivity, final ControllerContext context, final String phraseID, final Monster npc) {
		showConversation(currentActivity, context, phraseID, npc, true);
	}

	private static void showConversation(final MainActivity currentActivity, final ControllerContext context, final String phraseID, final Monster npc, boolean applyScriptEffectsForFirstPhrase) {
		context.gameRoundController.pause();
		Intent intent = new Intent(currentActivity, ConversationActivity.class);
		intent.setData(Uri.parse("content://com.gpl.rpg.AndorsTrail/conversation/" + phraseID));
		intent.putExtra("applyScriptEffectsForFirstPhrase", applyScriptEffectsForFirstPhrase);
		addMonsterIdentifiers(intent, npc);
		currentActivity.startActivityForResult(intent, MainActivity.INTENTREQUEST_CONVERSATION);
	}

	public static void addMonsterIdentifiers(Intent intent, Monster monster) {
		if (monster == null) return;
		intent.putExtra("x", monster.position.x);
		intent.putExtra("y", monster.position.y);
	}
	public static void addMonsterIdentifiers(Bundle bundle, Monster monster) {
		if (monster == null) return;
		bundle.putInt("x", monster.position.x);
		bundle.putInt("y", monster.position.y);
	}

	public static Monster getMonsterFromIntent(Intent intent, final WorldContext world) {
		return getMonsterFromBundle(intent.getExtras(), world);
	}
	public static Monster getMonsterFromBundle(Bundle params, final WorldContext world) {
		if (params == null) return null;
		if (!params.containsKey("x")) return null;
		int x = params.getInt("x");
		int y = params.getInt("y");
		return world.model.currentMaps.map.getMonsterAt(x, y);
	}

	public static void showMonsterEncounter(final MainActivity currentActivity, final ControllerContext context, final Monster monster) {
		context.gameRoundController.pause();
		Intent intent = new Intent(currentActivity, MonsterEncounterActivity.class);
		intent.setData(Uri.parse("content://com.gpl.rpg.AndorsTrail/monsterencounter"));
		addMonsterIdentifiers(intent, monster);
		currentActivity.startActivityForResult(intent, MainActivity.INTENTREQUEST_MONSTERENCOUNTER);
	}

	public static void showMonsterInfo(final Context context, final Monster monster) {
		Intent intent = new Intent(context, MonsterInfoActivity.class);
		intent.setData(Uri.parse("content://com.gpl.rpg.AndorsTrail/monsterinfo"));
		addMonsterIdentifiers(intent, monster);
		context.startActivity(intent);
	}

	public static String getGroundLootFoundMessage(final Context ctx, final Loot loot) {
		StringBuilder sb = new StringBuilder(60);
		if (!loot.items.isEmpty()) {
			sb.append(ctx.getString(R.string.dialog_groundloot_message));
		}
		appendGoldPickedUpMessage(ctx, loot, sb);
		return sb.toString();
	}
	public static String getGroundLootPickedUpMessage(final Context ctx, final Loot loot) {
		StringBuilder sb = new StringBuilder(60);
		appendLootPickedUpMessage(ctx, loot, sb);
		return sb.toString();
	}
	public static String getMonsterLootFoundMessage(final Context ctx, final Loot combinedLoot, final int exp) {
		StringBuilder sb = new StringBuilder(60);
		appendMonsterEncounterSurvivedMessage(ctx, sb, exp);
		appendGoldPickedUpMessage(ctx, combinedLoot, sb);
		return sb.toString();
	}
	public static String getMonsterLootPickedUpMessage(final Context ctx, final Loot combinedLoot, final int exp) {
		StringBuilder sb = new StringBuilder(60);
		appendMonsterEncounterSurvivedMessage(ctx, sb, exp);
		appendLootPickedUpMessage(ctx, combinedLoot, sb);
		return sb.toString();
	}
	private static void appendMonsterEncounterSurvivedMessage(final Context ctx, final StringBuilder sb, final int exp) {
		sb.append(ctx.getString(R.string.dialog_monsterloot_message));
		if (exp > 0) {
			sb.append(' ');
			sb.append(ctx.getString(R.string.dialog_monsterloot_gainedexp, exp));
		}
	}
	private static void appendGoldPickedUpMessage(final Context ctx, final Loot loot, final StringBuilder sb) {
		if (loot.gold > 0) {
			sb.append(' ');
			sb.append(ctx.getString(R.string.dialog_loot_foundgold, loot.gold));
		}
	}
	private static void appendLootPickedUpMessage(final Context ctx, final Loot loot, final StringBuilder sb) {
		appendGoldPickedUpMessage(ctx, loot, sb);
		int numItems = loot.items.countItems();
		if (numItems == 1) {
			sb.append(' ');
			sb.append(ctx.getString(R.string.dialog_loot_pickedupitem));
		} else if (numItems > 1){
			sb.append(' ');
			sb.append(ctx.getString(R.string.dialog_loot_pickedupitems, numItems));
		}
	}

	public static void showMonsterLoot(final MainActivity mainActivity, final ControllerContext controllers, final WorldContext world, final Collection<Loot> lootBags, final Loot combinedLoot, final String msg) {
		// CombatController will do killedMonsterBags.clear() after this method has been called,
		// so we need to keep the list of objects. Therefore, we create a shallow copy of the list of bags.
		ArrayList<Loot> bags = new ArrayList<Loot>(lootBags);
		showLoot(mainActivity, controllers, world, combinedLoot, bags, R.string.dialog_monsterloot_title, msg);
	}

	public static void showGroundLoot(final MainActivity mainActivity, final ControllerContext controllers, final WorldContext world, final Loot loot, final String msg) {
		showLoot(mainActivity, controllers, world, loot, Collections.singletonList(loot), R.string.dialog_groundloot_title, msg);
	}

	private static void showLoot(final MainActivity mainActivity, final ControllerContext controllers, final WorldContext world, final Loot combinedLoot, final Iterable<Loot> lootBags, final int title, final String msg) {
		final ListView itemList = new ListView(mainActivity);
		itemList.setBackgroundResource(android.R.color.transparent);
		itemList.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT));
		//		itemList.setPadding(20, 0, 20, 20);
		itemList.setAdapter(new ItemContainerAdapter(mainActivity, world.tileManager, combinedLoot.items, world.model.player));

		final CustomDialog d = CustomDialogFactory.createDialog(mainActivity,
				mainActivity.getResources().getString(title), 
				mainActivity.getResources().getDrawable(R.drawable.ui_icon_equipment), 
				msg, 
				combinedLoot.items.isEmpty() ? null : itemList, 
				true,
				false);

		itemList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

				final String itemTypeID = ((ItemContainerAdapter) parent.getAdapter()).getItem(position).itemType.id;
				boolean removeFromCombinedLoot = true;
				for (Loot l : lootBags) {
					if (l == combinedLoot) removeFromCombinedLoot = false;
					if (l.items.removeItem(itemTypeID)) {
						controllers.itemController.removeLootBagIfEmpty(l);
						break;
					}
				}
				if (removeFromCombinedLoot) {
					combinedLoot.items.removeItem(itemTypeID);
				}
				if (((ItemContainerAdapter) parent.getAdapter()).isEmpty()) {
					ViewGroup vg = (ViewGroup) d.findViewById(R.id.dialog_content_container);
					vg.setVisibility(View.GONE);
				}
				ItemType type = world.itemTypes.getItemType(itemTypeID);
				world.model.player.inventory.addItem(type);
				((ItemContainerAdapter) itemList.getAdapter()).notifyDataSetChanged();
			}
		});

		if (!itemList.getAdapter().isEmpty()) {
			CustomDialogFactory.addButton(d, R.string.dialog_loot_pickall, new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					controllers.itemController.pickupAll(lootBags);
				}
			});
		}

		CustomDialogFactory.addDismissButton(d, R.string.dialog_close);

		showDialogAndPause(d, controllers, new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface arg0) {
				controllers.itemController.removeLootBagIfEmpty(lootBags);
			}
		});
	}

	public static void showHeroDied(final MainActivity mainActivity, final ControllerContext controllers) {
		final CustomDialog d = CustomDialogFactory.createDialog(mainActivity,
				mainActivity.getResources().getString(R.string.dialog_game_over_title),
				mainActivity.getResources().getDrawable(R.drawable.ui_icon_combat),
				mainActivity.getResources().getString(R.string.dialog_game_over_text),
				null,
				true,
				false);

		CustomDialogFactory.addDismissButton(d, android.R.string.ok);

		showDialogAndPause(d, controllers, new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface arg0) {
				mainActivity.finish();
			}
		});
	}


	public static Intent getIntentForItemInfo(final Context ctx, String itemTypeID, ItemInfoActivity.ItemInfoAction actionType, String buttonText, boolean buttonEnabled, Inventory.WearSlot inventorySlot) {
		Intent intent = new Intent(ctx, ItemInfoActivity.class);
		intent.putExtra("buttonText", buttonText);
		intent.putExtra("buttonEnabled", buttonEnabled);
		intent.putExtra("moreActions", (actionType == ItemInfoActivity.ItemInfoAction.equip || actionType == ItemInfoActivity.ItemInfoAction.use || actionType == ItemInfoActivity.ItemInfoAction.none));
		intent.putExtra("itemTypeID", itemTypeID);
		intent.putExtra("actionType", actionType.name());
		if (inventorySlot != null) intent.putExtra("inventorySlot", inventorySlot.name());
		intent.setData(Uri.parse("content://com.gpl.rpg.AndorsTrail/iteminfo/" + itemTypeID));
		return intent;
	}
	public static Intent getIntentForLevelUp(final Context ctx) {
		Intent intent = new Intent(ctx, LevelUpActivity.class);
		intent.setData(Uri.parse("content://com.gpl.rpg.AndorsTrail/levelup"));
		return intent;
	}

	public static void showConfirmRest(final Activity currentActivity, final ControllerContext controllerContext, final MapObject area) {
		final CustomDialog d = CustomDialogFactory.createDialog(currentActivity,
				currentActivity.getResources().getString(R.string.dialog_rest_title), 
				null, 
				currentActivity.getResources().getString(R.string.dialog_rest_confirm_message), 
				null, 
				true);

		CustomDialogFactory.addButton(d, android.R.string.yes, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				controllerContext.mapController.rest(area);
			}
		});

		CustomDialogFactory.addDismissButton(d, android.R.string.no);

		showDialogAndPause(d, controllerContext);
	}
	public static void showRested(final Activity currentActivity, final ControllerContext controllerContext) {
		//		Dialog d = new AlertDialog.Builder(new ContextThemeWrapper(currentActivity, R.style.AndorsTrailStyle))
		//		.setTitle(R.string.dialog_rest_title)
		//		.setMessage(R.string.dialog_rest_message)
		//		.setNeutralButton(android.R.string.ok, null)
		//		.create();
		final CustomDialog d = CustomDialogFactory.createDialog(currentActivity,
				currentActivity.getResources().getString(R.string.dialog_rest_title), 
				null, 
				currentActivity.getResources().getString(R.string.dialog_rest_message), 
				null, 
				true);


		CustomDialogFactory.addDismissButton(d, android.R.string.ok);

		showDialogAndPause(d, controllerContext);
	}

	public static void showNewVersion(final Activity currentActivity, final OnDismissListener onDismiss) {
		//		new AlertDialog.Builder(new ContextThemeWrapper(currentActivity, R.style.AndorsTrailStyle))
		//		.setTitle(R.string.dialog_newversion_title)
		//		.setMessage(R.string.dialog_newversion_message)
		//		.setNeutralButton(android.R.string.ok, null)
		//		.show();

		String text = currentActivity.getResources().getString(R.string.dialog_newversion_message);
		
		if (!hasPermissions(currentActivity) && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			text += currentActivity.getResources().getString(R.string.dialog_newversion_permission_information);
		}

		final CustomDialog d = CustomDialogFactory.createDialog(currentActivity,
				currentActivity.getResources().getString(R.string.dialog_newversion_title), 
				null, 
				text,
				null, 
				true);

		CustomDialogFactory.addDismissButton(d, android.R.string.ok);
		CustomDialogFactory.setDismissListener(d, new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface arg0) {
				if (onDismiss != null) onDismiss.onDismiss(arg0);
			}
		});
		CustomDialogFactory.show(d);
	}

	private static boolean hasPermissions(final Activity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (activity.getApplicationContext().checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
					|| activity.getApplicationContext().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				return false;
			}
		}
		return true;
	}

	public static boolean showSave(final Activity mainActivity, final ControllerContext controllerContext, final WorldContext world) {
		if (world.model.uiSelections.isInCombat) {
			Toast.makeText(mainActivity, R.string.menu_save_saving_not_allowed_in_combat, Toast.LENGTH_SHORT).show();
			return false;
		}

		if (!world.model.statistics.hasUnlimitedSaves()) {
			final CustomDialog d = CustomDialogFactory.createDialog(mainActivity,
					mainActivity.getResources().getString(R.string.menu_save_switch_character_title),
					null,
					mainActivity.getResources().getString(R.string.menu_save_switch_character),
					null,
					true);
			CustomDialogFactory.addButton(d, android.R.string.ok, new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					controllerContext.gameRoundController.pause();
					Intent intent = new Intent(mainActivity, LoadSaveActivity.class);
					intent.setData(Uri.parse("content://com.gpl.rpg.AndorsTrail/save"));
					mainActivity.startActivityForResult(intent, MainActivity.INTENTREQUEST_SAVEGAME);
				}
			});
			CustomDialogFactory.addDismissButton(d, android.R.string.cancel);
			CustomDialogFactory.show(d);
			return false;
		} else {
			controllerContext.gameRoundController.pause();
			Intent intent = new Intent(mainActivity, LoadSaveActivity.class);
			intent.setData(Uri.parse("content://com.gpl.rpg.AndorsTrail/save"));
			mainActivity.startActivityForResult(intent, MainActivity.INTENTREQUEST_SAVEGAME);
			return true;
		}
	}

	public static void showLoad(final Activity currentActivity) {
		Intent intent = new Intent(currentActivity, LoadSaveActivity.class);
		intent.setData(Uri.parse("content://com.gpl.rpg.AndorsTrail/load"));
		currentActivity.startActivityForResult(intent, StartScreenActivity_MainMenu.INTENTREQUEST_LOADGAME);
	}

	public static void showLoad(final Fragment currentFragment) {
		Intent intent = new Intent(currentFragment.getActivity(), LoadSaveActivity.class);
		intent.setData(Uri.parse("content://com.gpl.rpg.AndorsTrail/load"));
		currentFragment.startActivityForResult(intent, StartScreenActivity_MainMenu.INTENTREQUEST_LOADGAME);
	}

	public static void showActorConditionInfo(final Context context, ActorConditionType conditionType) {
		Intent intent = new Intent(context, ActorConditionInfoActivity.class);
		intent.setData(Uri.parse("content://com.gpl.rpg.AndorsTrail/actorconditioninfo/" + conditionType.conditionTypeID));
		context.startActivity(intent);
	}

	public static Intent getIntentForBulkBuyingInterface(final Context ctx, String itemTypeID, int totalAvailableAmount) {
		return getIntentForBulkSelectionInterface(ctx, itemTypeID, totalAvailableAmount, BulkSelectionInterface.BulkInterfaceType.buy);
	}

	public static Intent getIntentForBulkSellingInterface(final Context ctx, String itemTypeID, int totalAvailableAmount) {
		return getIntentForBulkSelectionInterface(ctx, itemTypeID, totalAvailableAmount, BulkSelectionInterface.BulkInterfaceType.sell);
	}

	public static Intent getIntentForBulkDroppingInterface(final Context ctx, String itemTypeID, int totalAvailableAmount) {
		return getIntentForBulkSelectionInterface(ctx, itemTypeID, totalAvailableAmount, BulkSelectionInterface.BulkInterfaceType.drop);
	}

	private static Intent getIntentForBulkSelectionInterface(final Context ctx, String itemTypeID, int totalAvailableAmount, BulkSelectionInterface.BulkInterfaceType interfaceType) {
		Intent intent = new Intent(ctx, BulkSelectionInterface.class);
		intent.putExtra("itemTypeID", itemTypeID);
		intent.putExtra("totalAvailableAmount", totalAvailableAmount);
		intent.putExtra("interfaceType", interfaceType.name());
		intent.setData(Uri.parse("content://com.gpl.rpg.AndorsTrail/bulkselection/" + itemTypeID));
		return intent;
	}
	public static Intent getIntentForSkillInfo(final Context ctx, SkillCollection.SkillID skillID) {
		Intent intent = new Intent(ctx, SkillInfoActivity.class);
		intent.putExtra("skillID", skillID.name());
		intent.setData(Uri.parse("content://com.gpl.rpg.AndorsTrail/showskillinfo/" + skillID));
		return intent;
	}

	public static void showCombatLog(final Context context, final ControllerContext controllerContext, final WorldContext world) {
		String[] combatLogMessages = world.model.combatLog.getAllMessages();

		View view = null;
		ListView itemList = null;
		itemList = new ListView(context);
		itemList.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT));
		itemList.setStackFromBottom(true);
		itemList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		itemList.setChoiceMode(ListView.CHOICE_MODE_NONE);
		itemList.setBackgroundColor(ThemeHelper.getThemeColor(context, R.attr.ui_theme_stdframe_bg_color));
		if (combatLogMessages.length <= 0) {
			combatLogMessages = new String[] {context.getResources().getString(R.string.combat_log_noentries)};
		}
		itemList.setAdapter(new ArrayAdapter<String>(context, R.layout.combatlog_row, android.R.id.text1, combatLogMessages));
		view = itemList;

		final CustomDialog d = CustomDialogFactory.createDialog(context,
				context.getResources().getString(R.string.combat_log_title), 
				context.getResources().getDrawable(R.drawable.ui_icon_combat), 
				null, 
				view, 
				true);


		CustomDialogFactory.addDismissButton(d, R.string.dialog_close);

		showDialogAndPause(d, controllerContext);
	}
}
