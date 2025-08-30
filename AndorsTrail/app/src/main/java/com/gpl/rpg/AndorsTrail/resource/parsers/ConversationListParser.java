package com.gpl.rpg.AndorsTrail.resource.parsers;

import org.json.JSONException;
import org.json.JSONObject;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.model.conversation.Phrase;
import com.gpl.rpg.AndorsTrail.model.conversation.Reply;
import com.gpl.rpg.AndorsTrail.model.script.Requirement;
import com.gpl.rpg.AndorsTrail.model.script.ScriptEffect;
import com.gpl.rpg.AndorsTrail.resource.TranslationLoader;
import com.gpl.rpg.AndorsTrail.resource.parsers.json.JsonArrayParserFor;
import com.gpl.rpg.AndorsTrail.resource.parsers.json.JsonCollectionParserFor;
import com.gpl.rpg.AndorsTrail.resource.parsers.json.JsonFieldNames;
import com.gpl.rpg.AndorsTrail.util.L;
import android.util.Pair;

public final class ConversationListParser extends JsonCollectionParserFor<Phrase> {

	private final TranslationLoader translationLoader;

	private final JsonArrayParserFor<Requirement> requirementParser = new JsonArrayParserFor<Requirement>(Requirement.class) {
		@Override
		protected Requirement parseObject(JSONObject o) throws JSONException {
			Requirement.RequirementType type = Requirement.RequirementType.valueOf(o.getString(JsonFieldNames.ReplyRequires.requireType));
			String requireID = o.getString(JsonFieldNames.ReplyRequires.requireID);
			return new Requirement(
					type
					,type == Requirement.RequirementType.random ? null : requireID
					,o.optInt(JsonFieldNames.ReplyRequires.value, 0)
					,o.optBoolean(JsonFieldNames.ReplyRequires.negate, false)
					,type == Requirement.RequirementType.random ? ResourceParserUtils.parseChance(requireID) : null
			);
		}
	};

	private final JsonArrayParserFor<Reply> replyParser = new JsonArrayParserFor<Reply>(Reply.class) {
		@Override
		protected Reply parseObject(JSONObject o) throws JSONException {
			return new Reply(
					translationLoader.translateConversationReply(o.optString(JsonFieldNames.Reply.text, ""))
					,o.getString(JsonFieldNames.Reply.nextPhraseID)
					,requirementParser.parseArray(o.optJSONArray(JsonFieldNames.Reply.requires))
			);
		}
	};

	private final JsonArrayParserFor<ScriptEffect> scriptEffectParser = new JsonArrayParserFor<ScriptEffect>(ScriptEffect.class) {
		@Override
		protected ScriptEffect parseObject(JSONObject o) throws JSONException {
			return new ScriptEffect(
					ScriptEffect.ScriptEffectType.valueOf(o.getString(JsonFieldNames.PhraseReward.rewardType))
					,o.getString(JsonFieldNames.PhraseReward.rewardID)
					,o.optInt(JsonFieldNames.PhraseReward.value, 0)
					,o.optString(JsonFieldNames.PhraseReward.mapName, null)
			);
		}
	};

	public ConversationListParser(TranslationLoader translationLoader) {
		this.translationLoader = translationLoader;
	}

	@Override
	protected Pair<String, Phrase> parseObject(JSONObject o) throws JSONException {
		final String id = o.getString(JsonFieldNames.Phrase.phraseID);

		Reply[] _replies = null;
		ScriptEffect[] _scriptEffects = null;
		try {
			_replies = replyParser.parseArray(o.optJSONArray(JsonFieldNames.Phrase.replies));
			_scriptEffects = scriptEffectParser.parseArray(o.optJSONArray(JsonFieldNames.Phrase.rewards));
		} catch (JSONException e) {
			if (AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES) {
				L.log("ERROR: parsing phrase " + id + " : " + e.getMessage());
			}
		}

		return new Pair<String, Phrase>(id, new Phrase(
				translationLoader.translateConversationPhrase(o.optString(JsonFieldNames.Phrase.message, null))
				, _replies
				, _scriptEffects
				, o.optString(JsonFieldNames.Phrase.switchToNPC, null)
		));
	}
}
