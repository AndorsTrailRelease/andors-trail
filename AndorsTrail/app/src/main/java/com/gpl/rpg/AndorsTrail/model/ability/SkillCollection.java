package com.gpl.rpg.AndorsTrail.model.ability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

import com.gpl.rpg.AndorsTrail.controller.Constants;
import com.gpl.rpg.AndorsTrail.model.ability.SkillInfo.SkillLevelRequirement;
import com.gpl.rpg.AndorsTrail.model.actor.Player;

public final class SkillCollection {
	public static enum SkillID {
		weaponChance
		,weaponDmg
		,barter
		,dodge
		,barkSkin
		,moreCriticals
		,betterCriticals
		,speed				// Raises max ap
		,coinfinder
		,moreExp
		,cleave				// +10ap on kill
		,eater				// +1hp per kill
		,fortitude			// +N hp per levelup
		,evasion			// increase successful flee chance & reduce chance of monster attack
		,regeneration		// +N hp per round
		,lowerExploss
		,magicfinder
		,resistanceMental	// lowers chance to get negative active conditions by monsters (Mental like Dazed)
		,resistancePhysical	// lowers chance to get negative active conditions by monsters (Physical Capacity like Minor fatigue)
		,resistanceBlood	// lowers chance to get negative active conditions by monsters (Blood Disorder like Weak Poison)
		,shadowBless
		,crit1			// lowers atk ability
		,crit2			// lowers def ability ,rejuvenation	// Reduces magnitudes of conditions
		,rejuvenation	// Reduces magnitudes of conditions
		,taunt			// Causes AP loss of attackers that miss
		,concussion		// AC loss for monsters with (AC-BC)>N
		,weaponProficiencyDagger
		,weaponProficiency1hsword
		,weaponProficiency2hsword
		,weaponProficiencyAxe
		,weaponProficiencyBlunt
		,weaponProficiencyUnarmed
		,armorProficiencyShield
		,armorProficiencyUnarmored
		,armorProficiencyLight
		,armorProficiencyHeavy
		,fightstyleDualWield
		,fightstyle2hand
		,fightstyleWeaponShield
		,specializationDualWield
		,specialization2hand
		,specializationWeaponShield
		,weaponProficiencyPole
		,fightstyleUnarmedUnarmored
		,sporeImmunity
	}

	// should be in the same number &U order as in arrays.xml
	public static enum SkillCategory {
		all, // filler value -- don't use
				offense,
				defense,
				criticals,
				immunity, //fortitude?
				utility, //	efficiency?
				specialty,
				proficiency
	}



	public static final int PER_SKILLPOINT_INCREASE_WEAPON_CHANCE = 12;
	public static final int PER_SKILLPOINT_INCREASE_WEAPON_DAMAGE_MAX = 2;
	public static final int PER_SKILLPOINT_INCREASE_WEAPON_DAMAGE_MIN = 0;
	public static final int PER_SKILLPOINT_INCREASE_DODGE = 9;
	public static final int PER_SKILLPOINT_INCREASE_BARKSKIN = 1;
	public static final int PER_SKILLPOINT_INCREASE_MORE_CRITICALS_PERCENT = 20;
	public static final int PER_SKILLPOINT_INCREASE_BETTER_CRITICALS_PERCENT = 25;
	public static final int PER_SKILLPOINT_INCREASE_SPEED = 1;
	public static final int PER_SKILLPOINT_INCREASE_BARTER_PRICEFACTOR_PERCENTAGE = 4;
	public static final int PER_SKILLPOINT_INCREASE_COINFINDER_CHANCE_PERCENT = 30;
	public static final int PER_SKILLPOINT_INCREASE_MAGICFINDER_CHANCE_PERCENT = 50;
	public static final int PER_SKILLPOINT_INCREASE_COINFINDER_QUANTITY_PERCENT = 50;
	public static final int PER_SKILLPOINT_INCREASE_MORE_EXP_PERCENT = 10;
	public static final int PER_SKILLPOINT_INCREASE_CLEAVE_AP = 3;
	public static final int PER_SKILLPOINT_INCREASE_EATER_HEALTH = 1;
	public static final int PER_SKILLPOINT_INCREASE_FORTITUDE_HEALTH = 1;
	public static final int PER_SKILLPOINT_INCREASE_EVASION_FLEE_CHANCE_PERCENTAGE = 5;
	public static final int PER_SKILLPOINT_INCREASE_EVASION_MONSTER_ATTACK_CHANCE_PERCENTAGE = 5;
	public static final int PER_SKILLPOINT_INCREASE_REGENERATION = 1;
	public static final int PER_SKILLPOINT_INCREASE_EXPLOSS_PERCENT = 20;
	public static final int PER_SKILLPOINT_INCREASE_RESISTANCE_CHANCE_PERCENT = 10;
	public static final int PER_SKILLPOINT_INCREASE_RESISTANCE_SHADOW_BLESS = 5;
	public static final int PER_SKILLPOINT_INCREASE_CRIT1_CHANCE = 50;
	public static final int PER_SKILLPOINT_INCREASE_CRIT2_CHANCE = 50;
	public static final int PER_SKILLPOINT_INCREASE_REJUVENATION_CHANCE = 20;
	public static final int PER_SKILLPOINT_INCREASE_TAUNT_CHANCE = 75;
	public static final int TAUNT_AP_LOSS = 2;
	public static final int CONCUSSION_THRESHOLD = 50;
	public static final int PER_SKILLPOINT_INCREASE_CONCUSSION_CHANCE = 15;
	public static final int PER_SKILLPOINT_INCREASE_WEAPON_PROF_AC_PERCENT = 30;
	public static final int PER_SKILLPOINT_INCREASE_WEAPON_PROF_CS_PERCENT = 10;
	public static final int PER_SKILLPOINT_INCREASE_WEAPON_PROF_BC_PERCENT = 30;
	public static final int PER_SKILLPOINT_INCREASE_UNARMED_AC = 20;
	public static final int PER_SKILLPOINT_INCREASE_UNARMED_DMG = 2;
	public static final int PER_SKILLPOINT_INCREASE_UNARMED_BC = 5;
	public static final int PER_SKILLPOINT_INCREASE_SHIELD_PROF_DR = 1;
	public static final int PER_SKILLPOINT_INCREASE_UNARMORED_BC = 10;
	public static final int PER_SKILLPOINT_INCREASE_LIGHT_ARMOR_BC_PERCENT = 30;
	public static final int PER_SKILLPOINT_INCREASE_HEAVY_ARMOR_BC_PERCENT = 20;
	public static final int PER_SKILLPOINT_INCREASE_HEAVY_ARMOR_MOVECOST_PERCENT = 25;
	public static final int PER_SKILLPOINT_INCREASE_HEAVY_ARMOR_ATKCOST_PERCENT = 25;
	public static final int PER_SKILLPOINT_INCREASE_HEAVY_ARMOR_USECOST_PERCENT = 25;
	public static final int PER_SKILLPOINT_INCREASE_FIGHTSTYLE_2HAND_DMG_PERCENT = 30;
	public static final int PER_SKILLPOINT_INCREASE_SPECIALIZATION_2HAND_DMG_PERCENT = 50;
	public static final int PER_SKILLPOINT_INCREASE_SPECIALIZATION_2HAND_AC_PERCENT = 20;
	public static final int PER_SKILLPOINT_INCREASE_FIGHTSTYLE_WEAPON_AC_PERCENT = 25;
	public static final int PER_SKILLPOINT_INCREASE_FIGHTSTYLE_SHIELD_BC_PERCENT = 25;
	public static final int PER_SKILLPOINT_INCREASE_UNARMED_UNARMORED_BC = 5;
	public static final int PER_SKILLPOINT_INCREASE_UNARMED_UNARMORED_DR = 1;
	public static final int PER_SKILLPOINT_INCREASE_UNARMED_UNARMORED_AC = 12;
	public static final int PER_SKILLPOINT_INCREASE_UNARMED_UNARMORED_DMG_MAX = 4;
	public static final int PER_SKILLPOINT_INCREASE_UNARMED_UNARMORED_CM_PERCENT = 25;
	public static final int PER_SKILLPOINT_INCREASE_SPECIALIZATION_WEAPON_AC_PERCENT = 50;
	public static final int PER_SKILLPOINT_INCREASE_SPECIALIZATION_WEAPON_DMG_PERCENT = 20;
	public static final int DUALWIELD_EFFICIENCY_LEVEL2 = 100;
	public static final int DUALWIELD_EFFICIENCY_LEVEL1 = 50;
	public static final int DUALWIELD_EFFICIENCY_LEVEL0 = 25;
	public static final int DUALWIELD_LEVEL1_OFFHAND_AP_COST_PERCENT = 50;
	public static final int PER_SKILLPOINT_INCREASE_SPECIALIZATION_DUALWIELD_AC_PERCENT = 50;
	public static final int PER_SKILLPOINT_INCREASE_SPECIALIZATION_DUALWIELD_BC_PERCENT = 50;

	private static final int MAX_LEVEL_BARTER = (int) Math.floor((float) Constants.MARKET_PRICEFACTOR_PERCENT / PER_SKILLPOINT_INCREASE_BARTER_PRICEFACTOR_PERCENTAGE);
	private static final int MAX_LEVEL_BARKSKIN = 5;
	private static final int MAX_LEVEL_SPEED = 2;
	private static final int MAX_LEVEL_EVASION = Math.max(
			Constants.FLEE_FAIL_CHANCE_PERCENT / PER_SKILLPOINT_INCREASE_EVASION_FLEE_CHANCE_PERCENTAGE
			,Constants.MONSTER_AGGRESSION_CHANCE_PERCENT / PER_SKILLPOINT_INCREASE_EVASION_MONSTER_ATTACK_CHANCE_PERCENTAGE
			);
	public static final int MAX_LEVEL_LOWER_EXPLOSS = 100 / PER_SKILLPOINT_INCREASE_EXPLOSS_PERCENT;
	public static final int MAX_LEVEL_RESISTANCE = 70 / PER_SKILLPOINT_INCREASE_RESISTANCE_CHANCE_PERCENT;

	private final LinkedHashMap<Integer, SkillInfo> skills = new LinkedHashMap<Integer, SkillInfo>();
	private void initializeSkill(SkillInfo skill) {
		skills.put(skill.id.ordinal(), skill);
	}
	public void initialize() {
		int position = 0;
		initializeSkill(new SkillInfo(SkillID.weaponChance, SkillInfo.MAXLEVEL_NONE, SkillInfo.LevelUpType.alwaysShown, SkillCategory.offense, null, position++));
		initializeSkill(new SkillInfo(SkillID.weaponDmg, SkillInfo.MAXLEVEL_NONE, SkillInfo.LevelUpType.alwaysShown, SkillCategory.offense, null, position++));
		initializeSkill(new SkillInfo(SkillID.barter, MAX_LEVEL_BARTER, SkillInfo.LevelUpType.alwaysShown, SkillCategory.utility, null, position++));
		initializeSkill(new SkillInfo(SkillID.dodge, SkillInfo.MAXLEVEL_NONE, SkillInfo.LevelUpType.alwaysShown, SkillCategory.defense, null, position++));
		initializeSkill(new SkillInfo(SkillID.barkSkin, MAX_LEVEL_BARKSKIN, SkillInfo.LevelUpType.alwaysShown, SkillCategory.defense, new SkillLevelRequirement[] {
			SkillLevelRequirement.requireExperienceLevels(10, 0)
			,SkillLevelRequirement.requirePlayerStats(Player.StatID.blockChance, 15, 0)
		}, position++));
		initializeSkill(new SkillInfo(SkillID.moreCriticals, SkillInfo.MAXLEVEL_NONE, SkillInfo.LevelUpType.alwaysShown, SkillCategory.criticals, null, position++));
		initializeSkill(new SkillInfo(SkillID.betterCriticals, SkillInfo.MAXLEVEL_NONE, SkillInfo.LevelUpType.alwaysShown, SkillCategory.criticals, new SkillLevelRequirement[] {
			SkillLevelRequirement.requireOtherSkill(SkillID.moreCriticals, 1)
		}, position++));
		initializeSkill(new SkillInfo(SkillID.speed, MAX_LEVEL_SPEED, SkillInfo.LevelUpType.alwaysShown, SkillCategory.offense, new SkillLevelRequirement[] {
			SkillLevelRequirement.requireExperienceLevels(15, 0)
		}, position++));
		initializeSkill(new SkillInfo(SkillID.coinfinder, SkillInfo.MAXLEVEL_NONE, SkillInfo.LevelUpType.alwaysShown, SkillCategory.utility, null, position++));
		initializeSkill(new SkillInfo(SkillID.moreExp, SkillInfo.MAXLEVEL_NONE, SkillInfo.LevelUpType.alwaysShown, SkillCategory.utility, null, position++));
		initializeSkill(new SkillInfo(SkillID.cleave, SkillInfo.MAXLEVEL_NONE, SkillInfo.LevelUpType.alwaysShown, SkillCategory.offense, new SkillLevelRequirement[] {
			SkillLevelRequirement.requireOtherSkill(SkillID.weaponChance, 1)
			,SkillLevelRequirement.requireOtherSkill(SkillID.weaponDmg, 1)
		}, position++));
		initializeSkill(new SkillInfo(SkillID.eater, SkillInfo.MAXLEVEL_NONE, SkillInfo.LevelUpType.alwaysShown, SkillCategory.immunity, new SkillLevelRequirement[] {
			SkillLevelRequirement.requirePlayerStats(Player.StatID.maxHP, 20, 20)
		}, position++));
		initializeSkill(new SkillInfo(SkillID.fortitude, SkillInfo.MAXLEVEL_NONE, SkillInfo.LevelUpType.alwaysShown, SkillCategory.immunity, new SkillLevelRequirement[] {
			SkillLevelRequirement.requireExperienceLevels(15, -10)
		}, position++));
		initializeSkill(new SkillInfo(SkillID.evasion, MAX_LEVEL_EVASION, SkillInfo.LevelUpType.alwaysShown, SkillCategory.defense, null, position++));
		initializeSkill(new SkillInfo(SkillID.regeneration, SkillInfo.MAXLEVEL_NONE, SkillInfo.LevelUpType.alwaysShown, SkillCategory.immunity, new SkillLevelRequirement[] {
			SkillLevelRequirement.requirePlayerStats(Player.StatID.maxHP, 30, 0)
			,SkillLevelRequirement.requireOtherSkill(SkillID.fortitude, 1)
		}, position++));
		initializeSkill(new SkillInfo(SkillID.lowerExploss, MAX_LEVEL_LOWER_EXPLOSS, SkillInfo.LevelUpType.alwaysShown, SkillCategory.utility, null, position++));
		initializeSkill(new SkillInfo(SkillID.magicfinder, SkillInfo.MAXLEVEL_NONE, SkillInfo.LevelUpType.alwaysShown, SkillCategory.utility, null, position++));
		initializeSkill(new SkillInfo(SkillID.resistanceMental, MAX_LEVEL_RESISTANCE, SkillInfo.LevelUpType.alwaysShown, SkillCategory.immunity, null, position++));
		initializeSkill(new SkillInfo(SkillID.resistancePhysical, MAX_LEVEL_RESISTANCE, SkillInfo.LevelUpType.alwaysShown, SkillCategory.immunity, null, position++));
		initializeSkill(new SkillInfo(SkillID.resistanceBlood, MAX_LEVEL_RESISTANCE, SkillInfo.LevelUpType.alwaysShown, SkillCategory.immunity, null, position++));
		initializeSkill(new SkillInfo(SkillID.shadowBless, 1, SkillInfo.LevelUpType.onlyByQuests, SkillCategory.immunity, null, position++));
		initializeSkill(new SkillInfo(SkillID.crit1, 1, SkillInfo.LevelUpType.alwaysShown, SkillCategory.criticals, new SkillLevelRequirement[] {
			SkillLevelRequirement.requireOtherSkill(SkillID.moreCriticals, 2)
			,SkillLevelRequirement.requireOtherSkill(SkillID.betterCriticals, 2)
		}, position++));
		initializeSkill(new SkillInfo(SkillID.crit2, 1, SkillInfo.LevelUpType.alwaysShown, SkillCategory.criticals, new SkillLevelRequirement[] {
			SkillLevelRequirement.requireOtherSkill(SkillID.moreCriticals, 4)
			,SkillLevelRequirement.requireOtherSkill(SkillID.betterCriticals, 4)
			,SkillLevelRequirement.requireOtherSkill(SkillID.crit1, 1)
		}, position++));
		initializeSkill(new SkillInfo(SkillID.rejuvenation, 1, SkillInfo.LevelUpType.alwaysShown, SkillCategory.immunity, new SkillLevelRequirement[] {
			SkillLevelRequirement.requireOtherSkill(SkillID.resistanceBlood, 3)
			,SkillLevelRequirement.requireOtherSkill(SkillID.resistanceMental, 3)
			,SkillLevelRequirement.requireOtherSkill(SkillID.resistancePhysical, 3)
		}, position++));
		initializeSkill(new SkillInfo(SkillID.taunt, 1, SkillInfo.LevelUpType.alwaysShown, SkillCategory.defense, new SkillLevelRequirement[] {
			SkillLevelRequirement.requireOtherSkill(SkillID.evasion, 2)
			,SkillLevelRequirement.requireOtherSkill(SkillID.dodge, 4)
		}, position++));
		initializeSkill(new SkillInfo(SkillID.concussion, 1, SkillInfo.LevelUpType.alwaysShown, SkillCategory.offense, new SkillLevelRequirement[] {
			SkillLevelRequirement.requireOtherSkill(SkillID.speed, 2)
			,SkillLevelRequirement.requireOtherSkill(SkillID.weaponChance, 3)
			,SkillLevelRequirement.requireOtherSkill(SkillID.weaponDmg, 5)
		}, position++));
		initializeSkill(new SkillInfo(SkillID.weaponProficiencyDagger, 3, SkillInfo.LevelUpType.firstLevelRequiresQuest, SkillCategory.proficiency, null, position++));
		initializeSkill(new SkillInfo(SkillID.weaponProficiency1hsword, 3, SkillInfo.LevelUpType.firstLevelRequiresQuest, SkillCategory.proficiency, null, position++));
		initializeSkill(new SkillInfo(SkillID.weaponProficiency2hsword, 3, SkillInfo.LevelUpType.firstLevelRequiresQuest, SkillCategory.proficiency, null, position++));
		initializeSkill(new SkillInfo(SkillID.weaponProficiencyAxe, 3, SkillInfo.LevelUpType.firstLevelRequiresQuest, SkillCategory.proficiency, null, position++));
		initializeSkill(new SkillInfo(SkillID.weaponProficiencyBlunt, 3, SkillInfo.LevelUpType.firstLevelRequiresQuest, SkillCategory.proficiency, null, position++));
		initializeSkill(new SkillInfo(SkillID.weaponProficiencyUnarmed, 3, SkillInfo.LevelUpType.firstLevelRequiresQuest, SkillCategory.proficiency, null, position++));
		initializeSkill(new SkillInfo(SkillID.weaponProficiencyPole, 3, SkillInfo.LevelUpType.firstLevelRequiresQuest, SkillCategory.proficiency, null, position++));
		initializeSkill(new SkillInfo(SkillID.armorProficiencyShield, 2, SkillInfo.LevelUpType.firstLevelRequiresQuest, SkillCategory.proficiency, null, position++));
		initializeSkill(new SkillInfo(SkillID.armorProficiencyUnarmored, 3, SkillInfo.LevelUpType.firstLevelRequiresQuest, SkillCategory.proficiency, null, position++));
		initializeSkill(new SkillInfo(SkillID.armorProficiencyLight, 3, SkillInfo.LevelUpType.firstLevelRequiresQuest, SkillCategory.proficiency, null, position++));
		initializeSkill(new SkillInfo(SkillID.armorProficiencyHeavy, 4, SkillInfo.LevelUpType.firstLevelRequiresQuest, SkillCategory.proficiency, null, position++));
		initializeSkill(new SkillInfo(SkillID.fightstyleDualWield, 2, SkillInfo.LevelUpType.alwaysShown, SkillCategory.specialty, new SkillLevelRequirement[] {
				SkillLevelRequirement.requireExperienceLevels(15, 0)
			}, position++));
		initializeSkill(new SkillInfo(SkillID.fightstyle2hand, 2, SkillInfo.LevelUpType.alwaysShown, SkillCategory.specialty, new SkillLevelRequirement[] {
				SkillLevelRequirement.requireExperienceLevels(15, 0)
			}, position++));
		initializeSkill(new SkillInfo(SkillID.fightstyleWeaponShield, 2, SkillInfo.LevelUpType.alwaysShown, SkillCategory.specialty, new SkillLevelRequirement[] {
				SkillLevelRequirement.requireExperienceLevels(15, 0)
			}, position++));
		initializeSkill(new SkillInfo(SkillID.fightstyleUnarmedUnarmored, 3, SkillInfo.LevelUpType.alwaysShown, SkillCategory.specialty, new SkillLevelRequirement[] {
				SkillLevelRequirement.requireExperienceLevels(15, 0)
		}, position++));
		initializeSkill(new SkillInfo(SkillID.specializationDualWield, 1, SkillInfo.LevelUpType.alwaysShown, SkillCategory.specialty, new SkillLevelRequirement[] {
				SkillLevelRequirement.requireExperienceLevels(45, 0)
				,SkillLevelRequirement.requireOtherSkill(SkillID.fightstyleDualWield, 2)
			}, position++));
		initializeSkill(new SkillInfo(SkillID.specialization2hand, 1, SkillInfo.LevelUpType.alwaysShown, SkillCategory.specialty, new SkillLevelRequirement[] {
				SkillLevelRequirement.requireExperienceLevels(45, 0)
				,SkillLevelRequirement.requireOtherSkill(SkillID.fightstyle2hand, 2)
			}, position++));
		initializeSkill(new SkillInfo(SkillID.specializationWeaponShield, 1, SkillInfo.LevelUpType.alwaysShown, SkillCategory.specialty, new SkillLevelRequirement[] {
				SkillLevelRequirement.requireExperienceLevels(45, 0)
				,SkillLevelRequirement.requireOtherSkill(SkillID.fightstyleWeaponShield, 2)
			}, position++));
		initializeSkill(new SkillInfo(SkillID.sporeImmunity, 1, SkillInfo.LevelUpType.onlyByQuests, SkillCategory.immunity, null, position++));
	}

	public SkillInfo getSkill(SkillID skillID) {
		return skills.get(skillID.ordinal());
	}

	public Collection<SkillInfo> getAllSkills() {
		ArrayList<SkillInfo> result = new ArrayList<SkillInfo>(skills.size());
		for(int i = 0; i < skills.size(); ++i) result.add(skills.get(i));
		return result;
	}
}
