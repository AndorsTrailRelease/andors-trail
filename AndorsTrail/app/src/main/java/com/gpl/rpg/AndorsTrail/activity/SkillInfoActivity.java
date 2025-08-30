package com.gpl.rpg.AndorsTrail.activity;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.SkillController;
import com.gpl.rpg.AndorsTrail.model.ability.SkillCollection;
import com.gpl.rpg.AndorsTrail.model.ability.SkillInfo;
import com.gpl.rpg.AndorsTrail.model.ability.SkillInfo.SkillLevelRequirement;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.util.ThemeHelper;

public final class SkillInfoActivity extends AndorsTrailBaseActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setTheme(ThemeHelper.getDialogTheme());
		super.onCreate(savedInstanceState);
		AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
		if (!app.isInitialized()) { finish(); return; }
		final WorldContext world = app.getWorld();
		final Player player = world.model.player;

		initializeView(this, R.layout.skill_info_view, R.id.skillinfo_root);

		final Resources res = getResources();
		final Intent intent = getIntent();
		final SkillCollection.SkillID skillID = SkillCollection.SkillID.valueOf(intent.getExtras().getString("skillID"));
		SkillInfo skill = world.skills.getSkill(skillID);

		TextView skillinfo_title = (TextView) findViewById(R.id.skillinfo_title);
		skillinfo_title.setText(getSkillTitleResourceID(skillID));

		TextView skillinfo_longdescription = (TextView) findViewById(R.id.skillinfo_longdescription);
		skillinfo_longdescription.setText(getSkillLongDescription(skillID, res));

		TextView skillinfo_currentlevel = (TextView) findViewById(R.id.skillinfo_currentlevel);
		final int playerSkillLevel = player.getSkillLevel(skillID);
		final int nextSkillLevel = playerSkillLevel + 1;
		if (skill.hasMaxLevel()) {
			skillinfo_currentlevel.setText(res.getString(R.string.skill_current_level_with_maximum, playerSkillLevel, skill.maxLevel));
		} else if (player.hasSkill(skillID)) {
			skillinfo_currentlevel.setText(res.getString(R.string.skill_current_level, playerSkillLevel));
		} else {
			skillinfo_currentlevel.setVisibility(View.GONE);
		}


		TextView skillinfo_requirement = (TextView) findViewById(R.id.skillinfo_requirement);
		LayoutParams requirementParams = skillinfo_requirement.getLayoutParams();
		ViewGroup requirementList = (ViewGroup) skillinfo_requirement.getParent();
		requirementList.removeView(skillinfo_requirement);
		if (shouldShowSkillRequirements(skill, playerSkillLevel)) {
			for (SkillLevelRequirement requirement : skill.levelupRequirements) {
				TextView tv = new TextView(this);
				tv.setLayoutParams(requirementParams);

				int requiredValue = requirement.getRequiredValue(nextSkillLevel);
				tv.setText(getRequirementDescription(requirement, requiredValue, res));
				boolean satisfiesRequirement = requirement.isSatisfiedByPlayer(player, nextSkillLevel);
				tv.setEnabled(!satisfiesRequirement);

				requirementList.addView(tv, requirementParams);
			}
		}


		Button b = (Button) findViewById(R.id.skillinfoinfo_close);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				setResult(RESULT_CANCELED);
				SkillInfoActivity.this.finish();
			}
		});

		b = (Button) findViewById(R.id.skillinfoinfo_action);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent result = new Intent();
				result.putExtras(intent);
				setResult(RESULT_OK, result);
				SkillInfoActivity.this.finish();
			}
		});
		b.setEnabled(SkillController.canLevelupSkillManually(player, skill));
	}

	private static boolean shouldShowSkillRequirements(SkillInfo skill, int playerSkillLevel) {
		if (!skill.hasLevelupRequirements()) return false;
		if (!skill.hasMaxLevel()) return true;
		if (playerSkillLevel >= skill.maxLevel) return false;
		return true;
	}

	public static int getSkillTitleResourceID(SkillCollection.SkillID skill) {
		switch (skill) {
		case weaponChance: return R.string.skill_title_weapon_chance;
		case weaponDmg: return R.string.skill_title_weapon_dmg;
		case barter: return R.string.skill_title_barter;
		case dodge: return R.string.skill_title_dodge;
		case barkSkin: return R.string.skill_title_barkskin;
		case moreCriticals: return R.string.skill_title_more_criticals;
		case betterCriticals: return R.string.skill_title_better_criticals;
		case speed: return R.string.skill_title_speed;
		case coinfinder: return R.string.skill_title_coinfinder;
		case moreExp: return R.string.skill_title_more_exp;
		case cleave: return R.string.skill_title_cleave;
		case eater: return R.string.skill_title_eater;
		case fortitude: return R.string.skill_title_fortitude;
		case evasion: return R.string.skill_title_evasion;
		case regeneration: return R.string.skill_title_regeneration;
		case lowerExploss: return R.string.skill_title_lower_exploss;
		case magicfinder: return R.string.skill_title_magicfinder;
		case resistanceMental: return R.string.skill_title_resistance_mental;
		case resistancePhysical: return R.string.skill_title_resistance_physical_capacity;
		case resistanceBlood: return R.string.skill_title_resistance_blood_disorder;
		case shadowBless: return R.string.skill_title_shadow_bless;
		case crit1: return R.string.skill_title_crit1;
		case crit2: return R.string.skill_title_crit2;
		case rejuvenation: return R.string.skill_title_rejuvenation;
		case taunt: return R.string.skill_title_taunt;
		case concussion: return R.string.skill_title_concussion;
		case weaponProficiencyDagger: return R.string.skill_title_weapon_prof_dagger;
		case weaponProficiency1hsword: return R.string.skill_title_weapon_prof_1hsword;
		case weaponProficiency2hsword: return R.string.skill_title_weapon_prof_2hsword;
		case weaponProficiencyAxe: return R.string.skill_title_weapon_prof_axe;
		case weaponProficiencyBlunt: return R.string.skill_title_weapon_prof_blunt;
		case weaponProficiencyUnarmed: return R.string.skill_title_weapon_prof_unarmed;
		case armorProficiencyShield: return R.string.skill_title_armor_prof_shield;
		case armorProficiencyUnarmored: return R.string.skill_title_armor_prof_unarmored;
		case armorProficiencyLight: return R.string.skill_title_armor_prof_light;
		case armorProficiencyHeavy: return R.string.skill_title_armor_prof_heavy;
		case fightstyleDualWield: return R.string.skill_title_fightstyle_dualwield;
		case fightstyle2hand: return R.string.skill_title_fightstyle_2hand;
		case fightstyleWeaponShield: return R.string.skill_title_fightstyle_weapon_shield;
		case fightstyleUnarmedUnarmored: return R.string.skill_title_fightstyle_unarmed_unarmored;
		case specializationDualWield: return R.string.skill_title_specialization_dualwield;
		case specialization2hand: return R.string.skill_title_specialization_2hand;
		case specializationWeaponShield: return R.string.skill_title_specialization_weapon_shield;
		case weaponProficiencyPole: return R.string.skill_title_weapon_prof_pole;
		case sporeImmunity: return R.string.skill_title_spore_immunity;
		default:
			return -1;
		}
	}

	private static String getSkillLongDescription(final SkillCollection.SkillID skill, final Resources res) {
		switch (skill) {
		case weaponChance: return res.getString(R.string.skill_longdescription_weapon_chance, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_CHANCE);
		case weaponDmg: return res.getString(R.string.skill_longdescription_weapon_dmg, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_DAMAGE_MAX);
		case barter: return res.getString(R.string.skill_longdescription_barter, SkillCollection.PER_SKILLPOINT_INCREASE_BARTER_PRICEFACTOR_PERCENTAGE);
		case dodge: return res.getString(R.string.skill_longdescription_dodge, SkillCollection.PER_SKILLPOINT_INCREASE_DODGE);
		case barkSkin: return res.getString(R.string.skill_longdescription_barkskin, SkillCollection.PER_SKILLPOINT_INCREASE_BARKSKIN);
		case moreCriticals: return res.getString(R.string.skill_longdescription_more_criticals, SkillCollection.PER_SKILLPOINT_INCREASE_MORE_CRITICALS_PERCENT);
		case betterCriticals: return res.getString(R.string.skill_longdescription_better_criticals, SkillCollection.PER_SKILLPOINT_INCREASE_BETTER_CRITICALS_PERCENT);
		case speed: return res.getString(R.string.skill_longdescription_speed, SkillCollection.PER_SKILLPOINT_INCREASE_SPEED);
		case coinfinder: return res.getString(R.string.skill_longdescription_coinfinder, SkillCollection.PER_SKILLPOINT_INCREASE_COINFINDER_CHANCE_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_COINFINDER_QUANTITY_PERCENT);
		case moreExp: return res.getString(R.string.skill_longdescription_more_exp, SkillCollection.PER_SKILLPOINT_INCREASE_MORE_EXP_PERCENT);
		case cleave: return res.getString(R.string.skill_longdescription_cleave, SkillCollection.PER_SKILLPOINT_INCREASE_CLEAVE_AP);
		case eater: return res.getString(R.string.skill_longdescription_eater, SkillCollection.PER_SKILLPOINT_INCREASE_EATER_HEALTH);
		case fortitude: return res.getString(R.string.skill_longdescription_fortitude, SkillCollection.PER_SKILLPOINT_INCREASE_FORTITUDE_HEALTH);
		case evasion: return res.getString(R.string.skill_longdescription_evasion, SkillCollection.PER_SKILLPOINT_INCREASE_EVASION_FLEE_CHANCE_PERCENTAGE, SkillCollection.PER_SKILLPOINT_INCREASE_EVASION_MONSTER_ATTACK_CHANCE_PERCENTAGE);
		case regeneration: return res.getString(R.string.skill_longdescription_regeneration, SkillCollection.PER_SKILLPOINT_INCREASE_REGENERATION);
		case lowerExploss: return res.getString(R.string.skill_longdescription_lower_exploss, SkillCollection.PER_SKILLPOINT_INCREASE_EXPLOSS_PERCENT, SkillCollection.MAX_LEVEL_LOWER_EXPLOSS);
		case magicfinder: return res.getString(R.string.skill_longdescription_magicfinder, SkillCollection.PER_SKILLPOINT_INCREASE_MAGICFINDER_CHANCE_PERCENT);
		case resistanceMental: return res.getString(R.string.skill_longdescription_resistance_mental, SkillCollection.PER_SKILLPOINT_INCREASE_RESISTANCE_CHANCE_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_RESISTANCE_CHANCE_PERCENT * SkillCollection.MAX_LEVEL_RESISTANCE);
		case resistancePhysical: return res.getString(R.string.skill_longdescription_resistance_physical_capacity, SkillCollection.PER_SKILLPOINT_INCREASE_RESISTANCE_CHANCE_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_RESISTANCE_CHANCE_PERCENT * SkillCollection.MAX_LEVEL_RESISTANCE);
		case resistanceBlood: return res.getString(R.string.skill_longdescription_resistance_blood_disorder, SkillCollection.PER_SKILLPOINT_INCREASE_RESISTANCE_CHANCE_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_RESISTANCE_CHANCE_PERCENT * SkillCollection.MAX_LEVEL_RESISTANCE);
		case shadowBless: return res.getString(R.string.skill_longdescription_shadow_bless, SkillCollection.PER_SKILLPOINT_INCREASE_RESISTANCE_SHADOW_BLESS);
		case sporeImmunity: return res.getString(R.string.skill_longdescription_spore_immunity);
		case crit1: return res.getString(R.string.skill_longdescription_crit1, SkillCollection.PER_SKILLPOINT_INCREASE_CRIT1_CHANCE);
		case crit2: return res.getString(R.string.skill_longdescription_crit2, SkillCollection.PER_SKILLPOINT_INCREASE_CRIT2_CHANCE);
		case rejuvenation: return res.getString(R.string.skill_longdescription_rejuvenation, SkillCollection.PER_SKILLPOINT_INCREASE_REJUVENATION_CHANCE);
		case taunt: return res.getString(R.string.skill_longdescription_taunt, SkillCollection.PER_SKILLPOINT_INCREASE_TAUNT_CHANCE, SkillCollection.TAUNT_AP_LOSS);
		case concussion: return res.getString(R.string.skill_longdescription_concussion, SkillCollection.CONCUSSION_THRESHOLD, SkillCollection.PER_SKILLPOINT_INCREASE_CONCUSSION_CHANCE);
		case weaponProficiencyDagger: return res.getString(R.string.skill_longdescription_weapon_prof_dagger, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_AC_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_BC_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_CS_PERCENT);
		case weaponProficiency1hsword: return res.getString(R.string.skill_longdescription_weapon_prof_1hsword, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_AC_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_BC_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_CS_PERCENT);
		case weaponProficiency2hsword: return res.getString(R.string.skill_longdescription_weapon_prof_2hsword, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_AC_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_BC_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_CS_PERCENT);
		case weaponProficiencyAxe: return res.getString(R.string.skill_longdescription_weapon_prof_axe, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_AC_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_BC_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_CS_PERCENT);
		case weaponProficiencyBlunt: return res.getString(R.string.skill_longdescription_weapon_prof_blunt, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_AC_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_BC_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_CS_PERCENT);
		case weaponProficiencyUnarmed: return res.getString(R.string.skill_longdescription_weapon_prof_unarmed, SkillCollection.PER_SKILLPOINT_INCREASE_UNARMED_AC, SkillCollection.PER_SKILLPOINT_INCREASE_UNARMED_DMG, SkillCollection.PER_SKILLPOINT_INCREASE_UNARMED_BC);
		case armorProficiencyShield: return res.getString(R.string.skill_longdescription_armor_prof_shield, SkillCollection.PER_SKILLPOINT_INCREASE_SHIELD_PROF_DR);
		case armorProficiencyUnarmored: return res.getString(R.string.skill_longdescription_armor_prof_unarmored, SkillCollection.PER_SKILLPOINT_INCREASE_UNARMORED_BC);
		case armorProficiencyLight: return res.getString(R.string.skill_longdescription_armor_prof_light, SkillCollection.PER_SKILLPOINT_INCREASE_LIGHT_ARMOR_BC_PERCENT);
		case armorProficiencyHeavy: return res.getString(R.string.skill_longdescription_armor_prof_heavy, SkillCollection.PER_SKILLPOINT_INCREASE_HEAVY_ARMOR_BC_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_HEAVY_ARMOR_MOVECOST_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_HEAVY_ARMOR_ATKCOST_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_HEAVY_ARMOR_USECOST_PERCENT);
		case fightstyleDualWield: return res.getString(R.string.skill_longdescription_fightstyle_dualwield, SkillCollection.DUALWIELD_EFFICIENCY_LEVEL0, SkillCollection.DUALWIELD_EFFICIENCY_LEVEL1, SkillCollection.DUALWIELD_LEVEL1_OFFHAND_AP_COST_PERCENT, SkillCollection.DUALWIELD_EFFICIENCY_LEVEL2);
		case fightstyle2hand: return res.getString(R.string.skill_longdescription_fightstyle_2hand, SkillCollection.PER_SKILLPOINT_INCREASE_FIGHTSTYLE_2HAND_DMG_PERCENT);
		case fightstyleWeaponShield: return res.getString(R.string.skill_longdescription_fightstyle_weapon_shield, SkillCollection.PER_SKILLPOINT_INCREASE_FIGHTSTYLE_WEAPON_AC_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_FIGHTSTYLE_SHIELD_BC_PERCENT);
		case fightstyleUnarmedUnarmored: return res.getString(R.string.skill_longdescription_fightstyle_unarmed_unarmored,
				SkillCollection.PER_SKILLPOINT_INCREASE_UNARMED_UNARMORED_AC,
				SkillCollection.PER_SKILLPOINT_INCREASE_UNARMED_UNARMORED_BC,
				SkillCollection.PER_SKILLPOINT_INCREASE_UNARMED_UNARMORED_DR,
				SkillCollection.PER_SKILLPOINT_INCREASE_UNARMED_UNARMORED_DMG_MAX,
				SkillCollection.PER_SKILLPOINT_INCREASE_UNARMED_UNARMORED_CM_PERCENT);
		case specializationDualWield: return res.getString(R.string.skill_longdescription_specialization_dualwield, SkillCollection.PER_SKILLPOINT_INCREASE_SPECIALIZATION_DUALWIELD_AC_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_SPECIALIZATION_DUALWIELD_BC_PERCENT);
		case specialization2hand: return res.getString(R.string.skill_longdescription_specialization_2hand, SkillCollection.PER_SKILLPOINT_INCREASE_SPECIALIZATION_2HAND_DMG_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_SPECIALIZATION_2HAND_AC_PERCENT);
		case specializationWeaponShield: return res.getString(R.string.skill_longdescription_specialization_weapon_shield, SkillCollection.PER_SKILLPOINT_INCREASE_SPECIALIZATION_WEAPON_AC_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_SPECIALIZATION_WEAPON_DMG_PERCENT);
		case weaponProficiencyPole: return res.getString(R.string.skill_longdescription_weapon_prof_pole, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_AC_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_BC_PERCENT, SkillCollection.PER_SKILLPOINT_INCREASE_WEAPON_PROF_CS_PERCENT);
		default:
			return "";
		}
	}

	private static String getRequirementDescription(SkillLevelRequirement requirement, int requiredValue, final Resources res) {
		switch (requirement.requirementType) {
		case skillLevel:
			String skillName = res.getString(getSkillTitleResourceID(SkillCollection.SkillID.valueOf(requirement.skillOrStatID)));
			return res.getString(R.string.skill_prerequisite_other_skill, requiredValue, skillName);
		case experienceLevel:
			return res.getString(R.string.skill_prerequisite_level, requiredValue);
		case playerStat:
			String combatStatName = res.getString(getRequirementPlayerStatsResourceID(requirement.skillOrStatID)).replace(':', ' ').trim();
			return res.getString(R.string.skill_prerequisite_stat, requiredValue, combatStatName);
		}
		return "";
	}

	private static int getRequirementPlayerStatsResourceID(String statID) {
		return getRequirementPlayerStatsResourceID(Player.StatID.valueOf(statID));
	}
	private static int getRequirementPlayerStatsResourceID(Player.StatID statID) {
		switch (statID) {
		case maxHP: return R.string.actorinfo_health;
		case maxAP: return R.string.heroinfo_actionpoints;
		case moveCost: return R.string.actorinfo_movecost;
		case attackCost: return R.string.traitsinfo_attack_cost;
		case attackChance: return R.string.traitsinfo_attack_chance;
		case criticalSkill: return R.string.traitsinfo_criticalhit_skill;
		case criticalMultiplier: return R.string.traitsinfo_criticalhit_multiplier;
		case damagePotentialMin: return R.string.traitsinfo_attack_damage;
		case damagePotentialMax: return R.string.traitsinfo_attack_damage;
		case blockChance: return R.string.traitsinfo_defense_chance;
		case damageResistance: return R.string.traitsinfo_defense_damageresist;
		default:
			return -1;
		}
	}
}
