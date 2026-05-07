package com.gpl.rpg.AndorsTrail.activity;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.InputController;
import com.gpl.rpg.AndorsTrail.controller.ItemController;
import com.gpl.rpg.AndorsTrail.model.item.ItemType;
import com.gpl.rpg.AndorsTrail.util.ThemeHelper;
import com.gpl.rpg.AndorsTrail.view.CustomDialogFactory;
import com.gpl.rpg.AndorsTrail.view.CustomDialogFactory.CustomDialog;

/**
 * @author ejwessel
 * Creates the BulkSelectionInterface dialog that allows for buy/drop/selling
 */
public final class BulkSelectionInterface extends AndorsTrailBaseActivity implements TextWatcher {

	// class variables
	public static enum BulkInterfaceType {
		buy, sell, drop
	}

	private static final int BUTTON_REPEAT_FIRST_TIME = 300;		// Delay after the touch before the counting starts
	private static final int BUTTON_REPEAT_FURTHER_TIMES = 50;		// Delay between two count events
	private static final int BUTTON_REPEAT_DOUBLE_AFTER = 10;		// after how many count events the countValue doubles?

	private WorldContext world;
	private BulkInterfaceType interfaceType;
	private ItemType itemType;
	private int totalAvailableAmount;
	private int pricePerUnit;

	private TextView bulkselection_summary_totalgold;
	private EditText bulkselection_amount_taken;					// the amount we're going to take from the totalAmount
	private SeekBar bulkselection_slider;
	private Button okButton;
	private Button cancelButton;

	private final Handler timedEventHandler = new Handler();		// variables to count up or down on long presses on the buttons
	private int countValue;
	private int countTime;
	private final Runnable countEvent = new Runnable() {
		@Override
		public void run() {
			incrementValueAndRepeat(BUTTON_REPEAT_FURTHER_TIMES);
		 }
	};

	/*
	 * constructor
	 * @param context - the activity it is displayed upon
	 * @param p_interfaceType - the type of interface: BULK_INTERFACE_BUY, BULK_INTERFACE_SELL or BULK_INTERFACE_DROP
	 * @param p_itemName - the name of the item currently used - example: meat, rock, fish
	 * @param p_totalAmount - the total amount available of that item
	 * @param p_price - the price of the item - not necessary when dropping items
	 * @param p_money - the total amount of money available - only necessary when buying
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(ThemeHelper.getDialogTheme());
		super.onCreate(savedInstanceState);
		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
		if (!app.isInitialized()) { finish(); return; }
		this.world = app.getWorld();
		app.setWindowParameters(this);

		final Resources res = getResources();

		final Intent intent = getIntent();
		Bundle params = intent.getExtras();
		String itemTypeID = params.getString("itemTypeID");
		itemType = world.itemTypes.getItemType(itemTypeID);
		totalAvailableAmount = params.getInt("totalAvailableAmount");
		interfaceType = BulkInterfaceType.valueOf(params.getString("interfaceType"));

		int initialSelection = 1;
		initializeView(this, R.layout.bulkselection, R.id.bulkselection_root);

		// initialize UI variables
		TextView bulkselection_action_type = (TextView)findViewById(R.id.bulkselection_action_type);
		bulkselection_amount_taken = (EditText)findViewById(R.id.bulkselection_amount_taken);
		TextView bulkselection_amount_available = (TextView) findViewById(R.id.bulkselection_amount_available);
		bulkselection_slider = (SeekBar)findViewById(R.id.bulkselection_slider);
		bulkselection_summary_totalgold = (TextView)findViewById(R.id.bulkselection_summary_totalgold);
		okButton = findViewById(R.id.bulkselection_finalize_button);
		cancelButton = findViewById(R.id.bulkselection_cancel_button);
		final Button decrementButton = (Button)findViewById(R.id.bulkselection_decrement_button);
		final Button incrementButton = (Button)findViewById(R.id.bulkselection_increment_button);
		final Button selectAllButton = (Button)findViewById(R.id.bulkselection_select_all_button);

		// change image and name of the item
		final TextView itemName = (TextView)findViewById(R.id.bulkselection_itemname);
		itemName.setText(itemType.getName(world.model.player));
		world.tileManager.setImageViewTileForSingleItemType(res, itemName, itemType);

		int actionTextResourceID = 0;
		if (interfaceType == BulkInterfaceType.buy) {
			pricePerUnit = ItemController.getBuyingPrice(world.model.player, itemType);
			actionTextResourceID = R.string.shop_buy;
		} else if (interfaceType == BulkInterfaceType.sell) {
			pricePerUnit = ItemController.getSellingPrice(world.model.player, itemType);
			actionTextResourceID = R.string.shop_sell;
		} else if (interfaceType == BulkInterfaceType.drop) {
			pricePerUnit = 0;
			actionTextResourceID = R.string.inventory_drop;
			bulkselection_summary_totalgold.setVisibility(View.GONE);
		}
		String actionText = res.getString(actionTextResourceID);

		// initialize the visual components visuals
		okButton.setText(actionText);
		bulkselection_action_type.setText(actionText + ' ');
		bulkselection_amount_available.setText(Integer.toString(totalAvailableAmount));
		bulkselection_slider.setMax(totalAvailableAmount - 1);

		// hide Slider and Buttons when there is only 1 item available
		if(totalAvailableAmount == 1){
			decrementButton.setVisibility(View.GONE);
			incrementButton.setVisibility(View.GONE);
			selectAllButton.setVisibility(View.GONE);
			bulkselection_slider.setVisibility(View.GONE);
		}

		updateControls(initialSelection);

		View.OnTouchListener incrementDecrementListener = (View v, MotionEvent event) -> {
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					startIncrementDecrement(v);
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_OUTSIDE:
					stopIncrementDecrement();
					break;
			}
			return false;
		};

		// Key listener for dpad center / enter — mirrors the touch listener's hold-to-repeat logic.
		View.OnKeyListener incrementDecrementKeyListener = (v, keyCode, event) -> {
			if(InputController.isMappedKey(keyCode, InputController.KEY_SELECT)) {
				switch (event.getAction()) {
					case KeyEvent.ACTION_DOWN:
						if (event.getRepeatCount() == 0) { // only start counting on the first down
							startIncrementDecrement(v);
						}
						return true;
					case KeyEvent.ACTION_UP:
						stopIncrementDecrement();
						return true;
				}
				return true;
			}
			return false;
		};

		// setup decrement button
		decrementButton.setOnTouchListener(incrementDecrementListener);
		decrementButton.setOnKeyListener(incrementDecrementKeyListener);

		// setup increment button
		incrementButton.setOnTouchListener(incrementDecrementListener);
		incrementButton.setOnKeyListener(incrementDecrementKeyListener);

		// setup EditText listeners
		bulkselection_amount_taken.setOnKeyListener((v, keyCode, event) -> {
			if(keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ENTER){
				updateControls(getTextboxAmount());
			}
			return false;
		});

		bulkselection_amount_taken.addTextChangedListener(this);

		// setup slider listener - this supports both touch and dpad input
		bulkselection_slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				// +1 because slider max is totalAvailableAmount - 1
				updateControls(progress + 1);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) { }

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) { }
		});

		// setup OK button
		okButton.setOnClickListener(v -> {
			if (requiresConfirmation()) {
				final String displayType = ItemInfoActivity.getDisplayTypeString(res, itemType).toLowerCase();
				final String message = res.getString(R.string.bulkselection_sell_confirmation, itemType.getName(world.model.player), displayType);

				final CustomDialog d = CustomDialogFactory.createDialog(v.getContext(),
						v.getContext().getResources().getString(R.string.bulkselection_sell_confirmation_title),
						v.getContext().getResources().getDrawable(android.R.drawable.ic_dialog_info),
						message,
						null,
						true);
				CustomDialogFactory.addButton(d, android.R.string.yes, btn -> itemsResult(intent));
				CustomDialogFactory.addDismissButton(d, android.R.string.no);
				CustomDialogFactory.show(d);

			} else {
				itemsResult(intent);
			}
	 	});

		// setup cancel button
		cancelButton.setOnClickListener(v -> {
			setResult(RESULT_CANCELED);
			finish();
		});

		selectAllButton.setOnClickListener(v -> updateControls(totalAvailableAmount));

		// Set focus to Buy button if it is enabled, otherwise to the Cancel button.
		if (okButton.isEnabled()) {
			okButton.post(() -> okButton.requestFocus());
		} else {
			cancelButton.post(() -> cancelButton.requestFocus());
		}
	}

	private void itemsResult(Intent intent){
		Intent result = new Intent();
		result.putExtras(intent);
		result.putExtra("selectedAmount", getTextboxAmount());
		setResult(RESULT_OK, result);
		BulkSelectionInterface.this.finish();
	}

	private void incrementValueAndRepeat(int repeatAfterInterval) {
		if(++countTime % BUTTON_REPEAT_DOUBLE_AFTER == 0) countValue *= 2;
		int newAmount = getTextboxAmount() + countValue;
		updateControls(newAmount);
		if (newAmount <= 1 || newAmount >= totalAvailableAmount) return; // Do not repeat if we have reached the end of the scale.
		timedEventHandler.postDelayed(countEvent, repeatAfterInterval);
	}

	private void startIncrementDecrement(View v) {
		countTime = 0;
		if (v == findViewById(R.id.bulkselection_decrement_button)) countValue = -1;
		if (v == findViewById(R.id.bulkselection_increment_button)) countValue = +1;
		incrementValueAndRepeat(BUTTON_REPEAT_FIRST_TIME);
	}

	private void stopIncrementDecrement() {
		timedEventHandler.removeCallbacks(countEvent);
	}

	private boolean canSelectFinalizeButton() {
		int amount = getTextboxAmount();
		if (amount <= 0) return false;
		if (amount > totalAvailableAmount) return false;

		if (interfaceType == BulkInterfaceType.buy) {
			if (amount * pricePerUnit > world.model.player.getGold()) return false;
		}

		return true;
	}

	// adjusts the amount to the possible interval / synchronizes changes between controls
	private void updateControls(int newAmount) {
		int oldSliderAmount = bulkselection_slider.getProgress() + 1;
		int oldEditboxAmount = getTextboxAmount();

		// adjust amount
		if (newAmount < 1) newAmount = 1;
		if (newAmount > totalAvailableAmount) newAmount = totalAvailableAmount;

		// update controls
		if (newAmount != oldEditboxAmount) bulkselection_amount_taken.setText(Integer.toString(newAmount));	// change the amount taken/text
		if (newAmount != oldSliderAmount) bulkselection_slider.setProgress(newAmount - 1);					// change the amount taken/text

		// display buying/selling information if not dropping
		if (interfaceType == BulkInterfaceType.buy) {
			bulkselection_summary_totalgold.setText(getResources().getString(R.string.bulkselection_totalcost_buy, newAmount * pricePerUnit));
		} else if (interfaceType == BulkInterfaceType.sell) {
			bulkselection_summary_totalgold.setText(getResources().getString(R.string.bulkselection_totalcost_sell, newAmount * pricePerUnit));
		}

		okButton.setEnabled(canSelectFinalizeButton());
	}

	private int getTextboxAmount() {
		final String s = bulkselection_amount_taken.getText().toString();
		if (s.equals("")) return 0;
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException ignored) { }
		return 0;
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) { }

	@Override
	public void afterTextChanged(Editable s) {
		if (bulkselection_amount_taken.getText().toString().equals("")) return;
		int newAmount = getTextboxAmount();
		updateControls(newAmount);
	}

	private boolean requiresConfirmation() {
		if (interfaceType != BulkInterfaceType.sell) return false;
		if (itemType.isOrdinaryItem()) return false;
		return true;
	}
}
