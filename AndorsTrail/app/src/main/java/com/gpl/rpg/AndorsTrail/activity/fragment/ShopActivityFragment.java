package com.gpl.rpg.AndorsTrail.activity.fragment;

import java.util.HashSet;

import android.content.res.Resources;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.Dialogs;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.model.actor.Monster;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.model.item.ItemContainer;
import com.gpl.rpg.AndorsTrail.resource.tiles.TileCollection;
import com.gpl.rpg.AndorsTrail.view.ShopItemContainerAdapter;
import com.gpl.rpg.AndorsTrail.view.SpinnerEmulator;
import com.gpl.rpg.AndorsTrail.view.ShopItemContainerAdapter.OnContainerItemClickedListener;

public abstract class ShopActivityFragment extends Fragment implements OnContainerItemClickedListener {

	protected static final int INTENTREQUEST_ITEMINFO = 3;
	protected static final int INTENTREQUEST_BULKSELECT = 9;

	protected WorldContext world;
	protected Player player;

	protected ItemContainer shopInventory;
	private TextView shop_gc;
	private ShopItemContainerAdapter listAdapter;
	private Button shoplist_sort;

	protected abstract boolean isSellingInterface();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(getActivity());
		if (!app.isInitialized()) return;
		this.world = app.getWorld();
		this.player = world.model.player;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.shoplist, container, false);

		final Monster npc = Dialogs.getMonsterFromIntent(getActivity().getIntent(), world);

		final Resources res = getResources();

		shop_gc = (TextView) v.findViewById(R.id.shop_gc);

		ListView shoplist = (ListView) v.findViewById(R.id.shop_list);
		shoplist.setItemsCanFocus(true);

		shopInventory = npc.getShopItems(player);

		HashSet<Integer> iconIDs = world.tileManager.getTileIDsFor(shopInventory);
		iconIDs.addAll(world.tileManager.getTileIDsFor(player.inventory));
		TileCollection tiles = world.tileManager.loadTilesFor(iconIDs, res);
		final boolean isSelling = isSellingInterface();
		listAdapter = new ShopItemContainerAdapter(getActivity(), tiles, world.tileManager, player, isSelling ? player.inventory : shopInventory, this, isSelling);
		shoplist.setAdapter(listAdapter);
		setupSelectionFocusBehavior(shoplist);

		//Initiating drop-down list for category filters
		shoplist_sort = (Button) v.findViewById(R.id.shoplist_sort_filters);
		new SpinnerEmulator(v, R.id.shoplist_sort_filters, R.array.shoplist_sort_filters, R.string.shop_item_sort) {
			@Override
			public void setValue(int value) {
				world.model.uiSelections.selectedShopSort = value;
			}
			@Override
			public void selectionChanged(int value) {
				reloadShownSort(isSelling ? player.inventory : shopInventory);
			}
			@Override
			public int getValue() {
				return world.model.uiSelections.selectedShopSort;
			}
		};

		return v;
	}

	private void reloadShownSort(ItemContainer itemContainer) {
		listAdapter.reloadShownSort(world.model.uiSelections.selectedShopSort, itemContainer, player);
		listAdapter.notifyDataSetChanged();
	}

	// Set up listview so that the focus follows the selection. This is needed to make sure that
	// the correct item is focused when using a gamepad or keyboard to navigate the list.
	private void setupSelectionFocusBehavior(ListView shoplist) {
	shoplist.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			if (view != null) {
				view.post(() -> {
					if (shoplist.hasFocus()) {
						view.requestFocus();
					}
				});
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) { }
	});
}

	@Override
	public void onStart() {
		super.onStart();
		update();
	}

	private Toast lastToast = null;
	protected void displayStoreAction(final String msg) {
		if (lastToast != null) {
			lastToast.setText(msg);
		} else {
			lastToast = Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT);
		}
		lastToast.show();
		update();
	}

	@Override
	public void onPause() {
		super.onPause();
		lastToast = null;
	}

	protected void update() {
		listAdapter.notifyDataSetChanged();
		String gc = getResources().getString(R.string.shop_yourgold, player.getGold());
		shop_gc.setText(gc);
	}
}
