package com.gpl.rpg.AndorsTrail.activity.fragment;

import android.app.Activity;
import android.content.Intent;

import com.gpl.rpg.AndorsTrail.Dialogs;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.activity.ItemInfoActivity;
import com.gpl.rpg.AndorsTrail.controller.ItemController;
import com.gpl.rpg.AndorsTrail.model.item.ItemType;

public final class ShopActivity_Buy extends ShopActivityFragment {

	@Override
	protected boolean isSellingInterface() {
		return false;
	}

	@Override
	public void onItemActionClicked(int position, ItemType itemType) {
		showBuyingInterface(itemType);
	}

	@Override
	public void onItemInfoClicked(int position, ItemType itemType) {
		int price = ItemController.getBuyingPrice(player, itemType, shopDropList);
		boolean enableButton = (price > 0 && ItemController.canAfford(player, price));
		String text = getResources().getString(R.string.shop_buyitem, price);
		Intent intent = Dialogs.getIntentForItemInfo(getActivity(), itemType.id, ItemInfoActivity.ItemInfoAction.buy, text, enableButton, null);
		startActivityForResult(intent, INTENTREQUEST_ITEMINFO);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != Activity.RESULT_OK) return;

		ItemType itemType = world.itemTypes.getItemType(data.getExtras().getString("itemTypeID"));
		switch (requestCode) {
		case INTENTREQUEST_ITEMINFO:
			showBuyingInterface(itemType);
			break;
		case INTENTREQUEST_BULKSELECT:
			int quantity = data.getExtras().getInt("selectedAmount");
			buy(itemType, quantity);
			break;
		}
		update();
	}

	private void showBuyingInterface(ItemType itemType) {
		int price = ItemController.getBuyingPrice(player, itemType, shopDropList);
		Intent intent = Dialogs.getIntentForBulkBuyingInterface(getActivity(), itemType.id, shopInventory.getItemQuantity(itemType.id), price);
		startActivityForResult(intent, INTENTREQUEST_BULKSELECT);
	}

	private void buy(ItemType itemType, int quantity) {
		if (!ItemController.buy(world.model, player, itemType, shopInventory, shopDropList, quantity)) return;
		final String msg = getResources().getString(R.string.shop_item_bought, itemType.getName(player));
		displayStoreAction(msg);
	}
}
