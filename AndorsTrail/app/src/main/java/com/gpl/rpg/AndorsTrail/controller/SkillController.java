package com.gpl.rpg.AndorsTrail.controller;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.context.ControllerContext;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.listeners.CombatActionListeners;
import com.gpl.rpg.AndorsTrail.model.ability.ActorConditionEffect;
import com.gpl.rpg.AndorsTrail.model.ability.ActorConditionType;
import com.gpl.rpg.AndorsTrail.model.ability.SkillCollection;
import com.gpl.rpg.AndorsTrail.model.ability.SkillCollection.SkillID;
import com.gpl.rpg.AndorsTrail.model.ability.SkillInfo;
import com.gpl.rpg.AndorsTrail.model.actor.Actor;
import com.gpl.rpg.AndorsTrail.model.actor.Monster;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.model.item.DropList.DropItem;
import com.gpl.rpg.AndorsTrail.model.item.Inventory;
import com.gpl.rpg.AndorsTrail.model.item.ItemCategory;
import com.gpl.rpg.AndorsTrail.model.item.ItemType;
import com.gpl.rpg.AndorsTrail.model.item.ItemTypeCollection;
import com.gpl.rpg.AndorsTrail.util.ConstRange;
import com.gpl.rpg.AndorsTrail.util.L;

public final class SkillController {
	private final ControllerContext controllers;
	private final WorldContext world;
	public final CombatActionListeners combatActionListeners = new CombatActionListeners();
	

	public SkillController(ControllerContext controllers, WorldContext world) {
		this.controllers = controllers;
		this.world = world;
	}

	public void applySkillEffects(Player player) {
		player.attackChance += SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_CHANCE * player.getSkillLevel(SkillID.weaponChance);
		player.damagePotential.addToMax(SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_DAMAGE_MAX * player.getSkillLevel(SkillID.weaponDmg));
		player.damagePotential.add(SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_DAMAGE_MIN * player.getSkillLevel(SkillID.weaponDmg), false);
		player.blockChance += SkillCollection.PER_SKILLPOINT_INCREASE_DODGE * player.getSkillLevel(SkillID.dodge);
		player.damageResistance += SkillCollection.PER_SKILLPOINT_INCREASE_BARKSKIN * player.getSkillLevel(SkillID.barkSkin);
		if (player.hasCriticalSkillEffect()) {
			if (player.criticalSkill > 0) {
				player.criticalSkill += player.criticalSkill * SkillCollection.PER_SKILLPOINT_INCREASE_MORE_CRITICALS_PERCENT * player.getSkillLevel(SkillID.moreCriticals) / 100;
			}
		}
		if (player.hasCriticalMultiplierEffect()) {
			player.criticalMultiplier += player.criticalMultiplier * SkillCollection.PER_SKILLPOINT_INCREASE_BETTER_CRITICALS_PERCENT * player.getSkillLevel(SkillID.betterCriticals) / 100;
		}
		controllers.actorStatsController.addActorMaxAP(player, SkillCollection.PER_SKILLPOINT_INCREASE_SPEED * player.getSkillLevel(SkillID.speed), false);
		/*final int berserkLevel = player.getSkillLevel(Skills.SKILL_BERSERKER);
		if (berserkLevel > 0) {
			final int berserkHealth = player.health.max * Skills.BERSERKER_STARTS_AT_HEALTH_PERCENT / 100;
			if (player.health.current <= berserkHealth) {
				player.traits.attackChance += Skills.PER_SKILLPOINT_INCREASE_BERSERKER_WEAPON_CHANCE * berserkLevel;
				player.traits.damagePotential.addToMax(Skills.PER_SKILLPOINT_INCREASE_BERSERKER_WEAPON_DAMAGE_MAX * berserkLevel);
				player.traits.damagePotential.add(Skills.PER_SKILLPOINT_INCREASE_BERSERKER_WEAPON_DAMAGE_MIN * berserkLevel, false);
				player.traits.blockChance += Skills.PER_SKILLPOINT_INCREASE_BERSERKER_DODGE * berserkLevel;
			}
		}*/
	}

	public static int getDropChanceRollBias(DropItem item, ItemType itemType, Player player) {
		if (player == null) return 0;
		if(item.itemTypeID == null && AndorsTrailApplication.DEVELOPMENT_DEBUGMESSAGES){
			L.log("Item type missing: " + item + " " + player.id);
		}

		if (ItemTypeCollection.isGoldItemType(item.itemTypeID)) {
			return getRollBias(item, player, SkillID.coinfinder, SkillCollection.PER_SKILLPOINT_INCREASE_COINFINDER_CHANCE_PERCENT);
		} else if (!itemType.isOrdinaryItem()) {
			return getRollBias(item, player, SkillID.magicfinder, SkillCollection.PER_SKILLPOINT_INCREASE_MAGICFINDER_CHANCE_PERCENT);
		} else {
			return 0;
		}
	}

	public static int getDropQuantityRollBias(DropItem item, Player player) {
		if (player == null) return 0;
		if (!ItemTypeCollection.isGoldItemType(item.itemTypeID)) return 0;

		return getRollBias(item.quantity, player, SkillID.coinfinder, SkillCollection.PER_SKILLPOINT_INCREASE_COINFINDER_QUANTITY_PERCENT);
	}

	private static int getRollBias(DropItem item, Player player, SkillID skill, int perSkillpointIncrease) {
		return getRollBias(item.chance, player, skill, perSkillpointIncrease);
	}

	private static int getRollBias(ConstRange chance, Player player, SkillID skill, int perSkillpointIncrease) {
		int skillLevel = player.getSkillLevel(skill);
		if (skillLevel <= 0) return 0;
		return chance.current * skillLevel * perSkillpointIncrease;
	}


	static boolean canLevelupSkillWithQuest(Player player, SkillInfo skill, int levels) {
		final int playerSkillLevel = player.getSkillLevel(skill.id);
		if (skill.hasMaxLevel()) {
			if (playerSkillLevel + levels > skill.maxLevel) return false;
		}
		if (!skill.canLevelUpSkillTo(player, playerSkillLevel + levels)) return false;
		return true;
	}
	public static boolean canLevelupSkillManually(Player player, SkillInfo skill) {
		if (!player.hasAvailableSkillpoints()) return false;
		if (!canLevelupSkillWithQuest(player, skill, 1)) return false;
		if (skill.levelupVisibility == SkillInfo.LevelUpType.onlyByQuests) return false;
		if (skill.levelupVisibility == SkillInfo.LevelUpType.firstLevelRequiresQuest) {
			if (!player.hasSkill(skill.id)) return false;
		}
		return true;
	}
	public void levelUpSkillManually(Player player, SkillInfo skill) {
		if (!canLevelupSkillManually(player, skill)) return;
		player.availableSkillIncreases -= 1;
		addSkillLevel(skill.id);
	}
	public boolean levelUpSkillByQuest(Player player, SkillInfo skill) {
		if (!canLevelupSkillWithQuest(player, skill, 1)) return false;
		addSkillLevel(skill.id);
		return true;
	}

	public void addSkillLevel(SkillID skillID) {
		Player player = world.model.player;
		player.addSkillLevel(skillID);
		controllers.actorStatsController.recalculatePlayerStats(player);
	}

	public static int getActorConditionEffectChanceRollBias(ActorConditionEffect effect, Player player) {
		if (effect.chance.isMax()) return 0;

		int result = 0;
		result += getActorConditionEffectChanceRollBiasFromResistanceSkills(effect, player);
		result += getActorConditionEffectChanceRollBias(effect, player, SkillID.shadowBless, SkillCollection.PER_SKILLPOINT_INCREASE_RESISTANCE_SHADOW_BLESS);
		if ("spore_poison".equals(effect.conditionType.conditionTypeID)) {
			result += getActorConditionEffectChanceRollBias(effect, player, SkillID.sporeImmunity, 100);
		}
		return result;
	}

	private static int getActorConditionEffectChanceRollBiasFromResistanceSkills(ActorConditionEffect effect, Player player) {
		SkillID skill;
		switch (effect.conditionType.conditionCategory) {
		case mental:
			skill = SkillID.resistanceMental; break;
		case physical:
			skill = SkillID.resistancePhysical; break;
		case blood:
			skill = SkillID.resistanceBlood; break;
		case spiritual:
		default:
			return 0;
		}

		return getActorConditionEffectChanceRollBias(effect, player, skill, SkillCollection.PER_SKILLPOINT_INCREASE_RESISTANCE_CHANCE_PERCENT);
	}

	private static int getActorConditionEffectChanceRollBias(ActorConditionEffect effect, Player player, SkillID skill, int chanceIncreasePerSkillLevel) {
		// Note that the bias should be negative, making it less likely that the chance roll will succeed
		return getRollBias(effect.chance, player, skill, -chanceIncreasePerSkillLevel);
	}

	public static boolean rollForSkillChance(Player player, SkillID skill, int chancePerSkillLevel) {
		int skillLevel = player.getSkillLevel(skill);
		if (skillLevel <= 0) return false;
		return Constants.roll100(chancePerSkillLevel * skillLevel);
	}
	private void addConditionToActor(Actor target, String conditionName, int magnitude, int duration) {
		ActorConditionType conditionType = world.actorConditionsTypes.getActorConditionType(conditionName);
		ActorConditionEffect effect = new ActorConditionEffect(conditionType, magnitude, duration, null);
		controllers.actorStatsController.applyActorCondition(target, effect);
	}

	public void applySkillEffectsFromPlayerAttack(AttackResult result, Monster monster) {
		if (!result.isHit) return;

		Player player = world.model.player;

		if (player.getAttackChance() - monster.getBlockChance() > SkillCollection.CONCUSSION_THRESHOLD) {
			if (rollForSkillChance(player, SkillID.concussion, SkillCollection.PER_SKILLPOINT_INCREASE_CONCUSSION_CHANCE)) {
				addConditionToActor(monster, "concussion", 1, 5);
			}
		}

		if (result.isCriticalHit) {
			if (rollForSkillChance(player, SkillID.crit2, SkillCollection.PER_SKILLPOINT_INCREASE_CRIT2_CHANCE)) {
				addConditionToActor(monster, "crit2", 1, 5);
			}

			if (rollForSkillChance(player, SkillID.crit1, SkillCollection.PER_SKILLPOINT_INCREASE_CRIT1_CHANCE)) {
				addConditionToActor(monster, "crit1", 1, 5);
			}
		}
	}

	public void applySkillEffectsFromMonsterAttack(AttackResult result, Monster monster) {
		if (!result.isHit) {
			if (rollForSkillChance(world.model.player, SkillID.taunt, SkillCollection.PER_SKILLPOINT_INCREASE_TAUNT_CHANCE)) {
				combatActionListeners.onPlayerTauntsMonster(monster);
				controllers.actorStatsController.changeActorAP(monster, -SkillCollection.TAUNT_AP_LOSS, false, false);
			}
		}
	}

	public static void applySkillEffectsFromItemProficiencies(Player player) {
		Player playerTraits = player;

		ItemType mainWeapon = ItemController.getMainWeapon(player);
		if (mainWeapon != null) {
			final int skillLevel = getSkillLevelForItemType(player, mainWeapon);
			addPercentAttackChance(player, mainWeapon, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_AC_PERCENT * skillLevel, 0);
			addPercentBlockChance(player, mainWeapon, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_BC_PERCENT * skillLevel, 0);
			addPercentCriticalSkill(player, mainWeapon, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_CS_PERCENT * skillLevel, 0);
		}

		final int unarmedLevel = player.getSkillLevel(SkillID.weaponProficiencyUnarmed);
		if (unarmedLevel > 0) {
			if (isUnarmed(player)) {
				playerTraits.attackChance += SkillCollection.PER_SKILLPOINT_INCREASE_UNARMED_AC * unarmedLevel;
				playerTraits.damagePotential.addToMax(SkillCollection.PER_SKILLPOINT_INCREASE_UNARMED_DMG * unarmedLevel);
				playerTraits.damagePotential.add(SkillCollection.PER_SKILLPOINT_INCREASE_UNARMED_DMG * unarmedLevel, false);
				playerTraits.blockChance += SkillCollection.PER_SKILLPOINT_INCREASE_UNARMED_BC * unarmedLevel;
			}
		}

		ItemType shield = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.shield);
		if (shield != null && shield.isShield()) {
			playerTraits.damageResistance += SkillCollection.PER_SKILLPOINT_INCREASE_SHIELD_PROF_DR * getSkillLevelForItemType(player, shield);
		}

		final int unarmoredLevel = player.getSkillLevel(SkillID.armorProficiencyUnarmored);
		if (unarmoredLevel > 0) {
			if (isUnarmored(player)) {
				playerTraits.blockChance += SkillCollection.PER_SKILLPOINT_INCREASE_UNARMORED_BC * unarmoredLevel;
			}
		}

		int skillLevelLightArmor = player.getSkillLevel(SkillID.armorProficiencyLight);
		int skillLevelHeavyArmor = player.getSkillLevel(SkillID.armorProficiencyHeavy);
		for (Inventory.WearSlot slot : Inventory.WearSlot.values()) {
			if (!Inventory.isArmorSlot(slot)) continue;

			ItemType itemType = player.inventory.getItemTypeInWearSlot(slot);
			if (itemType == null) continue;
			if (itemType.effects_equip == null) continue;

			SkillID skill = getProficiencySkillForItemCategory(itemType.category);
			if (skill == SkillID.armorProficiencyLight) {
				if (skillLevelLightArmor > 0) {
					addPercentBlockChance(player, itemType, SkillCollection.PER_SKILLPOINT_INCREASE_LIGHT_ARMOR_BC_PERCENT * skillLevelLightArmor, 0);
				}
			} else if (skill == SkillID.armorProficiencyHeavy) {
				if (skillLevelHeavyArmor > 0) {
					addPercentBlockChance(player, itemType, SkillCollection.PER_SKILLPOINT_INCREASE_HEAVY_ARMOR_BC_PERCENT * skillLevelHeavyArmor, 0);
					playerTraits.moveCost -= getPercentage(itemType.effects_equip.stats.increaseMoveCost, SkillCollection.PER_SKILLPOINT_INCREASE_HEAVY_ARMOR_MOVECOST_PERCENT * skillLevelHeavyArmor, 0);
					playerTraits.attackCost -= getPercentage(itemType.effects_equip.stats.increaseAttackCost, SkillCollection.PER_SKILLPOINT_INCREASE_HEAVY_ARMOR_ATKCOST_PERCENT * skillLevelHeavyArmor, 0);
					playerTraits.useItemCost -= getPercentage(itemType.effects_equip.stats.increaseUseItemCost, SkillCollection.PER_SKILLPOINT_INCREASE_HEAVY_ARMOR_USECOST_PERCENT * skillLevelHeavyArmor, 0);
				}
			}
		}
	}

	private static boolean isUnarmed(Player player) {
		if (hasItemWithWeight(player, Inventory.WearSlot.weapon)) return false;
		if (hasItemWithWeight(player, Inventory.WearSlot.shield)) return false;
		return true;
	}
	private static boolean isUnarmored(Player player) {
		for (Inventory.WearSlot slot : Inventory.WearSlot.values()) {
			if (!Inventory.isArmorSlot(slot)) continue;
			if (hasItemWithWeight(player, slot)) return false;
		}
		return true;
	}
	private static boolean hasItemWithWeight(Player player, Inventory.WearSlot slot) {
		ItemType itemType = player.inventory.getItemTypeInWearSlot(slot);
		if (itemType == null) return false;
		if (itemType.category.getSize() == ItemCategory.ItemCategorySize.none) return false;
		return true;
	}

	private static int getSkillLevelForItemType(final Player player, ItemType itemType) {
		SkillID skill = getProficiencySkillForItemCategory(itemType.category);
		if (skill == null) return 0;
		return player.getSkillLevel(skill);
	}

	public static SkillID getProficiencySkillForItemCategory(ItemCategory category) {
		final String itemCategoryID = category.id;
		if (category.isWeapon()) {
			if (itemCategoryID.equals("dagger") || itemCategoryID.equals("ssword"))
				return SkillID.weaponProficiencyDagger;
			else if (itemCategoryID.equals("lsword") || itemCategoryID.equals("bsword") || itemCategoryID.equals("rapier"))
				return SkillID.weaponProficiency1hsword;
			else if (itemCategoryID.equals("2hsword"))
				return SkillID.weaponProficiency2hsword;
			else if (itemCategoryID.equals("axe") || itemCategoryID.equals("axe2h"))
				return SkillID.weaponProficiencyAxe;
			else if (itemCategoryID.equals("club") || itemCategoryID.equals("staff") || itemCategoryID.equals("mace")
					|| itemCategoryID.equals("scepter") || itemCategoryID.equals("hammer") || itemCategoryID.equals("hammer2h") || itemCategoryID.equals("whip"))
				return SkillID.weaponProficiencyBlunt;
            else if (itemCategoryID.equals("pole"))
                return SkillID.weaponProficiencyPole;
		} else if (category.isShield()) {
			return SkillID.armorProficiencyShield;
		} else if (category.isArmor()) {
			ItemCategory.ItemCategorySize size = category.getSize();
			if (size == ItemCategory.ItemCategorySize.light) return SkillID.armorProficiencyLight;
			if (size == ItemCategory.ItemCategorySize.std) return SkillID.armorProficiencyLight;
			if (size == ItemCategory.ItemCategorySize.large) return SkillID.armorProficiencyHeavy;
		}
		return null;
	}

	public static void applySkillEffectsFromFightingStyles(Player player) {
		Player playerTraits = player;
		ItemType mainHandItem = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.weapon);
		ItemType offHandItem = player.inventory.getItemTypeInWearSlot(Inventory.WearSlot.shield);

		final int skillLevelFightStyleUnarmedUnarmored = player.getSkillLevel(SkillID.fightstyleUnarmedUnarmored);
		if (skillLevelFightStyleUnarmedUnarmored > 0 && isUnarmored(player) && mainHandItem == null && offHandItem == null) {
			player.blockChance += SkillCollection.PER_SKILLPOINT_INCREASE_UNARMED_UNARMORED_BC * skillLevelFightStyleUnarmedUnarmored;
			player.damageResistance += SkillCollection.PER_SKILLPOINT_INCREASE_UNARMED_UNARMORED_DR * skillLevelFightStyleUnarmedUnarmored;
			player.attackChance += SkillCollection.PER_SKILLPOINT_INCREASE_UNARMED_UNARMORED_AC * skillLevelFightStyleUnarmedUnarmored;
			player.damagePotential.addToMax(SkillCollection.PER_SKILLPOINT_INCREASE_UNARMED_UNARMORED_DMG_MAX * skillLevelFightStyleUnarmedUnarmored);
			player.criticalMultiplier = 1 + ((float)SkillCollection.PER_SKILLPOINT_INCREASE_UNARMED_UNARMORED_CM_PERCENT / 100) * skillLevelFightStyleUnarmedUnarmored;
        }

		if (isWielding2HandItem(mainHandItem, offHandItem)) {
			int skillLevelFightStyle = player.getSkillLevel(SkillID.fightstyle2hand);
			int skillLevelSpecialization = player.getSkillLevel(SkillID.specialization2hand);
			addPercentDamage(playerTraits, mainHandItem, skillLevelFightStyle * SkillCollection.PER_SKILLPOINT_INCREASE_FIGHTSTYLE_2HAND_DMG_PERCENT, 0);
			addPercentDamage(playerTraits, mainHandItem, skillLevelSpecialization * SkillCollection.PER_SKILLPOINT_INCREASE_SPECIALIZATION_2HAND_DMG_PERCENT, 0);
			addPercentAttackChance(playerTraits, mainHandItem, skillLevelSpecialization * SkillCollection.PER_SKILLPOINT_INCREASE_SPECIALIZATION_2HAND_AC_PERCENT, 0);
		}

		if (isWieldingWeaponAndShield(mainHandItem, offHandItem)) {
			int skillLevelFightStyle = player.getSkillLevel(SkillID.fightstyleWeaponShield);
			int skillLevelSpecialization = player.getSkillLevel(SkillID.specializationWeaponShield);
			addPercentAttackChance(playerTraits, mainHandItem, skillLevelFightStyle * SkillCollection.PER_SKILLPOINT_INCREASE_FIGHTSTYLE_WEAPON_AC_PERCENT, 0);
			addPercentBlockChance(playerTraits, offHandItem, skillLevelFightStyle * SkillCollection.PER_SKILLPOINT_INCREASE_FIGHTSTYLE_SHIELD_BC_PERCENT, 0);
			addPercentAttackChance(playerTraits, mainHandItem, skillLevelSpecialization * SkillCollection.PER_SKILLPOINT_INCREASE_SPECIALIZATION_WEAPON_AC_PERCENT, 0);
			addPercentDamage(playerTraits, mainHandItem, skillLevelSpecialization * SkillCollection.PER_SKILLPOINT_INCREASE_SPECIALIZATION_WEAPON_DMG_PERCENT, 0);
		}

		if (isDualWielding(mainHandItem, offHandItem)) {
			int skillLevelFightStyle = player.getSkillLevel(SkillID.fightstyleDualWield);
			if (offHandItem.effects_equip != null) {
				int attackCostMainHand = mainHandItem.effects_equip.stats.increaseAttackCost;
				int attackCostOffHand = offHandItem.effects_equip.stats.increaseAttackCost;
				int percent;
				if (skillLevelFightStyle == 2) {
					percent = SkillCollection.DUALWIELD_EFFICIENCY_LEVEL2;
					playerTraits.attackCost = Math.max(attackCostMainHand, attackCostOffHand);
					playerTraits.criticalMultiplier = Math.max(mainHandItem.effects_equip.stats.setCriticalMultiplier, getPercentage(offHandItem.effects_equip.stats.setCriticalMultiplier, SkillCollection.DUALWIELD_EFFICIENCY_LEVEL2, 0));
				} else if (skillLevelFightStyle == 1) {
					percent = SkillCollection.DUALWIELD_EFFICIENCY_LEVEL1;
					//Take into account the case where the worst weapon AP-wise is in the off-hand.
					playerTraits.attackCost = Math.max(attackCostMainHand, attackCostOffHand) + getPercentage( Math.min(attackCostMainHand, attackCostOffHand), SkillCollection.DUALWIELD_LEVEL1_OFFHAND_AP_COST_PERCENT, 0);
					playerTraits.criticalMultiplier = Math.max(mainHandItem.effects_equip.stats.setCriticalMultiplier, getPercentage(offHandItem.effects_equip.stats.setCriticalMultiplier, SkillCollection.DUALWIELD_EFFICIENCY_LEVEL1, 0));
				} else {
					percent = SkillCollection.DUALWIELD_EFFICIENCY_LEVEL0;
					playerTraits.attackCost = attackCostMainHand + attackCostOffHand;
					playerTraits.criticalMultiplier = Math.max(mainHandItem.effects_equip.stats.setCriticalMultiplier, getPercentage(offHandItem.effects_equip.stats.setCriticalMultiplier, SkillCollection.DUALWIELD_EFFICIENCY_LEVEL0, 0));
				}

				final int skillLevel = getSkillLevelForItemType(player, offHandItem);
				addPercentAttackChance(player, offHandItem, getPercentage(SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_AC_PERCENT * skillLevel, percent, 0), 0);
				addPercentBlockChance(player, offHandItem, getPercentage(SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_BC_PERCENT * skillLevel, percent, 0), 0);
				addPercentCriticalSkill(player, offHandItem, getPercentage(SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_CS_PERCENT * skillLevel, percent, 0), 0);

				addPercentAttackChance(player, offHandItem, percent, 100);
				addPercentBlockChance(player, offHandItem, percent, 100);
				addPercentDamage(player, offHandItem, percent, 100);
				addPercentCriticalSkill(player, offHandItem, percent, 100);
				addPercentMaxHPBoost(player, offHandItem, percent, 100);
				addPercentDamageResistance(player, offHandItem, percent, 100);
				addPercentMaxAPBoost(player, offHandItem, percent, 100);
				//Reversed parameters, as a positive value is a malus for these...
				addPercentMoveCost(player, offHandItem, 100, percent);
				addPercentReequipCost(player, offHandItem, 100, percent);
				addPercentUseItemCost(player, offHandItem, 100, percent);
				
			}

			int skillLevelSpecialization = player.getSkillLevel(SkillID.specializationDualWield);
			addPercentAttackChance(playerTraits, mainHandItem, skillLevelSpecialization * SkillCollection.PER_SKILLPOINT_INCREASE_SPECIALIZATION_DUALWIELD_AC_PERCENT, 0);
			addPercentBlockChance(playerTraits, mainHandItem, skillLevelSpecialization * SkillCollection.PER_SKILLPOINT_INCREASE_SPECIALIZATION_DUALWIELD_BC_PERCENT, 0);
			addPercentAttackChance(playerTraits, offHandItem, skillLevelSpecialization * SkillCollection.PER_SKILLPOINT_INCREASE_SPECIALIZATION_DUALWIELD_AC_PERCENT, 0);
			addPercentBlockChance(playerTraits, offHandItem, skillLevelSpecialization * SkillCollection.PER_SKILLPOINT_INCREASE_SPECIALIZATION_DUALWIELD_BC_PERCENT, 0);
		}
	}

	private static void addPercentAttackChance(Player player, ItemType itemType, int percentForPositiveValues, int percentForNegativeValues) {
		if (itemType.effects_equip == null) return;
		player.attackChance += getPercentage(itemType.effects_equip.stats.increaseAttackChance, percentForPositiveValues, percentForNegativeValues);
	}

	private static void addPercentBlockChance(Player player, ItemType itemType, int percentForPositiveValues, int percentForNegativeValues) {
		if (itemType.effects_equip == null) return;
		player.blockChance += getPercentage(itemType.effects_equip.stats.increaseBlockChance, percentForPositiveValues, percentForNegativeValues);
	}

	private static void addPercentDamage(Player player, ItemType itemType, int percentForPositiveValues, int percentForNegativeValues) {
		if (itemType.effects_equip == null) return;
		player.damagePotential.addToMax(getPercentage(itemType.effects_equip.stats.increaseMaxDamage, percentForPositiveValues, percentForNegativeValues));
		player.damagePotential.add(getPercentage(itemType.effects_equip.stats.increaseMinDamage, percentForPositiveValues, percentForNegativeValues), false);

		if (itemType.isWeapon()) {
			player.weaponDamage.addToMax(getPercentage(itemType.effects_equip.stats.increaseMaxDamage, percentForPositiveValues, percentForNegativeValues));
			player.weaponDamage.add(getPercentage(itemType.effects_equip.stats.increaseMinDamage, percentForPositiveValues, percentForNegativeValues), false);
		}
	}

	private static void addPercentCriticalSkill(Player player, ItemType itemType, int percentForPositiveValues, int percentForNegativeValues) {
		if (itemType.effects_equip == null) return;
		player.criticalSkill += getPercentage(itemType.effects_equip.stats.increaseCriticalSkill, percentForPositiveValues, percentForNegativeValues);
	}
	
	private static void addPercentMaxHPBoost(Player player, ItemType itemType, int percentForPositiveValues, int percentForNegativeValues) {
		if (itemType.effects_equip == null) return;
		player.health.addToMax(getPercentage(itemType.effects_equip.stats.increaseMaxHP, percentForPositiveValues, percentForNegativeValues));
	}
	
	private static void addPercentDamageResistance(Player player, ItemType itemType, int percentForPositiveValues, int percentForNegativeValues) {
		if (itemType.effects_equip == null) return;
		player.damageResistance += getPercentage(itemType.effects_equip.stats.increaseDamageResistance, percentForPositiveValues, percentForNegativeValues);
	}
	
	private static void addPercentMaxAPBoost(Player player, ItemType itemType, int percentForPositiveValues, int percentForNegativeValues) {
		if (itemType.effects_equip == null) return;
		player.ap.addToMax(getPercentage(itemType.effects_equip.stats.increaseMaxAP, percentForPositiveValues, percentForNegativeValues));
	}
	
	private static void addPercentMoveCost(Player player, ItemType itemType, int percentForPositiveValues, int percentForNegativeValues) {
		if (itemType.effects_equip == null) return;
		player.moveCost += getPercentage(itemType.effects_equip.stats.increaseMoveCost, percentForPositiveValues, percentForNegativeValues);
	}
	
	private static void addPercentUseItemCost(Player player, ItemType itemType, int percentForPositiveValues, int percentForNegativeValues) {
		if (itemType.effects_equip == null) return;
		player.useItemCost += getPercentage(itemType.effects_equip.stats.increaseUseItemCost, percentForPositiveValues, percentForNegativeValues);
	}
	
	private static void addPercentReequipCost(Player player, ItemType itemType, int percentForPositiveValues, int percentForNegativeValues) {
		if (itemType.effects_equip == null) return;
		player.reequipCost += getPercentage(itemType.effects_equip.stats.increaseReequipCost, percentForPositiveValues, percentForNegativeValues);
	}

	private static int getPercentage(int originalValue, int percentForPositiveValues, int percentForNegativeValues) {
		if (originalValue == 0) {
			return 0;
		} else if (originalValue > 0) {
			return (int) Math.floor(originalValue * percentForPositiveValues / 100.0f);
		} else {
			return (int) Math.floor(originalValue * percentForNegativeValues / 100.0f);
		}
	}
	
	private static float getPercentage(float originalValue, int percentForPositiveValues, int percentForNegativeValues) {
		if (originalValue == 0) {
			return 0;
		} else if (originalValue > 0) {
			return originalValue * percentForPositiveValues / 100.0f;
		} else {
			return originalValue * percentForNegativeValues / 100.0f;
		}
	}

	public static boolean isDualWielding(ItemType mainHandItem, ItemType offHandItem) {
		if (mainHandItem == null) return false;
		if (offHandItem == null) return false;
		return mainHandItem.isWeapon() && offHandItem.isWeapon();
	}

	private static boolean isWielding2HandItem(ItemType mainHandItem, ItemType offHandItem) {
		if (mainHandItem == null) return false;
		if (offHandItem != null) return false;
		return mainHandItem.isTwohandWeapon();
	}

	private static boolean isWieldingWeaponAndShield(ItemType mainHandItem, ItemType offHandItem) {
		if (mainHandItem == null) return false;
		if (offHandItem == null) return false;
		return mainHandItem.isWeapon() && offHandItem.isShield();
	}
}
