package com.gpl.rpg.AndorsTrail.resource.parsers.json;

public final class JsonFieldNames {
	public static final class ActorCondition {
		public static final String conditionTypeID = "id";
		public static final String name = "name";
		public static final String description = "description";
		public static final String iconID = "iconID";
		public static final String category = "category";
		public static final String isStacking = "isStacking";
		public static final String isPositive = "isPositive";
		public static final String roundEffect = "roundEffect";
		public static final String fullRoundEffect = "fullRoundEffect";
		public static final String abilityEffect = "abilityEffect";
	}

	public static final class StatsModifierTraits {
		public static final String visualEffectID = "visualEffectID";
		public static final String increaseCurrentHP = "increaseCurrentHP";
		public static final String increaseCurrentAP = "increaseCurrentAP";
	}

	public static final class AbilityModifierTraits {
		public static final String increaseMaxHP = "increaseMaxHP";
		public static final String increaseMaxAP = "increaseMaxAP";
		public static final String increaseMoveCost = "increaseMoveCost";
		public static final String increaseUseItemCost = "increaseUseItemCost";
		public static final String increaseReequipCost = "increaseReequipCost";
		public static final String increaseAttackCost = "increaseAttackCost";
		public static final String increaseAttackChance = "increaseAttackChance";
		public static final String increaseCriticalSkill = "increaseCriticalSkill";
		public static final String setCriticalMultiplier = "setCriticalMultiplier";
		public static final String increaseAttackDamage = "increaseAttackDamage";
		public static final String setNonWeaponDamageModifier = "setNonWeaponDamageModifier";
		public static final String increaseBlockChance = "increaseBlockChance";
		public static final String increaseDamageResistance = "increaseDamageResistance";
	}

	public static final class ItemCategory {
		public static final String itemCategoryID = "id";
		public static final String name = "name";
		public static final String actionType = "actionType";
		public static final String inventorySlot = "inventorySlot";
		public static final String size = "size";
	}

	public static final class DropList {
		public static final String dropListID = "id";
		public static final String items = "items";
	}

	public static final class DropItem {
		public static final String itemID = "itemID";
		public static final String quantity = "quantity";
		public static final String chance = "chance";
	}

	public static final class Range {
		public static final String min = "min";
		public static final String max = "max";
	}

	public static final class Phrase {
		public static final String phraseID = "id";
		public static final String message = "message";
		public static final String rewards = "rewards";
		public static final String replies = "replies";
		public static final String switchToNPC = "switchToNPC";
	}

	public static final class Reply {
		public static final String text = "text";
		public static final String nextPhraseID = "nextPhraseID";
		public static final String requires = "requires";
	}

	public static final class ReplyRequires {
		public static final String requireType = "requireType";
		public static final String requireID = "requireID";
		public static final String value = "value";
		public static final String negate = "negate";
	}

	public static final class PhraseReward {
		public static final String rewardType = "rewardType";
		public static final String rewardID = "rewardID";
		public static final String value = "value";
		public static final String mapName = "mapName";
	}

	public static final class Quest {
		public static final String questID = "id";
		public static final String name = "name";
		public static final String showInLog = "showInLog";
		public static final String stages = "stages";
	}

	public static final class QuestLogEntry {
		public static final String progress = "progress";
		public static final String logText = "logText";
		public static final String rewardExperience = "rewardExperience";
		public static final String finishesQuest = "finishesQuest";
	}

	public static final class Monster {
		public static final String monsterTypeID = "id";
		public static final String iconID = "iconID";
		public static final String horizontalFlipChance = "horizontalFlipChance";
		public static final String name = "name";
		public static final String spawnGroup = "spawnGroup";
		public static final String monsterClass = "monsterClass";
		public static final String movementAggressionType = "movementAggressionType";
		public static final String unique = "unique";
		public static final String faction = "faction";
		public static final String maxHP = "maxHP";
		public static final String maxAP = "maxAP";
		public static final String moveCost = "moveCost";
		public static final String attackCost = "attackCost";
		public static final String attackChance = "attackChance";
		public static final String criticalSkill = "criticalSkill";
		public static final String criticalMultiplier = "criticalMultiplier";
		public static final String attackDamage = "attackDamage";
		public static final String blockChance = "blockChance";
		public static final String damageResistance = "damageResistance";
		public static final String droplistID = "droplistID";
		public static final String phraseID = "phraseID";
		public static final String hitEffect = "hitEffect";
		public static final String hitReceivedEffect = "hitReceivedEffect";
		public static final String deathEffect = "deathEffect";
	}

	public static final class ItemTraits_OnUse {
		public static final String increaseCurrentHP = "increaseCurrentHP";
		public static final String increaseCurrentAP = "increaseCurrentAP";
		public static final String conditionsSource = "conditionsSource";
		public static final String conditionsTarget = "conditionsTarget";
	}
	
	public static final class ItemTraits_OnHitReceived {
		public static final String increaseCurrentHP = "increaseCurrentHP";
		public static final String increaseCurrentAP = "increaseCurrentAP";
		public static final String increaseAttackerCurrentHP = "increaseAttackerCurrentHP";
		public static final String increaseAttackerCurrentAP = "increaseAttackerCurrentAP";
		public static final String conditionsSource = "conditionsSource";
		public static final String conditionsTarget = "conditionsTarget";
	}

	public static final class ActorConditionEffect {
		public static final String condition = "condition";
		public static final String magnitude = "magnitude";
		public static final String duration = "duration";
		public static final String chance = "chance";
	}

	public static final class ItemTraits_OnEquip {
		public static final String addedConditions = "addedConditions";
	}

	public static final class ItemType {
		public static final String itemTypeID = "id";
		public static final String iconID = "iconID";
		public static final String name = "name";
		public static final String description = "description";
		public static final String category = "category";
		public static final String displaytype = "displaytype";
		public static final String hasManualPrice = "hasManualPrice";
		public static final String baseMarketCost = "baseMarketCost";
		public static final String equipEffect = "equipEffect";
		public static final String useEffect = "useEffect";
		public static final String hitEffect = "hitEffect";
		public static final String missEffect = "missEffect";
		public static final String killEffect = "killEffect";
		public static final String hitReceivedEffect = "hitReceivedEffect";
		public static final String missReceivedEffect = "missReceivedEffect";
	}

	public static final class ItemFilter {
		public static final String itemFilterID = "id";
		public static final String include = "include";
	}
}
