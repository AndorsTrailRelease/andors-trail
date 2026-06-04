package com.gpl.rpg.AndorsTrail.view;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.controller.ItemController;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.model.item.ItemContainer;
import com.gpl.rpg.AndorsTrail.model.item.ItemContainer.ItemEntry;
import com.gpl.rpg.AndorsTrail.model.item.ItemType;
import com.gpl.rpg.AndorsTrail.resource.tiles.TileCollection;
import com.gpl.rpg.AndorsTrail.resource.tiles.TileManager;

public final class ShopItemContainerAdapter extends ArrayAdapter<ItemEntry> {
	private final TileManager tileManager;
	private final TileCollection tileCollection;
	private final OnContainerItemClickedListener clickListener;
	private final boolean isSelling;
	private final Resources r;
	private final Player player;

	public ShopItemContainerAdapter(Context context, TileCollection tileCollection, TileManager tileManager, Player player, ItemContainer items, OnContainerItemClickedListener clickListener, boolean isSelling) {
		super(context, 0, items.items);
		this.tileManager = tileManager;
		this.tileCollection = tileCollection;
		this.player = player;
		this.clickListener = clickListener;
		this.isSelling = isSelling;
		this.r = context.getResources();
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final ItemEntry item = getItem(position);
		final ItemType itemType = item.itemType;

		View result = convertView;
		if (result == null) {
			result = View.inflate(getContext(), R.layout.shopitemview, null);
		}

		// Set the icon
		tileManager.setImageViewTile(r, (ImageView) result.findViewById(R.id.shopitem_image), itemType, tileCollection);
		TextView item_text = result.findViewById(R.id.shopitem_text);
		Button shop_button = result.findViewById(R.id.shopitem_shopbutton);

		item_text.setText(ItemController.describeItemForListView(item, player));

		// Set the button text and enabled state and conditional text
		boolean enabled = true;
		if (isSelling) {
			enabled = ItemController.maySellItem(player, itemType);
			shop_button.setText(r.getString(R.string.shop_sellitem, ItemController.getSellingPrice(player, itemType)));
		} else {
			int price = ItemController.getBuyingPrice(player, itemType);
			enabled = price > 0 && ItemController.canAfford(player, price);
			shop_button.setText(r.getString(R.string.shop_buyitem, price));
		}
		shop_button.setEnabled(enabled);
		item_text.setEnabled(enabled);

		// Set up listeners
		shop_button.setOnClickListener(view -> clickListener.onItemActionClicked(position, itemType));

		result.findViewById(R.id.shopitem_infobutton).setOnClickListener(view -> clickListener.onItemInfoClicked(position, itemType));

		// Handlers for the row itself, so that it isn't necessary to hit the button exactly.
		result.setOnClickListener(view -> clickListener.onItemInfoClicked(position, itemType));
		if(enabled) result.setOnLongClickListener(view -> { clickListener.onItemActionClicked(position, itemType); return true; });

		return result;
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).itemType.id.hashCode();
	}

	public static interface OnContainerItemClickedListener {
		void onItemActionClicked(int position, ItemType itemType);
		void onItemInfoClicked(int position, ItemType itemType);
	}

	public void reloadShownSort(int selection, ItemContainer container, Player p){
		ItemContainerAdapter.reloadShownSort(selection, container, player);
	}
}
