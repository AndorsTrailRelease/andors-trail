package com.gpl.rpg.AndorsTrail.resource.parsers;

import java.util.Arrays;
import java.util.Comparator;

import org.json.JSONException;
import org.json.JSONObject;

import com.gpl.rpg.AndorsTrail.model.quest.Quest;
import com.gpl.rpg.AndorsTrail.model.quest.QuestLogEntry;
import com.gpl.rpg.AndorsTrail.resource.TranslationLoader;
import com.gpl.rpg.AndorsTrail.resource.parsers.json.JsonArrayParserFor;
import com.gpl.rpg.AndorsTrail.resource.parsers.json.JsonCollectionParserFor;
import com.gpl.rpg.AndorsTrail.resource.parsers.json.JsonFieldNames;
import android.util.Pair;

public final class QuestParser extends JsonCollectionParserFor<Quest> {
	private final TranslationLoader translationLoader;
	private int sortOrder = 0;

	private final JsonArrayParserFor<QuestLogEntry> questLogEntryParser = new JsonArrayParserFor<QuestLogEntry>(QuestLogEntry.class) {
		@Override
		protected QuestLogEntry parseObject(JSONObject o) throws JSONException {
			return new QuestLogEntry(
					o.getInt(JsonFieldNames.QuestLogEntry.progress)
					,translationLoader.translateQuestLogEntry(o.optString(JsonFieldNames.QuestLogEntry.logText, null))
					,o.optInt(JsonFieldNames.QuestLogEntry.rewardExperience, 0)
					,o.optInt(JsonFieldNames.QuestLogEntry.finishesQuest, 0) > 0
			);
		}
	};
	private final Comparator<QuestLogEntry> sortByQuestProgress = new Comparator<QuestLogEntry>() {
		@Override
		public int compare(QuestLogEntry a, QuestLogEntry b) {
			return a.progress - b.progress;
		}
	};

	public QuestParser(TranslationLoader translationLoader) {
		this.translationLoader = translationLoader;
	}

	@Override
	protected Pair<String, Quest> parseObject(JSONObject o) throws JSONException {
		final String id = o.getString(JsonFieldNames.Quest.questID);

		QuestLogEntry[] stages = questLogEntryParser.parseArray(o.getJSONArray(JsonFieldNames.Quest.stages));
		Arrays.sort(stages, sortByQuestProgress);

		++sortOrder;

		return new Pair<String, Quest>(id, new Quest(
				id
				, translationLoader.translateQuestName(o.getString(JsonFieldNames.Quest.name))
				, stages
				, o.optInt(JsonFieldNames.Quest.showInLog, 0) > 0
				, sortOrder
		));
	}
}
