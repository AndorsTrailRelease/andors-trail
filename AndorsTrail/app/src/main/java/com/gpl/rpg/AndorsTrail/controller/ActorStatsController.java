package com.gpl.rpg.AndorsTrail.controller;

import java.util.ArrayList;
import java.util.List;

import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.listeners.ActorConditionListeners;
import com.gpl.rpg.AndorsTrail.controller.listeners.ActorStatsListeners;
import com.gpl.rpg.AndorsTrail.controller.listeners.CombatActionListeners;
import com.gpl.rpg.AndorsTrail.controller.listeners.PlayerStatsListeners;
import com.gpl.rpg.AndorsTrail.model.ability.ActorCondition;
import com.gpl.rpg.AndorsTrail.model.ability.ActorConditionEffect;
import com.gpl.rpg.AndorsTrail.model.ability.ActorConditionType;
import com.gpl.rpg.AndorsTrail.model.ability.SkillCollection;
import com.gpl.rpg.AndorsTrail.model.ability.traits.AbilityModifierTraits;
import com.gpl.rpg.AndorsTrail.model.ability.traits.StatsModifierTraits;
import com.gpl.rpg.AndorsTrail.model.actor.Actor;
import com.gpl.rpg.AndorsTrail.model.actor.Monster;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.model.item.Inventory;
import com.gpl.rpg.AndorsTrail.model.item.ItemTraits_OnEquip;
import com.gpl.rpg.AndorsTrail.model.item.ItemTraits_OnHitReceived;
import com.gpl.rpg.AndorsTrail.model.item.ItemTraits_OnUse;
import com.gpl.rpg.AndorsTrail.model.item.ItemType;
import com.gpl.rpg.AndorsTrail.model.map.MonsterSpawnArea;
import com.gpl.rpg.AndorsTrail.model.map.PredefinedMap;
import com.gpl.rpg.AndorsTrail.resource.VisualEffectCollection;

public final class ActorStatsController {
	private final ControllerContext controllers;
	private final WorldContext world;
	public final ActorConditionListeners actorConditionListeners = new ActorConditionListeners();
	public final ActorStatsListeners actorStatsListeners = new ActorStatsListeners();
	public final PlayerStatsListeners playerStatsListeners = new PlayerStatsListeners();
	public final CombatActionListeners combatActionListeners = new CombatActionListeners();

	public ActorStatsController(ControllerContext controllers, WorldContext world) {
		this.controllers = controllers;
		this.world = world;
	}

	public void addConditionsFromEquippedItem(Player player, ItemType itemType) {
		ItemTraits_OnEquip equipEffects = itemType.effects_equip;
		if (equipEffects == null) return;
		if (equipEffects.addedConditions == null) return;
		for (ActorConditionEffect e : equipEffects.addedConditions) {
			applyActorCondition(player, e, ActorCondition.DURATION_FOREVER);
		}
	}
	public void removeConditionsFromUnequippedItem(Player player, ItemType itemType) {
		ItemTraits_OnEquip equipEffects = itemType.effects_equip;
		if (equipEffects == null) return;
		if (equipEffects.addedConditions == null) return;
		for (ActorConditionEffect e : equipEffects.addedConditions) {
			if (e.isImmunity()) {
				removeActorConditionImmunity(player, e.conditionType, e.magnitude, ActorCondition.DURATION_FOREVER);
			}
			if (e.isRemovalEffect()) continue;
			if (e.magnitude <= 0) continue;
			if (e.conditionType.isStacking) {
				removeStackableActorCondition(player, e.conditionType, e.magnitude, ActorCondition.DURATION_FOREVER);
			} else {
				removeNonStackableActorCondition(player, e.conditionType, e.magnitude, ActorCondition.DURATION_FOREVER);
			}
		}
	}

	private void removeStackableActorCondition(Actor actor, ActorConditionType type, int magnitude, int duration) {
		for(int i = actor.conditions.size() - 1; i >= 0; --i) {
			ActorCondition c = actor.conditions.get(i);
			if (!type.conditionTypeID.equals(c.conditionType.conditionTypeID)) continue;
			if (c.duration != duration) continue;

			if (c.magnitude > magnitude) {
				c.magnitude -= magnitude;
				actorConditionListeners.onActorConditionMagnitudeChanged(actor, c);
			} else {
				actorConditionsRemove(actor, c, i);
			}
			break;
		}
	}

	private void removeNonStackableActorCondition(Player player, ActorConditionType type, int magnitude, int duration) {
		for (Inventory.WearSlot slot : Inventory.WearSlot.values()) {
			ItemType t = player.inventory.getItemTypeInWearSlot(slot);
			if (t == null) continue;

			ItemTraits_OnEquip equipEffects = t.effects_equip;
			if (equipEffects == null) continue;
			if (equipEffects.addedConditions == null) continue;
			for (ActorConditionEffect e : equipEffects.addedConditions) {
				if (!e.conditionType.conditionTypeID.equals(type.conditionTypeID)) continue;
				if (e.duration != duration) continue;
				// The player is wearing some other item that gives this condition. It will not be removed now.
				return;
			}
		}
		removeStackableActorCondition(player, type, magnitude, duration);
	}
	

	private void removeActorConditionImmunity(Actor actor, ActorConditionType type, int magnitude, int duration) {
		Player player = actor instanceof Player ? (Player) actor : null;
		if (player != null) {
			for (Inventory.WearSlot slot : Inventory.WearSlot.values()) {
				ItemType t = player.inventory.getItemTypeInWearSlot(slot);
				if (t == null) continue;

				ItemTraits_OnEquip equipEffects = t.effects_equip;
				if (equipEffects == null) continue;
				if (equipEffects.addedConditions == null) continue;
				for (ActorConditionEffect e : equipEffects.addedConditions) {
					if (!e.conditionType.conditionTypeID.equals(type.conditionTypeID)) continue;
					if (!e.isImmunity()) continue;
					if (e.duration != duration) continue;
					// The player is wearing some other item that gives this immunity. It will not be removed now.
					return;
				}
			}
		}
		for(int i = actor.immunities.size() - 1; i >= 0; --i) {
			ActorCondition c = actor.immunities.get(i);
			if (!type.conditionTypeID.equals(c.conditionType.conditionTypeID)) continue;
			if (c.duration != duration) continue;

			actor.immunities.remove(i);
			actorConditionListeners.onActorConditionImmunityRemoved(actor, c);
			break;
		}
		if (player == null) return;

		//Looking for still-equipped items that would reapply this actor condition.
		List<ActorConditionEffect> toReapply = new ArrayList<ActorConditionEffect>();
		for (Inventory.WearSlot slot : Inventory.WearSlot.values()) {
			ItemType t = player.inventory.getItemTypeInWearSlot(slot);
			if (t == null) continue;

			ItemTraits_OnEquip equipEffects = t.effects_equip;
			if (equipEffects == null) continue;
			if (equipEffects.addedConditions == null) continue;
			for (ActorConditionEffect e : equipEffects.addedConditions) {
				if (!e.conditionType.conditionTypeID.equals(type.conditionTypeID)) continue;
				//There's another immunity (a temporary one for example) active. No nned to keep looking.
				if (e.isImmunity()) return;
				// The player is wearing some other item that gives this formerly immune actor condition
				toReapply.add(e);
			}
		}
		for (ActorConditionEffect e : toReapply) {
			applyActorCondition(player, e, ActorCondition.DURATION_FOREVER);
		}
	}	

	public void applyActorCondition(Actor actor, ActorConditionEffect e) { applyActorCondition(actor, e, e.duration); }
	private void applyActorCondition(Actor actor, ActorConditionEffect e, int duration) {
		if (e.isImmunity()) {
			removeAllConditionsOfType(actor, e.conditionType.conditionTypeID);
			addActorConditionImmunity(actor, e, duration);
		} else if (e.isRemovalEffect()) {
			removeAllConditionsOfType(actor, e.conditionType.conditionTypeID);
		} else if (e.magnitude > 0) {
			boolean immune = false;
			for (ActorCondition immunity : actor.immunities) {
				if (e.conditionType.conditionTypeID.equals(immunity.conditionType.conditionTypeID)) {
					immune = true;
				}
			}
			if (!immune) {
				if (e.conditionType.isStacking) {
					addStackableActorCondition(actor, e, duration);
				} else {
					addNonStackableActorCondition(actor, e, duration);
				}
			}
		}
		recalculateActorCombatTraits(actor);
	}

	private void addStackableActorCondition(Actor actor, ActorConditionEffect e, int duration) {
		final ActorConditionType type = e.conditionType;
		int magnitude = e.magnitude;

		for(int i = actor.conditions.size() - 1; i >= 0; --i) {
			ActorCondition c = actor.conditions.get(i);
			if (!type.conditionTypeID.equals(c.conditionType.conditionTypeID)) continue;
			if (c.duration == duration) {
				// If the actor already has a condition of this type and the same duration, just increase the magnitude instead.
				c.magnitude += magnitude;
				actorConditionListeners.onActorConditionMagnitudeChanged(actor, c);
				return;
			}
		}
		ActorCondition c = new ActorCondition(type, magnitude, duration);
		actor.conditions.add(c);
		actorConditionListeners.onActorConditionAdded(actor, c);
	}
	private void addNonStackableActorCondition(Actor actor, ActorConditionEffect e, int duration) {
		final ActorConditionType type = e.conditionType;

		for(int i = actor.conditions.size() - 1; i >= 0; --i) {
			ActorCondition c = actor.conditions.get(i);
			if (!type.conditionTypeID.equals(c.conditionType.conditionTypeID)) continue;
			if (c.magnitude > e.magnitude) return;
			if (c.magnitude == e.magnitude) {
				if (c.duration >= duration) return;
			}
			// If the actor already has this condition, but of a lower magnitude, we remove the old one and add this higher magnitude.
			actor.conditions.remove(i);
			actorConditionListeners.onActorConditionRemoved(actor, c);
		}

		ActorCondition c = e.createCondition(duration);
		actor.conditions.add(c);
		actorConditionListeners.onActorConditionAdded(actor, c);
	}
	
	private void addActorConditionImmunity(Actor actor, ActorConditionEffect e, int duration) {
		final ActorConditionType type = e.conditionType;

		for(int i = actor.immunities.size() - 1; i >= 0; --i) {
			ActorCondition c = actor.immunities.get(i);
			if (!type.conditionTypeID.equals(c.conditionType.conditionTypeID)) continue;
			if (c.duration >= duration) return;
			// If the actor already has this immunity, but of a shorter duration, we replace the old immunity by the new, longer one.
			actor.immunities.remove(i);
			actorConditionListeners.onActorConditionRemoved(actor, c);
		}

		ActorCondition c = e.createCondition(duration);
		actor.immunities.add(c);
		actorConditionListeners.onActorConditionImmunityAdded(actor, c);
	}

	public void removeAllTemporaryConditions(final Actor actor) {
		for(int i = actor.conditions.size() - 1; i >= 0; --i) {
			ActorCondition c = actor.conditions.get(i);
			if (!c.isTemporaryEffect() && !c.isDurationForeverUntilSleep())  continue;
			actorConditionsRemove(actor, c, i);
		}
		for(int i = actor.immunities.size() - 1; i >= 0; --i) {
			ActorCondition c = actor.immunities.get(i);
			if (!c.isTemporaryEffect() && !c.isDurationForeverUntilSleep()) continue;
			removeActorConditionImmunity(actor, c.conditionType, c.magnitude, c.duration);
		}
	}

	private void removeAllConditionsOfType(final Actor actor, final String conditionTypeID) {
		for(int i = actor.conditions.size() - 1; i >= 0; --i) {
			ActorCondition c = actor.conditions.get(i);
			if (!c.conditionType.conditionTypeID.equals(conditionTypeID)) continue;
			actorConditionsRemove(actor, c, i);
		}
	}

	private void applyEffectsFromCurrentConditions(Actor actor) {
		for (ActorCondition c : actor.conditions) {
			applyAbilityEffects(actor, c.conditionType.abilityEffect, c.magnitude);
		}
	}

	public void applyAbilityEffects(Actor actor, AbilityModifierTraits effects, int multiplier) {
		if (effects == null) return;

		addActorMaxHealth(actor, effects.increaseMaxHP * multiplier, false);
		addActorMaxAP(actor, effects.increaseMaxAP * multiplier, false);

		addActorMoveCost(actor, effects.increaseMoveCost * multiplier);
		addActorAttackCost(actor, effects.increaseAttackCost * multiplier);
		if (actor.isPlayer) {
			addPlayerReequipCost((Player)actor, effects.increaseReequipCost * multiplier);
			addPlayerUseCost((Player)actor, effects.increaseUseItemCost * multiplier);
		}
		//criticalMultiplier should not be increased. It is always defined by the weapon in use.
		actor.attackChance += effects.increaseAttackChance * multiplier;
		actor.criticalSkill += effects.increaseCriticalSkill * multiplier;
		actor.damagePotential.add(effects.increaseMinDamage * multiplier, true);
		actor.damagePotential.addToMax(effects.increaseMaxDamage * multiplier);
		actor.blockChance += effects.increaseBlockChance * multiplier;
		actor.damageResistance += effects.increaseDamageResistance * multiplier;
	}

	public void recalculatePlayerStats(Player player) {
		player.weaponDamage.set(0,0);
		player.resetStatsToBaseTraits();
		player.recalculateLevelExperience();
		controllers.itemController.applyInventoryEffects(player);
		controllers.skillController.applySkillEffects(player);
		applyEffectsFromCurrentConditions(player);
		ItemController.recalculateHitEffectsFromWornItems(player);
		ItemController.applyDamageModifier(player);
		capActorHealthAtMax(player);
		capActorAPAtMax(player);
		lowCapActorAttackChance(player);
		lowCapActorDamagePotential(player);
	}
	public void recalculateMonsterCombatTraits(Monster monster) {
		monster.resetStatsToBaseTraits();
		applyEffectsFromCurrentConditions(monster);
		capActorHealthAtMax(monster);
		capActorAPAtMax(monster);
		lowCapActorAttackChance(monster);
		lowCapActorDamagePotential(monster);
	}

	private void lowCapActorAttackChance(Actor actor) {
		if (actor.attackChance < 0) actor.attackChance = 0;
	}

	private void lowCapActorDamagePotential(Actor actor) {
		if (actor.damagePotential.max < 0) actor.damagePotential.set(0, 0);
	}

	private void recalculateActorCombatTraits(Actor actor) {
		if (actor.isPlayer) recalculatePlayerStats((Player) actor);
		else recalculateMonsterCombatTraits((Monster) actor);
	}

	public void applyConditionsToPlayer(Player player, boolean isFullRound) {
		if (player.conditions.isEmpty() && player.immunities.isEmpty()) return;
		if (!isFullRound) removeConditionsFromSkillEffects(player);

		applyStatsEffects(player, isFullRound);
		if (player.isDead()) {
			controllers.mapController.handlePlayerDeath();
			return;
		}

		if (!isFullRound) decreaseDurationAndRemoveConditions(player);
	}

	private void removeConditionsFromSkillEffects(Player player) {
		if (SkillController.rollForSkillChance(player, SkillCollection.SkillID.rejuvenation, SkillCollection.PER_SKILLPOINT_INCREASE_REJUVENATION_CHANCE)) {
			int i = getRandomConditionForRejuvenate(player);
			if (i >= 0) {
				ActorCondition c = player.conditions.get(i);
				if (c.magnitude > 1) {
					c.magnitude -= 1;
					actorConditionListeners.onActorConditionMagnitudeChanged(player, c);
				} else {
					player.conditions.remove(i);
					actorConditionListeners.onActorConditionRemoved(player, c);
				}
				recalculatePlayerStats(player);
			}
		}
	}

	private static int getRandomConditionForRejuvenate(Player player) {
		int i = -1;
		int count = 0;
		int potentialConditions[] = new int[player.conditions.size()];
		for (ActorCondition c : player.conditions) {
			i++;

			if (!c.isTemporaryEffect())
				continue;
			if (c.conditionType.isPositive)
				continue;
			if (c.conditionType.conditionCategory == ActorConditionType.ConditionCategory.spiritual)
				continue;

			potentialConditions[count++] = i;
		}

		if (count == 0)
			return -1;

		return potentialConditions[Constants.rnd.nextInt(count)];
	}

	public void applyConditionsToMonsters(PredefinedMap map, boolean isFullRound) {
		for (MonsterSpawnArea a : map.spawnAreas) {
			// Iterate the array backwards, since monsters may get removed from the array inside applyConditionsToMonster.
			for (int i = a.monsters.size()-1; i >= 0; --i) {
				final Monster m = a.monsters.get(i);
				applyConditionsToMonster(m, isFullRound);
			}
		}
	}

	private void applyConditionsToMonster(Monster monster, boolean isFullRound) {
		if (monster.conditions.isEmpty()) return;
		applyStatsEffects(monster, isFullRound);
		if (monster.isDead()) {
			controllers.combatController.playerKilledMonster(monster);
			return;
		}

		if (!isFullRound) decreaseDurationAndRemoveConditions(monster);
	}

	private void applyStatsEffects(Actor actor, boolean isFullRound) {
		// Apply negative effects before positive effects
		for (ActorCondition c : actor.conditions) {
			if (!c.conditionType.isPositive) applyStatsEffects(actor, isFullRound, c);
		}
		for (ActorCondition c : actor.conditions) {
			if (c.conditionType.isPositive) applyStatsEffects(actor, isFullRound, c);
		}
		controllers.effectController.startEnqueuedEffect(actor.position);
	}
	private void applyStatsEffects(Actor actor, boolean isFullRound, ActorCondition c) {
		StatsModifierTraits effect = isFullRound ? c.conditionType.statsEffect_everyFullRound : c.conditionType.statsEffect_everyRound;
		boolean hasEffect = applyStatsModifierEffect(actor, effect, c.magnitude);
		if (hasEffect) actorConditionListeners.onActorConditionRoundEffectApplied(actor, c);
	}

	private void decreaseDurationAndRemoveConditions(Actor actor) {
		boolean removedAnyConditions = false;
		for(int i = actor.conditions.size() - 1; i >= 0; --i) {
			ActorCondition c = actor.conditions.get(i);
			if (!c.isTemporaryEffect()) continue;
			if (c.duration <= 1) {
				actorConditionsRemove(actor, c, i);
				removedAnyConditions = true;
			} else {
				c.duration -= 1;
				actorConditionListeners.onActorConditionDurationChanged(actor, c);
			}
		}
		// Immunities
		for(int i = actor.immunities.size() - 1; i >= 0; --i) {
			ActorCondition c = actor.immunities.get(i);
			if (!c.isTemporaryEffect()) continue;
			if (c.duration <= 1) {
				actor.immunities.remove(i);
				actorConditionListeners.onActorConditionImmunityRemoved(actor, c);
				removedAnyConditions = true;
			} else {
				c.duration -= 1;
				actorConditionListeners.onActorConditionImmunityDurationChanged(actor, c);
			}
			if (actor instanceof Player) {
				Player player = (Player) actor;
				//Looking for still-equipped items that would reapply this actor condition.
				List<ActorConditionEffect> toReapply = new ArrayList<ActorConditionEffect>();
				for (Inventory.WearSlot slot : Inventory.WearSlot.values()) {
					ItemType t = player.inventory.getItemTypeInWearSlot(slot);
					if (t == null) continue;

					ItemTraits_OnEquip equipEffects = t.effects_equip;
					if (equipEffects == null) continue;
					if (equipEffects.addedConditions == null) continue;
					for (ActorConditionEffect e : equipEffects.addedConditions) {
						if (!e.conditionType.conditionTypeID.equals(c.conditionType.conditionTypeID)) continue;
						//There's another immunity (a temporary one for example) active. No need to keep looking.
						if (e.isImmunity()) {
							toReapply.clear();
							break;
						}
						// The player is wearing some other item that gives this formerly immune actor condition
						toReapply.add(e);
					}
				}
				for (ActorConditionEffect e : toReapply) {
					applyActorCondition(player, e, ActorCondition.DURATION_FOREVER);
				}
			}
		}
		if (removedAnyConditions) {
			recalculateActorCombatTraits(actor);
		}
	}

	int actorConditionsRemove(Actor actor, ActorCondition c, int i) {
		int magnitude = 0;	//default: No condition from worn items

		if (actor instanceof Player) {
			Player player = (Player) actor;
			magnitude = gotConditionFromWornItem(player, c);
			if (magnitude > 0) {			//condition from worn items?
				c.magnitude = magnitude;	// -> readd condition
				actorConditionListeners.onActorConditionMagnitudeChanged(actor, c);
				c.duration = ActorCondition.DURATION_FOREVER;
				actorConditionListeners.onActorConditionDurationChanged(actor, c);
			}
		}
		if (magnitude == 0) {
			actor.conditions.remove(i);
			actorConditionListeners.onActorConditionRemoved(actor, c);
		}

		return magnitude;
	}

	int gotConditionFromWornItem(Player player, ActorCondition c) {
		int magnitude = 0;	//Default: No worn item with this condition

		for (Inventory.WearSlot slot : Inventory.WearSlot.values()) {
			ItemType t = player.inventory.getItemTypeInWearSlot(slot);
			if (t == null) continue;
			ItemTraits_OnEquip equipEffects = t.effects_equip;
			if (equipEffects == null) continue;
			if (equipEffects.addedConditions == null) continue;
			for (ActorConditionEffect e : equipEffects.addedConditions) {
				if (e.conditionType.conditionTypeID.equals(c.conditionType.conditionTypeID)) {
					if (e.magnitude == ActorCondition.MAGNITUDE_REMOVE_ALL) {
						return 0;	//On an item-based immunity the result is always 0
					}
					if (magnitude < e.magnitude) {
						magnitude = e.magnitude;
					}
				}
			}
		}
		return magnitude;
	}

	public void applyUseEffect(Actor source, Actor target, ItemTraits_OnUse effect) {
		if (effect == null) return;

		if (effect.addedConditions_source != null) {
			for (ActorConditionEffect e : effect.addedConditions_source) {
				rollForConditionEffect(source, e);
			}
		}
		if (target != null) {
			if (effect.addedConditions_target != null) {
				for (ActorConditionEffect e : effect.addedConditions_target) {
					rollForConditionEffect(target, e);
				}
			}
		}
		if (effect.changedStats != null) {
			applyStatsModifierEffect(source, effect.changedStats, 1);
			controllers.effectController.startEnqueuedEffect(source.position);
		}
	}
	
	public void applyHitReceivedEffect(Actor source, Actor target, ItemTraits_OnHitReceived effect) {
		applyUseEffect(source, target, effect);
		if (effect.changedStats_target != null) {
			applyStatsModifierEffect(target, effect.changedStats, 1);
			controllers.effectController.startEnqueuedEffect(target.position);
		}
	}

	private void rollForConditionEffect(Actor actor, ActorConditionEffect conditionEffect) {
		int chanceRollBias = 0;
		if (actor.isPlayer) chanceRollBias = SkillController.getActorConditionEffectChanceRollBias(conditionEffect, (Player) actor);

		if (!Constants.rollResult(conditionEffect.chance, chanceRollBias)) return;
		if (actor.isPlayer) combatActionListeners.onPlayerReceviesActorCondition(conditionEffect);
		else combatActionListeners.onMonsterReceivesActorCondition(conditionEffect, (Monster)actor);
		applyActorCondition(actor, conditionEffect);
	}

	private boolean applyStatsModifierEffect(Actor actor, StatsModifierTraits effect, int magnitude) {
		if (effect == null) return false;

		boolean hasUpdatedStats = false;
		if (effect.currentAPBoost != null) {
			int effectValue = Constants.rollValue(effect.currentAPBoost) * magnitude;
			boolean changed = changeActorAP(actor, effectValue, false, false);
			if (changed) {
				VisualEffectCollection.VisualEffectID visualEffectID = effect.visualEffectID;
				if (visualEffectID == null) {
					visualEffectID = VisualEffectCollection.VisualEffectID.blueSwirl;
				}
				controllers.effectController.enqueueEffect(visualEffectID, effectValue);
				hasUpdatedStats = true;
			}
		}
		if (effect.currentHPBoost != null) {
			int effectValue = Constants.rollValue(effect.currentHPBoost) * magnitude;
			boolean changed = changeActorHealth(actor, effectValue, true, false);
			if (changed) {
				VisualEffectCollection.VisualEffectID visualEffectID = effect.visualEffectID;
				if (visualEffectID == null) {
					if (effectValue > 0) {
						visualEffectID = VisualEffectCollection.VisualEffectID.blueSwirl;
					} else {
						visualEffectID = VisualEffectCollection.VisualEffectID.redSplash;
					}
				}
				controllers.effectController.enqueueEffect(visualEffectID, effectValue);
				hasUpdatedStats = true;
			}
		}
		return hasUpdatedStats;
	}

	public void applyKillEffectsToPlayer(Player player) {
		for (Inventory.WearSlot slot : Inventory.WearSlot.values()) {
			ItemType type = player.inventory.getItemTypeInWearSlot(slot);
			if (type == null) continue;

			applyUseEffect(player, null, type.effects_kill);
		}
	}
	
	public void applyOnDeathEffectsToPlayer(Player player, Actor monster) {
		ItemTraits_OnUse onDeathEffect = monster.getOnDeathEffects();
		if (onDeathEffect == null) return;
		
		applyUseEffect(player, null, onDeathEffect);
	}

	public void applySkillEffectsForNewRound(Player player, PredefinedMap currentMap) {
		int level = player.getSkillLevel(SkillCollection.SkillID.regeneration);
		if (level > 0) {
			boolean hasAdjacentMonster = MovementController.hasAdjacentAggressiveMonster(currentMap, player);
			if (!hasAdjacentMonster) {
				addActorHealth(player, level * SkillCollection.PER_SKILLPOINT_INCREASE_REGENERATION);
			}
		}
	}

	public void addPlayerWeaponDamage(Player player, int increaseMinDamage, int increaseMaxDamage) {
		player.weaponDamage.add(increaseMinDamage, true);
		player.weaponDamage.addToMax(increaseMaxDamage);
	}

	public static enum LevelUpSelection {
		health
		,attackChance
		,attackDamage
		,blockChance
	}

	public void addLevelupEffect(Player player, LevelUpSelection selectionID) {
		int hpIncrease = 0;
		switch (selectionID) {
		case health:
			hpIncrease = Constants.LEVELUP_EFFECT_HEALTH;
			break;
		case attackChance:
			player.baseTraits.attackChance += Constants.LEVELUP_EFFECT_ATK_CH;
			break;
		case attackDamage:
			player.baseTraits.damagePotential.max += Constants.LEVELUP_EFFECT_ATK_DMG;
			player.baseTraits.damagePotential.current += Constants.LEVELUP_EFFECT_ATK_DMG;
			break;
		case blockChance:
			player.baseTraits.blockChance += Constants.LEVELUP_EFFECT_DEF_CH;
			break;
		}
		if (player.nextLevelAddsNewSkillpoint()) {
			player.availableSkillIncreases++;
		}
		player.level++;

		hpIncrease += player.getSkillLevel(SkillCollection.SkillID.fortitude) * SkillCollection.PER_SKILLPOINT_INCREASE_FORTITUDE_HEALTH;
		addActorMaxHealth(player, hpIncrease, true);
		player.baseTraits.maxHP += hpIncrease;

		recalculatePlayerStats(player);
	}

	public void healAllMonsters(MonsterSpawnArea area) {
		for (Monster m : area.monsters) {
			removeAllTemporaryConditions(m);
			setActorMaxHealth(m);
		}
	}

	public void addExperience(int exp) {
		if (exp == 0) return;
		Player p = world.model.player;
		p.totalExperience += exp;
		p.levelExperience.add(exp, true);
		playerStatsListeners.onPlayerExperienceChanged(p);
	}
	public void addActorMoveCost(Actor actor, int amount) {
		if (amount == 0) return;
		actor.moveCost += amount;
		if (actor.moveCost <= 0) actor.moveCost = 1;
		actorStatsListeners.onActorMoveCostChanged(actor, actor.moveCost);
	}
	public void addActorAttackCost(Actor actor, int amount) {
		if (amount == 0) return;
		actor.attackCost += amount;
		if (actor.attackCost <= 0) actor.attackCost = 1;
		actorStatsListeners.onActorAttackCostChanged(actor, actor.attackCost);
	}
	public void addPlayerReequipCost(Player player, int amount) {
		if (amount == 0) return;
		player.reequipCost += amount;
		if (player.reequipCost < 0) player.reequipCost = 0;
		actorStatsListeners.onPlayerReequipCostChanged(player, player.reequipCost);
	}
	public void addPlayerUseCost(Player player, int amount) {
		if (amount == 0) return;
		player.useItemCost += amount;
		if (player.useItemCost < 0) player.useItemCost = 0;
		actorStatsListeners.onPlayerUseCostChanged(player, player.useItemCost);
	}

	public void setActorMaxHealth(Actor actor) {
		if (actor.health.isMax()) return;
		actor.health.setMax();
		actorStatsListeners.onActorHealthChanged(actor);
	}
	public void capActorHealthAtMax(Actor actor) {
		if (actor.health.capAtMax()) actorStatsListeners.onActorHealthChanged(actor);
	}
	public boolean addActorHealth(Actor actor, int amount) { return changeActorHealth(actor, amount, false, false); }
	public boolean removeActorHealth(Actor actor, int amount) { return changeActorHealth(actor, -amount, false, false); }
	public boolean changeActorHealth(Actor actor, int deltaAmount, boolean mayUnderflow, boolean mayOverflow) {
		final boolean changed = actor.health.change(deltaAmount, mayUnderflow, mayOverflow);
		if(changed) actorStatsListeners.onActorHealthChanged(actor);
		return changed;
	}
	public void addActorMaxHealth(Actor actor, int amount, boolean affectCurrentHealth) {
		if (amount == 0) return;
		actor.health.addToMax(amount);
		if (affectCurrentHealth) actor.health.add(amount, false);
		actorStatsListeners.onActorHealthChanged(actor);
	}

	public void setActorMaxAP(Actor actor) {
		if (actor.ap.isMax()) return;
		actor.ap.setMax();
		actorStatsListeners.onActorAPChanged(actor);
	}
	public void capActorAPAtMax(Actor actor) {
		if (actor.ap.capAtMax()) actorStatsListeners.onActorAPChanged(actor);
	}
	public boolean addActorAP(Actor actor, int amount) { return changeActorAP(actor, amount, false, false); }
	public boolean changeActorAP(Actor actor, int deltaAmount, boolean mayUnderflow, boolean mayOverflow) {
		final boolean changed = actor.ap.change(deltaAmount, mayUnderflow, mayOverflow);
		if(changed) actorStatsListeners.onActorAPChanged(actor);
		return changed;
	}
	public boolean useAPs(Actor actor, int cost) {
		if (actor.ap.current < cost) return false;
		actor.ap.subtract(cost, false);
		actorStatsListeners.onActorAPChanged(actor);
		return true;
	}
	public void addActorMaxAP(Actor actor, int amount, boolean affectCurrentAP) {
		if (amount == 0) return;
		actor.ap.addToMax(amount);
		if (affectCurrentAP) actor.ap.add(amount, false);
		actorStatsListeners.onActorAPChanged(actor);
	}
	public void setActorMinAP(Actor actor) {
		if (actor.ap.current == 0) return;
		actor.ap.current = 0;
		actorStatsListeners.onActorAPChanged(actor);
	}
}
