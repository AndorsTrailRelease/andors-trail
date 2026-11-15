package com.gpl.rpg.AndorsTrail.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import com.gpl.rpg.AndorsTrail.model.ability.ActorCondition;
import com.gpl.rpg.AndorsTrail.model.ability.ActorConditionType;
import com.gpl.rpg.AndorsTrail.model.actor.Actor;
import com.gpl.rpg.AndorsTrail.util.Size;

public final class ActorStatsControllerTest {

	@Test
	public void removeAllTemporaryConditionsRemovesTimedImmunitiesFromNonPlayerActors() {
		ActorStatsController controller = new ActorStatsController(null, null);
		Actor actor = new Actor(new Size(1, 1), false, false);
		ActorConditionType type = new ActorConditionType(
				"test_condition",
				"Test condition",
				"",
				0,
				ActorConditionType.ConditionCategory.physical,
				false,
				false,
				null,
				null,
				null
		);

		ActorCondition temporaryCondition = new ActorCondition(type, 1, 3);
		ActorCondition untilSleepCondition = new ActorCondition(type, 1, ActorCondition.DURATION_FOREVER_UNTIL_SLEEP);
		ActorCondition permanentCondition = new ActorCondition(type, 1, ActorCondition.DURATION_FOREVER);
		ActorCondition temporaryImmunity = new ActorCondition(type, ActorCondition.MAGNITUDE_REMOVE_ALL, 3);
		ActorCondition untilSleepImmunity = new ActorCondition(type, ActorCondition.MAGNITUDE_REMOVE_ALL, ActorCondition.DURATION_FOREVER_UNTIL_SLEEP);
		ActorCondition permanentImmunity = new ActorCondition(type, ActorCondition.MAGNITUDE_REMOVE_ALL, ActorCondition.DURATION_FOREVER);
		actor.conditions.add(temporaryCondition);
		actor.conditions.add(untilSleepCondition);
		actor.conditions.add(permanentCondition);
		actor.immunities.add(temporaryImmunity);
		actor.immunities.add(untilSleepImmunity);
		actor.immunities.add(permanentImmunity);

		controller.removeAllTemporaryConditions(actor);

		assertEquals(1, actor.conditions.size());
		assertSame(permanentCondition, actor.conditions.get(0));
		assertEquals(1, actor.immunities.size());
		assertSame(permanentImmunity, actor.immunities.get(0));
	}
}
