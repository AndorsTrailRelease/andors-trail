package com.gpl.rpg.AndorsTrail.resource.parsers;

import org.json.JSONException;
import org.json.JSONObject;

import com.gpl.rpg.AndorsTrail.controller.Constants;
import com.gpl.rpg.AndorsTrail.model.ability.ActorConditionTypeCollection;
import com.gpl.rpg.AndorsTrail.model.actor.MonsterType;
import com.gpl.rpg.AndorsTrail.model.item.DropListCollection;
import com.gpl.rpg.AndorsTrail.model.item.ItemTraits_OnHitReceived;
import com.gpl.rpg.AndorsTrail.model.item.ItemTraits_OnUse;
import com.gpl.rpg.AndorsTrail.resource.DynamicTileLoader;
import com.gpl.rpg.AndorsTrail.resource.TranslationLoader;
import com.gpl.rpg.AndorsTrail.resource.parsers.json.JsonCollectionParserFor;
import com.gpl.rpg.AndorsTrail.resource.parsers.json.JsonFieldNames;
import com.gpl.rpg.AndorsTrail.util.ConstRange;
import com.gpl.rpg.AndorsTrail.util.Pair;
import com.gpl.rpg.AndorsTrail.util.Size;

public final class MonsterTypeParser extends JsonCollectionParserFor<MonsterType> {

	private final Size size1x1 = new Size(1, 1);
	private final DropListCollection droplists;
	private final ItemTraitsParser itemTraitsParser;
	private final DynamicTileLoader tileLoader;
	private final TranslationLoader translationLoader;

	public MonsterTypeParser(
			final DropListCollection droplists,
			final ActorConditionTypeCollection actorConditionTypes,
			final DynamicTileLoader tileLoader,
			final TranslationLoader translationLoader) {
		this.translationLoader = translationLoader;
		this.itemTraitsParser = new ItemTraitsParser(actorConditionTypes);
		this.droplists = droplists;
		this.tileLoader = tileLoader;
	}

	@Override
	protected Pair<String, MonsterType> parseObject(JSONObject o) throws JSONException {
		final String monsterTypeID = o.getString(JsonFieldNames.Monster.monsterTypeID);

		int maxHP = o.optInt(JsonFieldNames.Monster.maxHP, 1);
		int maxAP = o.optInt(JsonFieldNames.Monster.maxAP, 10);
		int attackCost = o.optInt(JsonFieldNames.Monster.attackCost, 10);
		int attackChance = o.optInt(JsonFieldNames.Monster.attackChance, 0);
		ConstRange damagePotential = ResourceParserUtils.parseConstRange(o.optJSONObject(JsonFieldNames.Monster.attackDamage));
		int criticalSkill = o.optInt(JsonFieldNames.Monster.criticalSkill, 0);
		float criticalMultiplier = (float) o.optDouble(JsonFieldNames.Monster.criticalMultiplier, 0);
		int blockChance = o.optInt(JsonFieldNames.Monster.blockChance, 0);
		int damageResistance = o.optInt(JsonFieldNames.Monster.damageResistance, 0);
		final ItemTraits_OnUse hitEffect = itemTraitsParser.parseItemTraits_OnUse(o.optJSONObject(JsonFieldNames.Monster.hitEffect));
		final ItemTraits_OnHitReceived hitReceivedEffect = itemTraitsParser.parseItemTraits_OnHitReceived(o.optJSONObject(JsonFieldNames.Monster.hitReceivedEffect));
		final ItemTraits_OnUse deathEffect = itemTraitsParser.parseItemTraits_OnUse(o.optJSONObject(JsonFieldNames.Monster.deathEffect));

		final int exp = getExpectedMonsterExperience(attackCost, attackChance, damagePotential, criticalSkill, criticalMultiplier, blockChance, damageResistance, hitEffect, maxHP, maxAP);

		final int horizontalFlipChance = o.optInt(JsonFieldNames.Monster.horizontalFlipChance, 0);

		return new Pair<String, MonsterType>(monsterTypeID, new MonsterType(
				monsterTypeID
				, translationLoader.translateMonsterTypeName(o.getString(JsonFieldNames.Monster.name))
				, o.optString(JsonFieldNames.Monster.spawnGroup, monsterTypeID)
				, exp
				, droplists.getDropList(o.optString(JsonFieldNames.Monster.droplistID, null))
				, o.optString(JsonFieldNames.Monster.phraseID, null)
				, o.optInt(JsonFieldNames.Monster.unique, 0) > 0
				, o.optString(JsonFieldNames.Monster.faction, null)
				, MonsterType.MonsterClass.fromString(o.optString(JsonFieldNames.Monster.monsterClass, null), MonsterType.MonsterClass.humanoid)
				, MonsterType.AggressionType.fromString(o.optString(JsonFieldNames.Monster.movementAggressionType, null), MonsterType.AggressionType.none)
				, ResourceParserUtils.parseTilesetTileSize(tileLoader, o.optString(JsonFieldNames.Monster.iconID, null), size1x1)
				, ResourceParserUtils.parseImageID(tileLoader, o.getString(JsonFieldNames.Monster.iconID))
				, horizontalFlipChance
				, maxAP
				, maxHP
				, o.optInt(JsonFieldNames.Monster.moveCost, 10)
				, attackCost
				, attackChance
				, criticalSkill
				, criticalMultiplier
				, damagePotential
				, blockChance
				, damageResistance
				, hitEffect == null ? null : new ItemTraits_OnUse[] { hitEffect }
				, hitReceivedEffect == null ? null : new ItemTraits_OnHitReceived[] { hitReceivedEffect }
				, deathEffect
		));
	}

	private static float div100(int v) {
		return (float) v / 100f;
	}
	private static int getExpectedMonsterExperience(
			int attackCost,
			int attackChance,
			ConstRange damagePotential,
			int criticalSkill,
			float criticalMultiplier,
			int blockChance,
			int damageResistance,
			ItemTraits_OnUse hitEffect,
			final int maxHP,
			final int maxAP) {
		final float averageDamage = damagePotential != null ? damagePotential.averagef() : 0;
		final float avgAttackHP = getAttacksPerTurn(maxAP, attackCost) * div100(attackChance) * averageDamage * (1 + div100(criticalSkill) * criticalMultiplier);
		final float avgDefenseHP = maxHP * (1 + div100(blockChance)) + Constants.EXP_FACTOR_DAMAGERESISTANCE * damageResistance;
		int attackConditionBonus = 0;
		if (hitEffect != null && hitEffect.addedConditions_target != null && hitEffect.addedConditions_target.length > 0) {
			attackConditionBonus += 50;
		}
		return (int) Math.ceil((avgAttackHP * 3 + avgDefenseHP) * Constants.EXP_FACTOR_SCALING) + attackConditionBonus;
	}
	private static int getAttacksPerTurn(int maxAP, int attackCost) {
		return (int) Math.floor(maxAP / attackCost);
	}
}
