package com.gpl.rpg.AndorsTrail.model.script;

import com.gpl.rpg.AndorsTrail.model.quest.QuestProgress;
import com.gpl.rpg.AndorsTrail.util.ConstRange;

public final class Requirement {
	public static enum RequirementType {
		questProgress
		,questLatestProgress // Highest quest stage reached must match.
		,inventoryRemove	// Player must have item(s) in inventory. Items will be removed when selecting reply.
		,inventoryKeep		// Player must have item(s) in inventory. Items will NOT be removed when selecting reply.
		,wear				// Player must be wearing item(s). Items will NOT be removed when selecting reply.
		,skillLevel			// Player needs to have a specific skill equal to or above a certain level
		,killedMonster
		,timerElapsed
		,usedItem
		,spentGold
		,consumedBonemeals
		,hasActorCondition
		,factionScore
		,random
		,factionScoreEquals
		,wearRemove
	}

	public final RequirementType requireType;
	public final String requireID;
	public final int value;
	public final boolean negate;
	public final ConstRange chance;

	public Requirement(
			RequirementType requireType
			, String requireID
			, int value
			, boolean negate
			, ConstRange chance
	) {
		this.requireType = requireType;
		this.requireID = requireID;
		this.value = value;
		this.negate = negate;
		this.chance = chance;
	}

	public Requirement(QuestProgress qp) {
		this.requireType = RequirementType.questProgress;
		this.requireID = qp.questID;
		this.value = qp.progress;
		this.negate = false;
		this.chance = null;
	}

	public String toString() {
		StringBuilder buf = new StringBuilder(requireType.toString());
		buf.append("--");
		buf.append(requireID);
		buf.append("--");
		if (negate) buf.append('!');
		buf.append(value);
		return buf.toString();
	}

	public boolean isValid() {
		switch (this.requireType) {
			case consumedBonemeals:
				return value >= 0;
			case hasActorCondition:
			case factionScore:
			case factionScoreEquals:
				return requireID != null;
			case inventoryKeep:
			case inventoryRemove:
			case wear:
			case wearRemove:
			case usedItem:
				return requireID != null && value >= 0;
			case killedMonster:
				return requireID != null && value >= 0;
			case questLatestProgress:
			case questProgress:
				return requireID != null && value >= 0;
			case skillLevel:
				return requireID != null && value >= 0;
			case spentGold:
				return value >= 0;
			case random:
				return chance != null;
			case timerElapsed:
				return requireID != null && value >= 0;
			default:
				return false;
		}
	}
}
