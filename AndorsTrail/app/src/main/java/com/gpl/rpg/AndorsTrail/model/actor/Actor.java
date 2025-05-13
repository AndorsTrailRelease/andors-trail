package com.gpl.rpg.AndorsTrail.model.actor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gpl.rpg.AndorsTrail.model.ability.ActorCondition;
import com.gpl.rpg.AndorsTrail.model.item.ItemTraits_OnHitReceived;
import com.gpl.rpg.AndorsTrail.model.item.ItemTraits_OnUse;
import com.gpl.rpg.AndorsTrail.util.Coord;
import com.gpl.rpg.AndorsTrail.util.CoordRect;
import com.gpl.rpg.AndorsTrail.util.Range;
import com.gpl.rpg.AndorsTrail.util.Size;

public class Actor {
	public int iconID;
	public final Size tileSize;
	public final Coord position = new Coord();
	public final CoordRect rectPosition;
	public final boolean isPlayer;
	private final boolean isImmuneToCriticalHits;
	protected String name;

	// TODO: Should be privates
	public final Range ap = new Range();
	public final Range health = new Range();
	public final ArrayList<ActorCondition> conditions = new ArrayList<ActorCondition>();
	public final ArrayList<ActorCondition> immunities = new ArrayList<ActorCondition>();
	public int moveCost;
	public int attackCost;
	public int attackChance;
	public int criticalSkill;
	public float criticalMultiplier;
	public final Range damagePotential = new Range();
	public int blockChance;
	public int damageResistance;
	public ItemTraits_OnUse[] onHitEffects;
	public ItemTraits_OnUse[] onMissEffects;
	public ItemTraits_OnHitReceived[] onHitReceivedEffects;
	public ItemTraits_OnHitReceived[] onMissReceivedEffects;
	public ItemTraits_OnUse onDeathEffects;
	public boolean hasVFXRunning = false;
	public long vfxStartTime = 0;
	public int vfxDuration = 0;
	public final Coord lastPosition = new Coord();

	public Actor(
			Size tileSize
			, boolean isPlayer
			, boolean isImmuneToCriticalHits
	) {
		this.tileSize = tileSize;
		this.rectPosition = new CoordRect(this.position, this.tileSize);
		this.isPlayer = isPlayer;
		this.isImmuneToCriticalHits = isImmuneToCriticalHits;
	}

	public boolean isImmuneToCriticalHits() { return isImmuneToCriticalHits; }
	public String getName() { return name; }
	public int getCurrentAP() { return ap.current; }
	public int getMaxAP() { return ap.max; }
	public int getCurrentHP() { return health.current; }
	public int getMaxHP() { return health.max; }
	public int getMoveCost() { return moveCost; }
	public int getAttackCost() { return attackCost; }
	public int getAttackChance() { return attackChance; }
	public int getCriticalSkill() { return criticalSkill; }
	public float getCriticalMultiplier() { return criticalMultiplier; }
	public Range getDamagePotential() { return damagePotential; }
	public int getBlockChance() { return blockChance; }
	public int getDamageResistance() { return damageResistance; }
	public ItemTraits_OnUse[] getOnHitEffects() { return onHitEffects; }
	public List<ItemTraits_OnUse> getOnHitEffectsAsList() { return onHitEffects == null ? null : Arrays.asList(onHitEffects); }
	public ItemTraits_OnUse[] getOnMissEffects() { return onMissEffects; }
	public List<ItemTraits_OnUse> getOnMissEffectsAsList() { return onMissEffects == null ? null : Arrays.asList(onMissEffects); }
	public ItemTraits_OnHitReceived[] getOnHitReceivedEffects() { return onHitReceivedEffects; }
	public List<ItemTraits_OnHitReceived> getOnHitReceivedEffectsAsList() { return onHitReceivedEffects == null ? null : Arrays.asList(onHitReceivedEffects); }
	public ItemTraits_OnHitReceived[] getOnMissReceivedEffects() { return onMissReceivedEffects; }
	public List<ItemTraits_OnHitReceived> getOnMissReceivedEffectsAsList() { return onMissReceivedEffects == null ? null : Arrays.asList(onMissReceivedEffects); }
	public ItemTraits_OnUse getOnDeathEffects() { return onDeathEffects; }
	
	public boolean hasCriticalSkillEffect() { return getCriticalSkill() != 0; }
	public boolean hasCriticalMultiplierEffect() { float m = getCriticalMultiplier(); return m != 0 && m != 1; }
	public boolean hasCriticalAttacks() { return hasCriticalSkillEffect() && hasCriticalMultiplierEffect(); }

	public int getAttacksPerTurn() { return (int) Math.floor(getMaxAP() / getAttackCost()); }
	public int getEffectiveCriticalChance() { return getEffectiveCriticalChance(getCriticalSkill()); }
	public static int getEffectiveCriticalChance(int criticalSkill) {
		if (criticalSkill <= 0) return 0;
		int v = (int) (-5 + 2 * Math.sqrt(5*criticalSkill));
		if (v < 0) return 0;
		return v;
	}

	public boolean isDead() {
		return health.current <= 0;
	}

	public boolean hasAPs(int cost) {
		return ap.current >= cost;
	}

	public boolean hasCondition(final String conditionTypeID) {
		for (ActorCondition c : conditions) {
			if (c.conditionType.conditionTypeID.equals(conditionTypeID)) return true;
		}
		return false;
	}
}
