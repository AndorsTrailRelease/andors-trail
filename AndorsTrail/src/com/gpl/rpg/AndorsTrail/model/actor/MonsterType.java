package com.gpl.rpg.AndorsTrail.model.actor;

import com.gpl.rpg.AndorsTrail.model.item.DropList;
import com.gpl.rpg.AndorsTrail.model.item.ItemTraits_OnHitReceived;
import com.gpl.rpg.AndorsTrail.model.item.ItemTraits_OnUse;
import com.gpl.rpg.AndorsTrail.util.ConstRange;
import com.gpl.rpg.AndorsTrail.util.Size;

public final class MonsterType {
	public static enum MonsterClass {
		humanoid
		,insect
		,demon
		,construct
		,animal
		,giant
		,undead
		,reptile
		,ghost;

		public static MonsterClass fromString(String s, MonsterClass default_) {
			if (s == null) return default_;
			return valueOf(s);
		}
	}

	public final String id;
	public final String name;
	public final String spawnGroup;
	public final int exp;
	public final DropList dropList;
	public final String phraseID;
	public final boolean isUnique; // Unique monsters are not respawned.
	public final String faction;
	public final MonsterClass monsterClass;
	public final AggressionType aggressionType;

	public final Size tileSize;
	public final int iconID;
	public final int maxAP;
	public final int maxHP;
	public final int moveCost;
	public final int attackCost;
	public final int attackChance;
	public final int criticalSkill;
	public final float criticalMultiplier;
	public final ConstRange damagePotential;
	public final int blockChance;
	public final int damageResistance;
	public final ItemTraits_OnUse[] onHitEffects;
	public final ItemTraits_OnHitReceived[] onHitReceivedEffects;
	public final ItemTraits_OnUse onDeathEffects;

	public MonsterType(
			String id
			, String name
			, String spawnGroup
			, int exp
			, DropList dropList
			, String phraseID
			, boolean isUnique
			, String faction
			, MonsterClass monsterClass
			, AggressionType aggressionType
			, Size tileSize
			, int iconID
			, int maxAP
			, int maxHP
			, int moveCost
			, int attackCost
			, int attackChance
			, int criticalSkill
			, float criticalMultiplier
			, ConstRange damagePotential
			, int blockChance
			, int damageResistance
			, ItemTraits_OnUse[] onHitEffects
			, ItemTraits_OnHitReceived[] onHitReceivedEffects
			, ItemTraits_OnUse onDeathEffects
	) {
		this.id = id;
		this.name = name;
		this.spawnGroup = spawnGroup;
		this.exp = exp;
		this.dropList = dropList;
		this.phraseID = phraseID;
		this.faction = faction;
		this.isUnique = isUnique;
		this.monsterClass = monsterClass;
		this.aggressionType = aggressionType;
		this.tileSize = tileSize;
		this.iconID = iconID;
		this.maxAP = maxAP;
		this.maxHP = maxHP;
		this.moveCost = moveCost;
		this.attackCost = attackCost;
		this.attackChance = attackChance;
		this.criticalSkill = criticalSkill;
		this.criticalMultiplier = criticalMultiplier;
		this.damagePotential = damagePotential;
		this.blockChance = blockChance;
		this.damageResistance = damageResistance;
		this.onHitEffects = onHitEffects;
		this.onHitReceivedEffects = onHitReceivedEffects;
		this.onDeathEffects = onDeathEffects;
	}

	public static enum AggressionType {
		none
		,helpOthers		// Will move to help if the player attacks some other monster in the same spawn.
		,protectSpawn	// Will move to attack if the player stands inside the spawn.
		,wholeMap		// Will move to attack even outside its spawn area
		,flee           // Will try to flee from the player
		;

		public static AggressionType fromString(String s, AggressionType default_) {
			if (s == null) return default_;
			return valueOf(s);
		}
	}

	public boolean isImmuneToCriticalHits() {
		if (monsterClass == MonsterClass.ghost) return true;
		if (monsterClass == MonsterClass.construct) return true;
		if (monsterClass == MonsterClass.demon) return true;
		return false;
	}

	public boolean hasCombatStats() {
		if (attackCost != 10) return true;
		if (attackChance != 0) return true;
		if (criticalSkill != 0) return true;
		if (criticalMultiplier != 0) return true;
		if (damagePotential != null) return true;
		if (blockChance != 0) return true;
		if (damageResistance != 0) return true;
		return false;
	}
}
