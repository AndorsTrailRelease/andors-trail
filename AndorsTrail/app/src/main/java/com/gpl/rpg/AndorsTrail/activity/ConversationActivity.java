package com.gpl.rpg.AndorsTrail.activity;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.Dialogs;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.ConversationController;
import com.gpl.rpg.AndorsTrail.model.actor.Actor;
import com.gpl.rpg.AndorsTrail.model.actor.Monster;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.model.conversation.Reply;
import com.gpl.rpg.AndorsTrail.model.item.Loot;
import com.gpl.rpg.AndorsTrail.model.quest.Quest;
import com.gpl.rpg.AndorsTrail.model.quest.QuestLogEntry;
import com.gpl.rpg.AndorsTrail.model.quest.QuestProgress;
import com.gpl.rpg.AndorsTrail.resource.tiles.TileCollection;
import com.gpl.rpg.AndorsTrail.resource.tiles.TileManager;
import com.gpl.rpg.AndorsTrail.util.ThemeHelper;

public final class ConversationActivity
		extends AndorsTrailBaseActivity
		implements OnKeyListener
		, ConversationController.ConversationStatemachine.ConversationStateListener {

	private WorldContext world;
	private Player player;
	private final ArrayList<ConversationStatement> conversationHistory = new ArrayList<ConversationStatement>();
	private ConversationController.ConversationStatemachine conversationState;
	private int numberOfNewMessage = 0;

	private StatementContainerAdapter listAdapter;
	private Button nextButton;
	private ListView statementList;
	private RadioGroup replyGroup;
	private OnCheckedChangeListener radioButtonListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(ThemeHelper.getDialogTheme());
		super.onCreate(savedInstanceState);
		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
		if (!app.isInitialized()) { finish(); return; }
		this.world = app.getWorld();
		this.player = world.model.player;
		this.conversationState = new ConversationController.ConversationStatemachine(world, app.getControllerContext(), this);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		initializeView(this, R.layout.conversation, R.id.conversation_root);

        setFinishOnTouchOutside(false);

        replyGroup = new RadioGroup(this);
		replyGroup.setLayoutParams(new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, ListView.LayoutParams.WRAP_CONTENT));
		statementList = (ListView) findViewById(R.id.conversation_statements);
		statementList.addFooterView(replyGroup);
		listAdapter = new StatementContainerAdapter(this, conversationHistory, world.tileManager, world.model.currentMaps.tiles);
		statementList.setAdapter(listAdapter);

		nextButton = (Button) findViewById(R.id.conversation_next);
		Button leaveButton = (Button) findViewById(R.id.conversation_leave);
		leaveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ConversationActivity.this.finish();
			}
		});

		radioButtonListener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				nextButton.setEnabled(true);
			}
		};
		nextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				nextButtonClicked();
			}
		});
		nextButton.setEnabled(false);

		statementList.setOnKeyListener(this);

		statementList.setSelected(false);
		statementList.setFocusable(false);
		statementList.setFocusableInTouchMode(false);

		String phraseID;
		boolean applyScriptEffectsForFirstPhrase;
		boolean displayLastMessage = true;
		if (savedInstanceState != null) {
			conversationState.setCurrentNPC(Dialogs.getMonsterFromBundle(savedInstanceState, world));
			ArrayList<ConversationStatement> savedConversationHistory = savedInstanceState.getParcelableArrayList("conversationHistory");
			if (savedConversationHistory != null) conversationHistory.addAll(savedConversationHistory);
			phraseID = savedInstanceState.getString("phraseID");
			applyScriptEffectsForFirstPhrase = false;
			displayLastMessage = false;
		} else {
			conversationState.setCurrentNPC(Dialogs.getMonsterFromIntent(getIntent(), world));
			phraseID = getIntent().getData().getLastPathSegment();
			applyScriptEffectsForFirstPhrase = getIntent().getBooleanExtra("applyScriptEffectsForFirstPhrase", true);
		}
		conversationState.proceedToPhrase(getResources(), phraseID, applyScriptEffectsForFirstPhrase, displayLastMessage);
	}

	@Override
	protected void onResume() {
		super.onResume();
		nextButton.requestFocus();
	}

	private int getSelectedReplyIndex() {
		int j = 0;
		for (int i = 0; i < replyGroup.getChildCount(); ++i) {
			final View v = replyGroup.getChildAt(i);
			if (v == null) continue;
			if (!(v instanceof RadioButton)) {
				continue;
			}
			final RadioButton rb = (RadioButton) v;
			if (rb.isChecked()) return j;
			j++;
		}
		return -1;
	}

	private void setSelectedReplyIndex(int i) {
		int replyCount = replyGroup.getChildCount();
		if (replyCount <= 0) return;
		if (i < 0) i = 0;
		else if (i >= (replyCount-1)) i = replyCount-1;

		for (int j = 0; j < replyGroup.getChildCount(); ++j) {
			View v = replyGroup.getChildAt(j);
			if (v == null) continue;
			if (!(v instanceof RadioButton)) {
				continue;
			}
			i--;
			if (i < 0) {
				RadioButton rb = (RadioButton) v;
				rb.setChecked(true);
				break;
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (handleKeypress(keyCode)) return true;
		else return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKey(View arg0, int keyCode, KeyEvent event) {
		if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
		return handleKeypress(keyCode);
	}

	public boolean handleKeypress(int keyCode) {
		int selectedReplyIndex = getSelectedReplyIndex();
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_UP:
			--selectedReplyIndex;
			setSelectedReplyIndex(selectedReplyIndex);
			return true;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			++selectedReplyIndex;
			setSelectedReplyIndex(selectedReplyIndex);
			return true;
		case KeyEvent.KEYCODE_SPACE:
		case KeyEvent.KEYCODE_ENTER:
		case KeyEvent.KEYCODE_DPAD_CENTER:
			if (nextButton.isEnabled()) nextButton.performClick();
			return true;
		case KeyEvent.KEYCODE_1: setSelectedReplyIndex(0); return true;
		case KeyEvent.KEYCODE_2: setSelectedReplyIndex(1); return true;
		case KeyEvent.KEYCODE_3: setSelectedReplyIndex(2); return true;
		case KeyEvent.KEYCODE_4: setSelectedReplyIndex(3); return true;
		case KeyEvent.KEYCODE_5: setSelectedReplyIndex(4); return true;
		case KeyEvent.KEYCODE_6: setSelectedReplyIndex(5); return true;
		case KeyEvent.KEYCODE_7: setSelectedReplyIndex(6); return true;
		case KeyEvent.KEYCODE_8: setSelectedReplyIndex(7); return true;
		case KeyEvent.KEYCODE_9: setSelectedReplyIndex(8); return true;
		default: return false;
		}
	}

	private RadioButton getSelectedReplyButton() {
		for (int i = 0; i < replyGroup.getChildCount(); ++i) {
			final View v = replyGroup.getChildAt(i);
			if (v == null) continue;
			if (!(v instanceof RadioButton)) {
				continue;
			}
			final RadioButton rb = (RadioButton) v;
			if (rb.isChecked()) {
				return rb;
			}
		}
		return null; // No reply was found. This is probably an error.
	}

	private void greyAllConversationStatement(){
		int numberOfMessage = this.conversationHistory.size();
		while(numberOfNewMessage != 0){
			ConversationStatement conversation = conversationHistory.get(numberOfMessage - numberOfNewMessage);
			if(conversation.hasActor()){
				conversation.textColor = getSpanColor(R.attr.ui_theme_dialogue_dark_color);
				if(conversation.isPlayerActor){
					conversation.nameColor = getSpanColor(R.attr.ui_theme_playername_light_color);
				} else {
					conversation.nameColor = getSpanColor(R.attr.ui_theme_npcname_dark_color);
				}
			} else if (conversation.isReward) {
				conversation.textColor = getSpanColor(R.attr.ui_theme_reward_light_color);
			} else {
				conversation.textColor = getSpanColor(R.attr.ui_theme_dialogue_dark_color);;
			}
			numberOfNewMessage--;
		}
	}

	private void nextButtonClicked() {
		greyAllConversationStatement();
		RadioButton rb = getSelectedReplyButton();
		replyGroup.removeAllViews();
		nextButton.setEnabled(false);
		if (conversationState.hasOnlyOneNextReply()) {
			conversationState.playerSelectedNextStep(getResources());
		} else {
			if (rb == null) return;
			Reply r = (Reply) rb.getTag();
			addConversationStatement(player, rb.getText().toString(), getSpanColor(R.attr.ui_theme_dialogue_light_color), false);
			conversationState.playerSelectedReply(getResources(), r);
		}
	}

	private void addConversationStatement(Actor actor, String text, int textColor, boolean isReward) {
		ConversationStatement s = new ConversationStatement();
		if (actor != null) {
			s.iconID = actor.iconID;
			s.actorName = actor.getName();
		} else {
			s.iconID = ConversationStatement.NO_ICON;
		}
		s.text = text;
		s.nameColor = actor == player ? getSpanColor(R.attr.ui_theme_playername_light_color) : getSpanColor(R.attr.ui_theme_npcname_light_color);
		s.textColor = textColor;
		s.isPlayerActor = actor != null && actor == player;
		s.isReward = isReward;
		conversationHistory.add(s);
		numberOfNewMessage++;
		statementList.clearFocus();
		listAdapter.notifyDataSetChanged();
		statementList.requestLayout();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString("phraseID", conversationState.getCurrentPhraseID());
		outState.putParcelableArrayList("conversationHistory", conversationHistory);
		Dialogs.addMonsterIdentifiers(outState, conversationState.getCurrentNPC());
	}

	private static final class ConversationStatement implements Parcelable {
		public static final int NO_ICON = -1;

		public String actorName;
		public String text;
		public int iconID;
		public int nameColor;
		public int textColor;
		public boolean isPlayerActor;
		public boolean isReward;

		public boolean hasActor() {
			return iconID != NO_ICON;
		}

		@Override
		public int describeContents() { return 0; }

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(actorName);
			dest.writeString(text);
			dest.writeInt(iconID);
			dest.writeInt(nameColor);
			dest.writeInt(textColor);
			dest.writeByte((byte) (isPlayerActor ? 1 : 0));
			dest.writeByte((byte) (isReward ? 1 : 0));
		}

		@SuppressWarnings("unused")
		public static final Parcelable.Creator<ConversationStatement> CREATOR = new Parcelable.Creator<ConversationStatement>() {
			@Override
			public ConversationStatement createFromParcel(Parcel in) {
				ConversationStatement result = new ConversationStatement();
				result.actorName = in.readString();
				result.text = in.readString();
				result.iconID = in.readInt();
				result.nameColor = in.readInt();
				result.textColor = in.readInt();
				result.isPlayerActor = in.readByte() == 1;
				result.isReward = in.readByte() == 1;
				return result;
			}

			@Override
			public ConversationStatement[] newArray(int size) {
				return new ConversationStatement[size];
			}
		};
	}

	private static final class StatementContainerAdapter extends ArrayAdapter<ConversationStatement> {

		private final TileManager tileManager;
		private final TileCollection tiles;

		public StatementContainerAdapter(Context context, ArrayList<ConversationStatement> items, TileManager tileManager, TileCollection tiles) {
			super(context, 0, items);
			this.tileManager = tileManager;
			this.tiles = tiles;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final ConversationStatement statement = getItem(position);
			View result = convertView;
			if (result == null) {
				result = View.inflate(getContext(), R.layout.conversation_statement, null);
			}

			final TextView tv = (TextView) result.findViewById(R.id.conversation_text);
			if (statement.hasActor()) {
				final Resources res = getContext().getResources();
				if (statement.isPlayerActor) tileManager.setImageViewTileForPlayer(res, tv, statement.iconID);
				else tileManager.setImageViewTileForMonster(res, tv, statement.iconID, tiles);

				tv.setText(statement.actorName + ": " + statement.text, BufferType.SPANNABLE);
				Spannable sp = (Spannable) tv.getText();
				sp.setSpan(new ForegroundColorSpan(statement.nameColor), 0, statement.actorName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				if (statement.textColor != 0) {
					sp.setSpan(new ForegroundColorSpan(statement.textColor), statement.actorName.length()+1, sp.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			} else {
				tv.setCompoundDrawables(null, null, null, null);
				if (statement.textColor == 0) {
					tv.setText(statement.text);
				} else {
					tv.setText(statement.text, BufferType.SPANNABLE);
					Spannable sp = (Spannable) tv.getText();
					sp.setSpan(new ForegroundColorSpan(statement.textColor), 0, statement.text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}

			return result;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return false;
		}
	}

	@Override
	public void onTextPhraseReached(String message, Actor actor, String phraseID) {
		addConversationStatement(actor, message, getSpanColor(R.attr.ui_theme_dialogue_light_color), false);
	}

	@Override
	public void onScriptEffectsApplied(ConversationController.ScriptEffectResult scriptEffectResult) {
		Loot loot = scriptEffectResult.loot;

		for (QuestProgress reward : scriptEffectResult.questProgress) {
			Quest q = world.quests.getQuest(reward.questID);
			if (!q.showInLog) continue;
			QuestLogEntry logEntry = q.getQuestLogEntry(reward.progress);
			if (logEntry.finishesQuest) {
				addRewardMessage(getString(R.string.conversation_reward_quest_finished, q.name));
			} else {
				addRewardMessage(getString(R.string.conversation_reward_quest_updated, q.name));
			}
		}
		if (loot.exp > 0) {
			addRewardMessage(getString(R.string.conversation_rewardexp, loot.exp));
		}
		if (loot.gold > 0) {
			addRewardMessage(getString(R.string.conversation_rewardgold, loot.gold));
		} else if (loot.gold < 0) {
			addRewardMessage(getString(R.string.conversation_lostgold, -loot.gold));
		}
		if (!loot.items.isEmpty()) {
			final int len = loot.items.countItems();
			if (len == 1) {
				addRewardMessage(getString(R.string.conversation_rewarditem));
			} else {
				addRewardMessage(getString(R.string.conversation_rewarditems, len));
			}
		}
	}

	private void addRewardMessage(String text) {
		addConversationStatement(null, text, getSpanColor(R.attr.ui_theme_reward_light_color), true);
	}

	@Override
	public void onConversationEnded() {
		ConversationActivity.this.finish();
	}

	@Override
	public void onConversationEndedWithShop(Monster npc) {
		Intent intent = new Intent(this, ShopActivity.class);
		Dialogs.addMonsterIdentifiers(intent, npc);
		startActivity(intent);
		ConversationActivity.this.finish();
	}

	@Override
	public void onConversationEndedWithCombat(Monster npc) {
		ConversationActivity.this.finish();
	}

	@Override
	public void onConversationEndedWithRemoval(Monster npc) {
		ConversationActivity.this.finish();
	}

	@Override
	public void onConversationCanProceedWithNext() {
		nextButton.setEnabled(true);
	}

	@Override
	public void onConversationHasReply(Reply r, String message) {
		RadioGroup.LayoutParams layoutParams = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT);
		layoutParams.setMargins(0, getResources().getDimensionPixelOffset(R.dimen.conversation_replyseparator_margintop), 0, getResources().getDimensionPixelOffset(R.dimen.conversation_replyseparator_marginbottom));
		RadioButton rb = new RadioButton(this);
		rb.setLayoutParams(layoutParams);
		rb.setText(message);
		rb.setOnCheckedChangeListener(radioButtonListener);
		rb.setTag(r);
		rb.setShadowLayer(1, 1, 1, Color.BLACK);
		rb.setFocusable(false);
		rb.setFocusableInTouchMode(false);
		if (replyGroup.getChildCount() == 0) { //Add a separator before first item
			ImageView iv = new ImageView(this);
			iv.setBackgroundResource(ThemeHelper.getThemeResource(this, R.attr.ui_theme_listseparator_drawable));
			RadioGroup.LayoutParams ivLayoutParams = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.MATCH_PARENT, RadioGroup.LayoutParams.WRAP_CONTENT);
			ivLayoutParams.setMargins(0, getResources().getDimensionPixelOffset(R.dimen.conversation_replyseparator_margintop), 0, getResources().getDimensionPixelOffset(R.dimen.conversation_replyseparator_marginbottom));
			iv.setLayoutParams(ivLayoutParams);
			replyGroup.addView(iv, ivLayoutParams);
		}
		replyGroup.addView(rb, layoutParams);
	}
	
	private int getSpanColor(int resId) {
		return ThemeHelper.getThemeColor(this, resId);
	}
}
