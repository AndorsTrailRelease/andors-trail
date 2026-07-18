package com.gpl.rpg.AndorsTrail.activity.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.model.quest.Quest;
import com.gpl.rpg.AndorsTrail.model.quest.QuestLogEntry;
import com.gpl.rpg.AndorsTrail.view.SpinnerEmulator;

public final class HeroinfoActivity_Quests extends Fragment {

	private WorldContext world;

//	private Button includeCompletedButton;
//	private Dialog includeCompletedDialog = null;
//	private ListView questlog_includecompleted;
	
	private SimpleExpandableListAdapter questlog_contents_adapter;

	private Player player;

	private final List<Map<String, ?>> groupList = new ArrayList<Map<String, ?>>();
	private final List<List<Map<String, ?>>> childList = new ArrayList<List<Map<String,?>>>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this.getActivity());
		if (!app.isInitialized()) return;
		this.world = app.getWorld();
		this.player = world.model.player;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.questlog, container, false);
		
		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this.getActivity());
		if (!app.isInitialized()) return v;
		
		Context ctx = getActivity();

		
		new SpinnerEmulator(v, R.id.questlog_includecompleted_button, R.array.questlog_includecompleted, R.string.questlog_includecompleted_prompt) {
			@Override
			public void setValue(int value) {
				world.model.uiSelections.selectedQuestFilter = value;
			}
			
			@Override
			public void selectionChanged(int value) {
				reloadQuests();
			}
			
			@Override
			public int getValue() {
				return world.model.uiSelections.selectedQuestFilter;
			}
		};
		
		ExpandableListView questlog_contents = (ExpandableListView) v.findViewById(R.id.questlog_contents);
		questlog_contents_adapter = new SimpleExpandableListAdapter(
				ctx
				, groupList
				, R.layout.questlogname
				, new String[] { mn_questName, mn_questStatus }
				, new int[] { R.id.questlog_questname, R.id.questlog_queststatus }
				, childList
				, R.layout.questlogentry
				, new String[] { mn_logText }
				, new int[] { R.id.questlog_entrytext }
			);
		questlog_contents.setAdapter(questlog_contents_adapter);

		return v;
	}

	private static final String mn_questName = "questName";
	private static final String mn_questStatus = "questStatus";
	private static final String mn_logText = "logText";

	@Override
	public void onStart() {
		super.onStart();
		update();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		update();
	}

	private void update() {
		reloadQuests();
	}

	private void reloadQuests() {
		groupList.clear();
		childList.clear();

		for (String questProgressID : player.getAllQuestProgressIDs()) {
			Quest q = world.quests.getQuest(questProgressID);
			if (q == null) continue; // This should not happen
			if (!q.showInLog) continue; // Do not show
			boolean isCompleted = q.isCompleted(player);

			int v = world.model.uiSelections.selectedQuestFilter;
			if (v == 0) { // Active quests
				if (isCompleted) continue;
			} else if (v == 1) { // All quests
				// Always show.
			} else if (v == 2) { // Completed quests
				if (!isCompleted) continue;
			}

			int statusResId;
			if (isCompleted) {
				statusResId = R.string.questlog_queststatus_completed;
			} else {
				statusResId = R.string.questlog_queststatus_inprogress;
			}

			Map<String, Object> item = new HashMap<String, Object>();
			item.put(mn_questName, q.name);
			//item.put(mn_questStatus, getString(R.string.questlog_queststatus, getString(statusResId)));
			if(isCompleted) item.put(mn_questStatus, getString(R.string.questlog_queststatus_checkmark));
			groupList.add(item);

			List<Map<String, ?>> logItemList = new ArrayList<Map<String, ?>>();
			for(Integer progress : player.getQuestProgress(q.questID)) {
				for(QuestLogEntry e : q.stages) {
					if (e.progress == progress.intValue()) {
						if (e.logtext.length() > 0) {
							item = new HashMap<String, Object>();
							item.put(mn_logText, e.logtext);
							logItemList.add(item);
						}
						continue;
					}
				}
			}
			childList.add(logItemList);
		}
		questlog_contents_adapter.notifyDataSetChanged();
	}
}
