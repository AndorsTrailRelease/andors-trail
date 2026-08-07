package com.gpl.rpg.AndorsTrail.view;

import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.model.actor.Actor;
import com.gpl.rpg.AndorsTrail.util.Format;
import com.gpl.rpg.AndorsTrail.util.Range;

public final class TraitsInfoView {

	public static void update(ViewGroup group, Actor actor) {
		TableLayout actorinfo_stats_table = (TableLayout) group.findViewById(R.id.actorinfo_stats_table);

		updateTraitsTable(
			actorinfo_stats_table
			,actor.getMoveCost()
			,actor.getAttackCost()
			,actor.getAttackChance()
			,actor.getDamagePotential()
			,actor.getCriticalSkill()
			,actor.getCriticalMultiplier()
			,actor.getBlockChance()
			,actor.getDamageResistance()
			,actor.isImmuneToCriticalHits());

		TextView actorinfo_currentconditions_title = (TextView) group.findViewById(R.id.actorinfo_currentconditions_title);
		ActorConditionList actorinfo_currentconditions = (ActorConditionList) group.findViewById(R.id.actorinfo_currentconditions);
		if (actor.conditions.isEmpty() && actor.immunities.isEmpty()) {
			actorinfo_currentconditions_title.setVisibility(View.GONE);
			actorinfo_currentconditions.setVisibility(View.GONE);
		} else {
			actorinfo_currentconditions_title.setVisibility(View.VISIBLE);
			actorinfo_currentconditions.setVisibility(View.VISIBLE);
			actorinfo_currentconditions.update(actor.conditions, actor.immunities);
		}
	}

	public static void updateTraitsTable(
			ViewGroup group
			,int moveCost
			,int attackCost
			,int attackChance
			,Range damagePotential
			,int criticalSkill
			,float criticalMultiplier
			,int blockChance
			,int damageResistance
			,boolean isImmuneToCriticalHits
		) {
		TableRow row;
		TextView tv;
		Resources res = group.getResources();

		tv = (TextView) group.findViewById(R.id.traitsinfo_move_cost);
		tv.setText(res.getString(R.string.general_ap_value, moveCost));

		tv = (TextView) group.findViewById(R.id.traitsinfo_attack_cost);
		tv.setText(res.getString(R.string.general_ap_value, attackCost));

		row = (TableRow) group.findViewById(R.id.traitsinfo_attack_chance_row);
		tv = (TextView) group.findViewById(R.id.traitsinfo_attack_chance);
		tv.setText(res.getString(R.string.general_integer_value, attackChance));


		row = (TableRow) group.findViewById(R.id.traitsinfo_attack_damage_row);
		if (damagePotential != null && damagePotential.max != 0) {
			row.setVisibility(View.VISIBLE);
			tv = (TextView) group.findViewById(R.id.traitsinfo_attack_damage);
			if (damagePotential.isMax()) {
				tv.setText(res.getString(R.string.general_hp_value, damagePotential.max));
			} else {
				tv.setText(res.getString(R.string.general_hp_minmax_value, damagePotential.current, damagePotential.max));
			}

		} else {
			row.setVisibility(View.GONE);
		}

		row = (TableRow) group.findViewById(R.id.traitsinfo_criticalhit_skill_row);
		if (criticalSkill == 0) {
			row.setVisibility(View.GONE);
		} else {
			row.setVisibility(View.VISIBLE);
			tv = (TextView) group.findViewById(R.id.traitsinfo_criticalhit_skill);
			tv.setText(res.getString(R.string.general_points_value, criticalSkill));
		}

		row = (TableRow) group.findViewById(R.id.traitsinfo_criticalhit_multiplier_row);
		if (criticalMultiplier != 0 && criticalMultiplier != 1) {
			row.setVisibility(View.VISIBLE);
			tv = (TextView) group.findViewById(R.id.traitsinfo_criticalhit_multiplier);
			tv.setText(res.getString(R.string.general_multiplier_value, criticalMultiplier));
		} else {
			row.setVisibility(View.GONE);
		}

		row = (TableRow) group.findViewById(R.id.traitsinfo_criticalhit_effectivechance_row);
		if (criticalSkill != 0 && criticalMultiplier != 0 && criticalMultiplier != 1) {
			row.setVisibility(View.VISIBLE);
			tv = (TextView) group.findViewById(R.id.traitsinfo_criticalhit_effectivechance);
			tv.setText(res.getString(R.string.general_percentage_value, Format.localizePercentFromIntPercent(Actor.getEffectiveCriticalChance(criticalSkill))));
		} else {
			row.setVisibility(View.GONE);
		}

		row = (TableRow) group.findViewById(R.id.traitsinfo_block_chance_row);
		if (blockChance == 0) {
			row.setVisibility(View.GONE);
		} else {
			row.setVisibility(View.VISIBLE);
			tv = (TextView) group.findViewById(R.id.traitsinfo_block_chance);
			tv.setText(res.getString(R.string.general_integer_value, blockChance));
		}

		row = (TableRow) group.findViewById(R.id.traitsinfo_damageresist_row);
		if (damageResistance == 0) {
			row.setVisibility(View.GONE);
		} else {
			row.setVisibility(View.VISIBLE);
			tv = (TextView) group.findViewById(R.id.traitsinfo_damageresist);
			tv.setText(res.getString(R.string.general_hp_value, damageResistance));
		}

		row = (TableRow) group.findViewById(R.id.traitsinfo_is_immune_to_critical_hits_row);
		row.setVisibility(isImmuneToCriticalHits ? View.VISIBLE : View.GONE);
	}
}
